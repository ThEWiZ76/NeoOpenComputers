package li.cil.oc.common.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.storage.StorageHelper;
import com.google.common.collect.ImmutableSet;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.prefab.AbstractValue;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

/** Each request owns one AE2 requester, shared by all restored aliases of its OC value. */
public final class CraftingStatusValue extends AbstractValue {
    private static final Map<UUID, WeakReference<Request>> REQUESTS = new HashMap<>();
    private static final Map<UUID, Request> ACTIVE = new HashMap<>();
    private static boolean registered;
    private Request request;

    public CraftingStatusValue() { }
    CraftingStatusValue(final CraftingTarget target, final int amount, final boolean prioritizePower, final String cpuName) {
        request = new Request(target, amount, prioritizePower, cpuName);
        synchronized (REQUESTS) { REQUESTS.put(request.id, new WeakReference<>(request)); ACTIVE.put(request.id, request); }
    }

    public static void register() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.addListener(CraftingStatusValue::tick);
        NeoForge.EVENT_BUS.addListener(CraftingStatusValue::stopped);
    }

    private static void tick(final ServerTickEvent.Post event) {
        final java.util.List<Request> requests;
        synchronized (REQUESTS) {
            REQUESTS.values().removeIf(reference -> reference.get() == null);
            requests = java.util.List.copyOf(ACTIVE.values());
        }
        for (final var request : requests) {
            request.tick();
            if (request.done || request.canceled || request.failed != null) {
                synchronized (REQUESTS) { ACTIVE.remove(request.id); }
            }
        }
    }

    private static void stopped(final ServerStoppedEvent event) {
        synchronized (REQUESTS) {
            for (final var reference : REQUESTS.values()) {
                final var request = reference.get();
                if (request != null) request.release();
            }
            REQUESTS.clear();
            ACTIVE.clear();
        }
    }

    @Callback(doc = "function():boolean -- Get whether this request has completed; nil, computing while calculating.")
    public Object[] isDone(final Context context, final Arguments args) { return request.result(true); }
    @Callback(doc = "function():boolean -- Get whether this request was canceled; nil, computing while calculating.")
    public Object[] isCanceled(final Context context, final Arguments args) { return request.result(false); }
    @Callback(doc = "function():boolean -- Cancel a submitted request.")
    public Object[] cancel(final Context context, final Arguments args) {
        request.captureTerminal();
        if (request.failed != null || request.computing()) return request.result(false);
        if (request.done) return new Object[]{false, "job already completed"};
        if (request.link != null) request.link.cancel();
        request.canceled = true;
        return new Object[]{true};
    }

    @Override public void save(final CompoundTag tag) { request.save(tag); }
    @Override public void load(final CompoundTag tag) {
        final UUID id = tag.getUUID("id");
        synchronized (REQUESTS) {
            final var existing = REQUESTS.get(id);
            request = existing == null ? null : existing.get();
            if (request == null) {
                request = new Request(tag);
                REQUESTS.put(id, new WeakReference<>(request));
                if (!request.done && !request.canceled && request.failed == null) ACTIVE.put(id, request);
            }
        }
    }

    private static final class Request implements ICraftingRequester, ICraftingSimulationRequester {
        final UUID id;
        final CraftingTarget target;
        final int amount;
        final boolean prioritizePower;
        final String cpuName;
        IManagedGridNode node;
        volatile IGridNode controller;
        ICraftingLink link;
        Future<ICraftingPlan> future;
        CompoundTag nodeData = new CompoundTag();
        String failed;
        boolean done;
        boolean canceled;
        int retryDelay = 1;
        int retryTicks;

        Request(final CraftingTarget target, final int amount, final boolean prioritizePower, final String cpuName) {
            id = UUID.randomUUID();
            this.target = target;
            this.amount = amount;
            this.prioritizePower = prioritizePower;
            this.cpuName = cpuName;
        }

        Request(final CompoundTag tag) {
            id = tag.getUUID("id");
            target = new CraftingTarget(tag.getCompound("target"));
            amount = tag.getInt("amount");
            prioritizePower = tag.getBoolean("prioritizePower");
            cpuName = tag.getString("cpuName");
            done = tag.getBoolean("done"); canceled = tag.getBoolean("canceled");
            if (tag.contains("failed")) failed = tag.getString("failed");
            nodeData = tag.getCompound("node").copy();
            if (tag.contains("link")) link = StorageHelper.loadCraftingLink(tag.getCompound("link"), this);
        }

        boolean computing() { return link == null && !done && !canceled && failed == null; }

        Object[] result(final boolean completion) {
            captureTerminal();
            if (failed != null) return new Object[]{false, "request failed (" + failed + ")"};
            if (computing()) return new Object[]{null, "computing"};
            return new Object[]{completion ? done : canceled};
        }

        void captureTerminal() {
            if (link != null) { done |= link.isDone(); canceled |= link.isCanceled(); }
        }

        void tick() {
            captureTerminal();
            if (done || canceled || failed != null) { release(); return; }
            if (retryTicks > 0) { retryTicks--; return; }
            try {
                final var current = target.node();
                if (controller != null && controller != current) release();
                controller = current;
                if (node == null) {
                    node = GridHelper.createManagedNode(this, (owner, changedNode) -> { })
                        .setInWorldNode(false).setIdlePowerUsage(0).addService(ICraftingRequester.class, this);
                    node.loadFromNBT(nodeData);
                    node.setOwningPlayerId(controller.getOwningPlayerId());
                    node.create(target.world(), target.position());
                    GridHelper.createConnection(node.getNode(), controller);
                }
                retryDelay = 1;
                if (link != null) return;
                final var crafting = controller.getGrid().getCraftingService();
                if (future == null) {
                    future = crafting.beginCraftingCalculation(target.world(), this, target.item(), amount, CalculationStrategy.REPORT_MISSING_ITEMS);
                    return;
                }
                // AE2 owns the asynchronous calculation; get() cannot block after isDone() is true.
                if (!future.isDone()) return;
                final var completed = future;
                future = null;
                final var plan = completed.get();
                ICraftingCPU cpu = null;
                if (!cpuName.isEmpty()) {
                    cpu = crafting.getCpus().stream().filter(candidate -> candidate.getName() != null && cpuName.equals(candidate.getName().getString())).findFirst().orElse(null);
                    if (cpu == null) { fail("no cpu named " + cpuName); return; }
                }
                final var result = crafting.submitJob(plan, this, cpu, prioritizePower, getActionSource());
                if (result.successful() && result.link() != null) link = result.link();
                else fail(result.errorCode() == null ? "no link" : result.errorCode().name().toLowerCase(java.util.Locale.ROOT));
            } catch (IllegalStateException exception) {
                if ("waiting for ae network to load".equals(exception.getMessage()) || "no ae grid".equals(exception.getMessage())) {
                    release();
                    retryTicks = retryDelay;
                    retryDelay = Math.min(100, retryDelay * 2);
                } else fail(exception.getMessage());
            } catch (Exception exception) { fail(exception.toString()); }
        }

        void fail(final String reason) {
            failed = java.util.Objects.requireNonNullElse(reason, "no link");
            if (link != null) link.cancel();
            release();
        }

        void release() {
            if (future != null) { future.cancel(true); future = null; }
            if (node != null) { node.saveToNBT(nodeData); node.destroy(); node = null; }
            controller = null;
        }

        void save(final CompoundTag tag) {
            captureTerminal();
            tag.putUUID("id", id); tag.put("target", target.save()); tag.putInt("amount", amount);
            tag.putBoolean("prioritizePower", prioritizePower); tag.putString("cpuName", cpuName);
            tag.putBoolean("done", done); tag.putBoolean("canceled", canceled);
            if (failed != null) tag.putString("failed", failed);
            if (node != null) node.saveToNBT(nodeData);
            tag.put("node", nodeData.copy());
            if (link != null) { final var data = new CompoundTag(); link.writeToNBT(data); tag.put("link", data); }
        }

        @Override public IGridNode getActionableNode() { return node == null ? null : node.getNode(); }
        @Override public IActionSource getActionSource() { return IActionSource.ofMachine(() -> controller); }
        @Override public ImmutableSet<ICraftingLink> getRequestedJobs() { return link == null || done || canceled ? ImmutableSet.of() : ImmutableSet.of(link); }
        @Override public long insertCraftedItems(final ICraftingLink link, final AEKey what, final long amount, final Actionable mode) {
            // Leave output with AE2's storage insertion path, as the original OC requester did.
            return 0;
        }
        @Override public void jobStateChange(final ICraftingLink changed) { captureTerminal(); }
    }
}
