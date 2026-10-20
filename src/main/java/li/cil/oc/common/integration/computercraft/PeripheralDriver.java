package li.cil.oc.common.integration.computercraft;

import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaTask;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.WorkMonitor;
import dan200.computercraft.core.methods.PeripheralMethod;
import dan200.computercraft.impl.Peripherals;
import dan200.computercraft.shared.computer.core.ServerContext;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.LimitReachedException;
import li.cil.oc.api.network.BlacklistedPeripheral;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.ManagedPeripheral;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.PeripheralBlacklist;
import li.cil.oc.common.blockentity.AdapterBlockEntity;
import li.cil.oc.common.driver.CompoundBlockEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Uses the pinned CC dispatcher, including its annotated and generic inventory methods. */
public final class PeripheralDriver implements DriverBlock {
    private static boolean configured(Class<?> type) {
        if (type == null) return false;
        if (ModSettings.peripheralBlacklist().contains(type.getName()) || configured(type.getSuperclass())) return true;
        for (Class<?> parent : type.getInterfaces()) if (configured(parent)) return true;
        return false;
    }
    private static boolean blocked(Object value) {
        if (value instanceof BlacklistedPeripheral policy) return policy.isPeripheralBlacklisted();
        return PeripheralBlacklist.isBlacklisted(value.getClass()) || configured(value.getClass());
    }
    private static IPeripheral find(ServerLevel world, BlockPos pos, Direction face) {
        if (!world.hasChunkAt(pos)) return null;
        final var tile = world.getBlockEntity(pos);
        if (tile == null || tile instanceof li.cil.oc.api.network.Environment || blocked(tile)) return null;
        IPeripheral peripheral = world.getCapability(PeripheralCapability.get(), pos, face);
        if (peripheral == null) peripheral = Peripherals.getGenericPeripheral(world, pos, face, tile);
        return peripheral == null || blocked(peripheral) ? null : peripheral;
    }
    @Override public boolean worksWith(Level world, BlockPos pos, Direction side) {
        return world instanceof ServerLevel server && pos != null && side != null && find(server, pos, side.getOpposite()) != null;
    }
    @Override public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        if (!(world instanceof ServerLevel server) || pos == null || side == null) return null;
        final var peripheral = find(server, pos, side.getOpposite());
        return peripheral == null ? null : new PeripheralEnvironment(server, pos.immutable(), side, peripheral);
    }

    public static final class PeripheralEnvironment extends AbstractManagedEnvironment implements ManagedPeripheral, NamedBlock {
        private static final AtomicLong TASK_IDS = new AtomicLong();
        static long nextTaskId() { return TASK_IDS.incrementAndGet(); }
        private final ServerLevel world;
        private final BlockPos pos;
        private final Direction side;
        private final BlockEntity tile;
        private final IPeripheral peripheral;
        private final Map<String, PeripheralMethod> methods;
        private final Map<String, Access> computers = new LinkedHashMap<>();
        private final ArrayList<PendingTask> tasks = new ArrayList<>();
        private boolean invalid;

        PeripheralEnvironment(ServerLevel world, BlockPos pos, Direction side, IPeripheral peripheral) {
            this.world = world; this.pos = pos; this.side = side; this.peripheral = peripheral; tile = world.getBlockEntity(pos);
            methods = ServerContext.get(world.getServer()).peripheralMethods().getSelfMethods(peripheral);
            setNode(Network.newNode(this, Visibility.Network).create());
        }
        @Override public String preferredName() {
            // CC's new generic fallback supplements existing OC drivers without renaming their components.
            if (peripheral instanceof dan200.computercraft.shared.peripheral.generic.GenericPeripheral) {
                final boolean inventory = new li.cil.oc.common.driver.InventoryBlockDriver().worksWith(world, pos, side);
                final boolean fluid = new li.cil.oc.common.driver.FluidHandlerBlockDriver().worksWith(world, pos, side);
                if (inventory && fluid) return "component";
                if (inventory) return "inventory";
                if (fluid) return "fluid_handler";
            }
            return peripheral.getType();
        }
        @Override public int priority() { return -1; }
        @Override public String[] methods() { return methods.keySet().toArray(String[]::new); }
        @Override public boolean canUpdate() { return true; }
        private boolean valid() {
            return !invalid && world.hasChunkAt(pos) && world.getBlockEntity(pos) == tile
                && peripheral.equals(find(world, pos, side.getOpposite()));
        }
        @Override public Object[] invoke(String method, Context context, Arguments args) throws Exception {
            // Both OC architectures retry LimitReachedException as a synchronized call, before any side effect.
            if (!world.getServer().isSameThread()) throw new LimitReachedException();
            if (!valid()) throw new IllegalStateException("peripheral is unavailable");
            final var function = methods.get(method);
            if (function == null) throw new IllegalArgumentException("no such peripheral method: " + method);
            if (context == null || context.node() == null) throw new IllegalArgumentException("peripheral requires a computer context");
            Access access = computers.get(context.node().address());
            final boolean temporary = access == null;
            if (temporary) access = new Access(context);
            final Access owner = access;
            final var requested = new ArrayList<Long>();
            final ILuaContext luaContext = new ILuaContext() {
                @Override public long issueMainThreadTask(LuaTask task) throws LuaException {
                    final long id = owner.issueMainThreadTask(task); requested.add(id); return id;
                }
                @Override public MethodResult executeMainThreadTask(LuaTask task) throws LuaException { return owner.executeMainThreadTask(task); }
            };
            try {
                if (temporary) peripheral.attach(access);
                final Object[] raw = new Object[args.count()];
                for (int i = 0; i < raw.length; i++) raw[i] = args.checkAny(i);
                var result = function.apply(peripheral, luaContext, access, new ComputerCraftArguments(raw));
                // CC's guarded generic peripherals wrap executeMainThreadTask using the default task callback.
                // Resolve only work requested by this invocation, already on the server thread; never await external events.
                for (int step = 0; result.getCallback() != null; step++) {
                    final Object[] filter = result.getResult();
                    if (step >= 64 || filter == null || filter.length != 1 || !"task_complete".equals(filter[0]) || requested.isEmpty()) {
                        throw new UnsupportedOperationException("peripheral event waits/yields are unsupported");
                    }
                    final long id = requested.removeLast();
                    final var pending = tasks.stream().filter(task -> task.id == id && task.owner == owner).findFirst().orElseThrow();
                    tasks.remove(pending);
                    owner.requireAttached();
                    Object[] completion;
                    try { pending.task.execute(); completion = new Object[]{"task_complete", id, true}; }
                    catch (Exception exception) { completion = new Object[]{"task_complete", id, false, exception.getMessage()}; }
                    result = result.getCallback().resume(completion);
                }
                return ComputerCraftArguments.results(result.getResult());
            } finally { if (temporary) access.close(); }
        }
        @Override public void onConnect(Node connected) {
            if (connected == node()) {
                for (Node reachable : node().reachableNodes()) attach(reachable);
            } else attach(connected);
        }
        private void attach(Node connected) {
            if (invalid || !(connected.host() instanceof Context context) || connected.address() == null || computers.containsKey(connected.address())) return;
            final var access = new Access(context); computers.put(connected.address(), access);
            try { peripheral.attach(access); }
            catch (RuntimeException exception) { computers.remove(connected.address()); access.close(); throw exception; }
        }
        @Override public void onDisconnect(Node disconnected) {
            if (disconnected == node()) {
                for (Access access : new ArrayList<>(computers.values())) access.close();
                computers.clear(); tasks.clear();
            } else {
                final var access = computers.remove(disconnected.address());
                if (access != null) access.close();
            }
        }
        @Override public void update() {
            if (!valid()) {
                if (!invalid) {
                    invalid = true; node().remove();
                    final BlockPos adapterPos = pos.relative(side.getOpposite());
                    if (world.hasChunkAt(adapterPos) && world.getBlockEntity(adapterPos) instanceof AdapterBlockEntity adapter) adapter.refreshNeighbor(pos);
                }
                return;
            }
            final long tick = world.getGameTime();
            final var due = new ArrayList<PendingTask>();
            tasks.removeIf(task -> { if (task.tick <= tick) { due.add(task); return true; } return !task.owner.active; });
            for (PendingTask pending : due) {
                if (!pending.owner.active) continue;
                try {
                    final Object[] result = pending.task.execute();
                    final Object[] event = new Object[2 + (result == null ? 0 : result.length)]; event[0] = pending.id; event[1] = true;
                    if (result != null) System.arraycopy(result, 0, event, 2, result.length);
                    pending.owner.queueEvent("task_complete", event);
                } catch (Exception exception) { pending.owner.queueEvent("task_complete", pending.id, false, exception.getMessage()); }
            }
        }
        private record PendingTask(long id, long tick, Access owner, LuaTask task) { }

        private final class Access implements IComputerAccess, ILuaContext, WorkMonitor {
            final Context context;
            final Map<String, ManagedEnvironment> mounts = new LinkedHashMap<>();
            volatile boolean active = true;
            boolean closing;
            long blockedTick = Long.MIN_VALUE;
            Access(Context context) { this.context = context; }
            void requireAttached() { if (!active) throw new IllegalStateException("peripheral is detached"); }
            void close() {
                if (!active || closing) return;
                closing = true;
                try { peripheral.detach(this); }
                catch (RuntimeException exception) { li.cil.oc.NeoOpenComputers.LOGGER.warn("ComputerCraft peripheral failed to detach at {}", pos, exception); }
                finally {
                    active = false;
                    for (var mount : mounts.values()) mount.node().remove();
                    mounts.clear(); tasks.removeIf(task -> task.owner == this);
                }
            }
            @Override public int getID() { return context.node().address().hashCode(); }
            @Override public String getAttachmentName() { requireAttached(); return node().address(); }
            @Override public void queueEvent(String event, Object... arguments) {
                requireAttached(); final var values = ComputerCraftArguments.results(arguments);
                final Runnable send = () -> { if (active) context.signal(event, values); };
                if (world.getServer().isSameThread()) send.run(); else world.getServer().execute(send);
            }
            @Override public Map<String, IPeripheral> getAvailablePeripherals() {
                requireAttached(); final var result = new LinkedHashMap<String, IPeripheral>();
                for (Node reachable : context.node().reachableNodes()) {
                    if (reachable instanceof Component component && component.canBeSeenFrom(context.node())
                        && reachable.host() instanceof CompoundBlockEnvironment compound) {
                        for (var entry : compound.environments()) {
                            if (entry.environment() instanceof PeripheralEnvironment environment && environment.valid()) {
                                result.put(component.address(), environment.peripheral);
                            }
                        }
                    }
                }
                return result;
            }
            @Override public IPeripheral getAvailablePeripheral(String name) { return getAvailablePeripherals().get(name); }
            @Override public String mount(String location, Mount mount, String driveName) { return mount(location, mount, driveName, false); }
            @Override public String mountWritable(String location, WritableMount mount, String driveName) { return mount(location, mount, driveName, true); }
            private String mount(String location, Mount mount, String label, boolean writable) {
                requireAttached();
                final String path = dan200.computercraft.core.filesystem.FileSystem.sanitizePath(location, false);
                if (path.isEmpty() || path.equals("..") || path.startsWith("../") || mounts.containsKey(path)) return null;
                final var fileSystem = new MountFileSystem(mount, writable);
                final var environment = li.cil.oc.api.FileSystem.asManagedEnvironment(fileSystem, label);
                mounts.put(path, environment); context.node().connect(environment.node()); return path;
            }
            @Override public void unmount(String location) {
                requireAttached(); if (location == null) return;
                final var environment = mounts.remove(location);
                if (environment == null) throw new IllegalStateException("mount does not exist or belongs to another peripheral");
                environment.node().remove();
            }
            @Override public long issueMainThreadTask(LuaTask task) throws LuaException {
                requireAttached(); final long id = nextTaskId();
                final Runnable queue = () -> { if (active) tasks.add(new PendingTask(id, world.getGameTime() + 1, this, task)); };
                if (world.getServer().isSameThread()) queue.run(); else world.getServer().execute(queue);
                return id;
            }
            @Override public MethodResult executeMainThreadTask(LuaTask task) throws LuaException {
                requireAttached();
                if (!world.getServer().isSameThread()) throw new IllegalStateException("main-thread task called outside server thread");
                return MethodResult.of(task.execute());
            }
            @Override public WorkMonitor getMainThreadMonitor() { requireAttached(); return this; }
            @Override public boolean canWork() { return active && !invalid && context.isRunning() && !context.isPaused() && blockedTick != world.getGameTime(); }
            @Override public boolean shouldWork() { return canWork(); }
            @Override public void trackWork(long time, TimeUnit unit) {
                // Charge the normal OC peripheral operation cost without throwing after a completed side effect.
                try { context.consumeCallBudget(0.01); }
                catch (LimitReachedException exception) { blockedTick = world.getGameTime(); }
            }
        }
    }
}
