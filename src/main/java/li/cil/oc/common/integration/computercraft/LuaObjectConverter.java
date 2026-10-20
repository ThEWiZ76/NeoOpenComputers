package li.cil.oc.common.integration.computercraft;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaTask;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.core.asm.LuaMethodSupplier;
import dan200.computercraft.core.methods.LuaMethod;
import dan200.computercraft.core.methods.MethodSupplier;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.LimitReachedException;
import li.cil.oc.api.network.ManagedPeripheral;
import li.cil.oc.common.integration.ComputerCraftLuaObjectValue;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Foreign implementations stay here; the persisted OC Value has no CC type references. */
public final class LuaObjectConverter implements Converter {
    private static final MethodSupplier<LuaMethod> METHODS = LuaMethodSupplier.create(List.of());
    private final List<PendingTask> pending = new ArrayList<>();
    private final Set<Handle> handles = Collections.newSetFromMap(new WeakHashMap<>());

    public void registerEvents() {
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::stopped);
    }

    @Override public void convert(Object value, Map<Object, Object> output) {
        final var methods = METHODS.getSelfMethods(value);
        if (methods.isEmpty()) return;
        final var handle = new Handle(value, methods);
        synchronized (pending) { handles.add(handle); }
        output.put("value", new ComputerCraftLuaObjectValue(handle));
    }

    private void tick(ServerTickEvent.Post event) {
        final var ready = new ArrayList<PendingTask>();
        synchronized (pending) {
            pending.removeIf(task -> {
                if (!task.call.owner.active || !task.call.context.isRunning()) return true;
                if (task.call.server == event.getServer() && task.tick <= event.getServer().getTickCount()) {
                    ready.add(task); return true;
                }
                return false;
            });
        }
        for (var task : ready) {
            if (!task.call.owner.active) continue;
            final var result = complete(task);
            task.call.context.signal("task_complete", java.util.Arrays.copyOfRange(result, 1, result.length));
        }
    }

    private void stopped(ServerStoppedEvent event) {
        synchronized (pending) {
            for (var handle : handles) handle.active = false;
            handles.clear(); pending.clear();
        }
    }

    private static Object[] complete(PendingTask task) {
        try {
            return ComputerCraftArguments.taskCompletion(task.id, task.task.execute());
        } catch (Exception failure) { return new Object[]{"task_complete", task.id, false, failure.getMessage()}; }
    }

    private final class Handle implements ManagedPeripheral, AutoCloseable {
        final Object value;
        final Map<String, LuaMethod> methods;
        volatile boolean active = true;
        Handle(Object value, Map<String, LuaMethod> methods) { this.value = value; this.methods = methods; }
        @Override public String[] methods() { return active ? methods.keySet().toArray(String[]::new) : new String[0]; }

        @Override public Object[] invoke(String method, Context context, Arguments args) throws Exception {
            final var server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) throw new IllegalStateException("ComputerCraft object requires a server");
            if (!server.isSameThread()) throw new LimitReachedException();
            if (!active) throw new IllegalStateException("ComputerCraft object is unavailable");
            final var function = methods.get(method);
            if (function == null) throw new IllegalArgumentException("no such Lua object method: " + method);
            if (context == null || context.node() == null) throw new IllegalArgumentException("Lua object requires a computer context");
            final Object[] raw = new Object[args.count()];
            for (int i = 0; i < raw.length; i++) raw[i] = args.checkAny(i);
            final var call = new Call(this, context, server);
            try {
                var result = function.apply(value, call, new ComputerCraftArguments(raw));
                for (int step = 0; result.getCallback() != null; step++) {
                    final var filter = result.getResult();
                    final PendingTask task;
                    synchronized (pending) {
                        if (step >= 64 || filter == null || filter.length != 1 || !"task_complete".equals(filter[0]) || call.requested.isEmpty()) {
                            throw new UnsupportedOperationException("Lua object event waits/yields are unsupported");
                        }
                        final long id = call.requested.keySet().iterator().next();
                        task = call.requested.remove(id); pending.remove(task);
                    }
                    if (!active) throw new IllegalStateException("ComputerCraft object is unavailable");
                    result = result.getCallback().resume(complete(task));
                }
                return ComputerCraftArguments.results(result.getResult());
            } finally {
                synchronized (pending) { call.invoking = false; call.requested.clear(); }
            }
        }

        @Override public void close() {
            active = false;
            synchronized (pending) { handles.remove(this); pending.removeIf(task -> task.call.owner == this); }
        }
    }

    private final class Call implements ILuaContext {
        final Handle owner;
        final Context context;
        final MinecraftServer server;
        final Map<Long, PendingTask> requested = new LinkedHashMap<>();
        boolean invoking = true;
        Call(Handle owner, Context context, MinecraftServer server) { this.owner = owner; this.context = context; this.server = server; }
        private void requireActive() {
            if (!owner.active || ServerLifecycleHooks.getCurrentServer() != server) throw new IllegalStateException("ComputerCraft object is unavailable");
        }
        @Override public long issueMainThreadTask(LuaTask task) throws LuaException {
            requireActive();
            final long id = PeripheralDriver.PeripheralEnvironment.nextTaskId();
            final var queued = new PendingTask(id, server.getTickCount() + 1, this, task);
            synchronized (pending) { pending.add(queued); if (invoking) requested.put(id, queued); }
            return id;
        }
        @Override public MethodResult executeMainThreadTask(LuaTask task) throws LuaException {
            requireActive();
            if (!server.isSameThread()) throw new IllegalStateException("main-thread task called outside server thread");
            return MethodResult.of(task.execute());
        }
    }

    private record PendingTask(long id, int tick, Call call, LuaTask task) { }
}
