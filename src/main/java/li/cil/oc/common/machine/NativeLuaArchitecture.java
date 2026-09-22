package li.cil.oc.common.machine;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.Memory;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.ExecutionResult;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ModSettings;
import li.cil.repack.com.naef.jnlua.JavaFunction;
import li.cil.repack.com.naef.jnlua.LuaState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/** Native architecture integration. Not registered as a selectable/default CPU yet. */
@Architecture.Name("Lua 5.2 (native)")
public final class NativeLuaArchitecture implements Architecture, MachineBoundArchitecture, SynchronizedCallAware {
    private final LongSupplier nanoTime;
    private Machine machine;
    private NativeLuaState owner;
    private NativeLuaPersistence persistence;
    private LuaState lua;
    private int memoryBytes;
    private double ramScale = 1D;
    private int kernelMemory;
    private boolean pendingCall;
    private boolean pendingReturn;
    private String bootAddress;

    public NativeLuaArchitecture() { this(System::nanoTime); }
    NativeLuaArchitecture(final LongSupplier nanoTime) { this.nanoTime = nanoTime; }
    @Override public void bind(final Machine machine) { this.machine = machine; }
    @Override public boolean isInitialized() { return owner != null; }
    @Override public boolean hasPendingSynchronizedCall() { return pendingCall; }
    @Override public boolean hasSynchronizedReturn() { return pendingReturn; }
    @Override public void onSignal() {}
    @Override public void onConnect() {}

    @Override
    public boolean recomputeMemory(final Iterable<ItemStack> components) {
        double memory = 0;
        for (final ItemStack stack : components) {
            if (Driver.driverFor(stack) instanceof Memory ram) memory += Math.max(0, ram.amount(stack)) * 1024;
        }
        memoryBytes = (int) Math.min(ModSettings.maxTotalRam(), memory);
        if (lua != null && kernelMemory > 0) lua.setTotalMemory(memoryLimit(kernelMemory));
        return memoryBytes > 0;
    }

    @Override
    public boolean initialize() {
        close();
        try {
            owner = NativeLuaState.create(NativeLuaState.Version.LUA52, 2 * 1024 * 1024 + memoryBytes);
            lua = owner.state();
            ramScale = lua.getPointerWidth() >= 8 ? ModSettings.ramScaleFor64Bit() : 1D;
            lua.setTotalMemory(memoryLimit(2 * 1024 * 1024));
            // The trusted upstream kernel creates its own restricted user sandbox.
            owner.pushHostLibrary("debug"); lua.setGlobal("debug");
            installComputer();
            installComponent();
            NativeLuaUserdata.install(lua, machine);
            NativeLuaLibraries.installUnicode(lua);
            NativeLuaLibraries.installOs(lua, machine);
            lua.newTable();
            function("timeout", state -> number(ModSettings.computerTimeout()));
            function("allowBytecode", state -> bool(false));
            function("allowGC", state -> bool(ModSettings.allowGc()));
            lua.setGlobal("system");
            lua.pushJavaFunction(state -> { state.pushString(persistence.persistenceKey()); return 1; });
            lua.setGlobal("persistKey");
            persistence = new NativeLuaPersistence(owner);
            try (final var source = NativeLuaArchitecture.class.getResourceAsStream("/assets/neoopencomputers/lua/native/machine.lua")) {
                if (source == null) throw new IllegalStateException("Missing native Lua kernel");
                lua.load(source, "=machine", "t");
            }
            lua.newThread();
            return true;
        } catch (Exception | LinkageError failure) {
            close();
            machine.crash("Native Lua initialization failed: " + failure.getMessage());
            return false;
        }
    }

    @Override
    public void runSynchronized() {
        if (!pendingCall) return;
        lua.call(0, 1);
        if (!lua.isTable(2)) throw new IllegalStateException("Invalid native synchronized result");
        pendingCall = false;
        pendingReturn = true;
    }

    @Override
    public ExecutionResult runThreaded(final boolean synchronizedReturn) {
        if (lua == null) return new ExecutionResult.Error("Native Lua is not initialized");
        try {
            final int results;
            if (pendingReturn) {
                pendingReturn = false;
                results = lua.resume(1, 1);
            } else if (kernelMemory == 0) {
                final int initialResults = lua.resume(1, 0);
                if (lua.status(1) != LuaState.YIELD || initialResults != 0) return stopped();
                lua.gc(LuaState.GcAction.COLLECT, 0);
                kernelMemory = Math.max(1, lua.getTotalMemory() - lua.getFreeMemory());
                lua.setTotalMemory(memoryLimit(kernelMemory));
                return new ExecutionResult.Sleep(0);
            } else {
                final var signal = machine.popSignal();
                int args = 0;
                if (signal != null) {
                    lua.pushString(signal.name());
                    args++;
                    for (final Object argument : signal.args()) { NativeLuaValues.push(lua, argument); args++; }
                }
                results = lua.resume(1, args);
            }
            if (lua.status(1) != LuaState.YIELD) return stopped();
            if (results == 1 && lua.isFunction(2)) {
                pendingCall = true;
                return new ExecutionResult.Sleep(0); // SimpleMachine's synchronized-call loop.
            }
            if (results == 1 && lua.isBoolean(2)) return new ExecutionResult.Shutdown(lua.toBoolean(2));
            final int ticks = results == 1 && lua.isNumber(2) ? (int) Math.min(Integer.MAX_VALUE, Math.max(0, lua.toNumber(2) * 20)) : Integer.MAX_VALUE;
            lua.pop(results);
            return new ExecutionResult.Sleep(ticks);
        } catch (RuntimeException failure) {
            return new ExecutionResult.Error(failure.getMessage());
        }
    }

    private ExecutionResult stopped() {
        final String message = lua.getTop() >= 3 && lua.isString(3) ? lua.toString(3) : "computer halted";
        return new ExecutionResult.Error(message);
    }

    @Override
    public void save(final CompoundTag nbt) {
        if (lua == null) return;
        final int top = lua.getTop();
        final int executionLimit = lua.getTotalMemory();
        // Serialization needs a working copy; this reserve is not available to programs.
        lua.setTotalMemory((int) Math.min(Integer.MAX_VALUE,
            (long) executionLimit + Math.max(2L * 1024 * 1024, 4L * executionLimit)));
        try {
            lua.newTable();
            lua.pushValue(1); lua.setField(-2, "thread");
            if (top == 2) { lua.pushValue(2); lua.setField(-2, "pending"); }
            lua.getGlobal("_G"); lua.setField(-2, "globals");
            // Strings share a VM-wide metatable, outside the ordinary object graph.
            // Its __index must retain the kernel's bounded pattern functions on reload.
            lua.pushString(""); lua.getMetatable(-1); lua.remove(-2);
            lua.setField(-2, "stringMetatable");
            nbt.put("snapshot", persistence.save(-1));
            nbt.putInt("kernelMemory", kernelMemory);
            nbt.putDouble("ramScale", ramScale);
            nbt.putBoolean("pendingCall", pendingCall);
            nbt.putBoolean("pendingReturn", pendingReturn);
            if (bootAddress != null) nbt.putString("bootAddress", bootAddress);
        } finally {
            lua.setTop(top);
            lua.gc(LuaState.GcAction.COLLECT, 0);
            lua.setTotalMemory(executionLimit);
        }
    }

    @Override
    public void load(final CompoundTag nbt) {
        if (!nbt.contains("snapshot")) return;
        if (!initialize()) throw new IllegalStateException("Native Lua initialization failed during restore");
        lua.setTop(0);
        persistence.restore(nbt.getCompound("snapshot"));
        lua.getField(1, "globals"); lua.rawSet(lua.getRegistryIndex(), LuaState.RIDX_GLOBALS);
        lua.pushString(""); lua.getField(1, "stringMetatable");
        lua.setMetatable(-2); lua.pop(1);
        lua.getField(1, "thread");
        pendingCall = nbt.getBoolean("pendingCall");
        pendingReturn = nbt.getBoolean("pendingReturn");
        if (pendingCall || pendingReturn) lua.getField(1, "pending");
        lua.remove(1);
        final double savedScale = nbt.contains("ramScale") ? Math.max(1D, nbt.getDouble("ramScale")) : 1D;
        kernelMemory = (int) Math.ceil(nbt.getInt("kernelMemory") / savedScale * ramScale);
        lua.gc(LuaState.GcAction.COLLECT, 0);
        if (kernelMemory > 0) lua.setTotalMemory(memoryLimit(kernelMemory));
        bootAddress = nbt.contains("bootAddress") ? nbt.getString("bootAddress") : null;
    }

    @Override
    public void close() {
        if (persistence != null) persistence.close();
        if (owner != null) owner.close();
        persistence = null; owner = null; lua = null;
        kernelMemory = 0; pendingCall = false; pendingReturn = false;
    }

    private void installComputer() {
        lua.newTable();
        function("realTime", state -> number(nanoTime.getAsLong() / 1e9));
        function("uptime", state -> number(machine.upTime()));
        function("address", state -> { NativeLuaValues.push(state, machine.node().address()); return 1; });
        function("tmpAddress", state -> { NativeLuaValues.push(state, machine.tmpAddress()); return 1; });
        function("isRobot", state -> bool(machine.host() instanceof Robot));
        function("freeMemory", state -> number(Math.max(0, Math.min(memoryBytes, (int) (lua.getFreeMemory() / ramScale)))));
        function("totalMemory", state -> number(memoryBytes));
        function("energy", state -> number(machine.node() instanceof Connector node ? node.globalBuffer() : 0));
        function("maxEnergy", state -> number(machine.node() instanceof Connector node ? node.globalBufferSize() : 0));
        function("users", state -> { for (final String user : machine.users()) state.pushString(user); return machine.users().length; });
        function("removeUser", state -> bool(machine.removeUser(state.checkString(1))));
        function("addUser", state -> { try { machine.addUser(state.checkString(1)); return bool(true); } catch (Exception failure) { state.pushNil(); state.pushString(failure.getMessage()); return 2; } });
        function("pushSignal", state -> bool(machine.signal(state.checkString(1), NativeLuaValues.arguments(state, 2))));
        function("getBootAddress", state -> { NativeLuaValues.push(state, bootAddress); return 1; });
        function("setBootAddress", state -> { bootAddress = state.isNoneOrNil(1) ? null : state.checkString(1); return 0; });
        function("getArchitecture", state -> { state.pushString("Lua 5.2 (native)"); return 1; });
        lua.setGlobal("computer");
    }

    private void installComponent() {
        lua.newTable();
        function("list", state -> {
            final String filter = state.isString(1) ? state.toString(1) : null;
            final boolean exact = state.toBoolean(2);
            final Map<String, String> result = new LinkedHashMap<>();
            machine.components().forEach((address, name) -> { if (filter == null || (exact ? name.equals(filter) : name.contains(filter))) result.put(address, name); });
            NativeLuaValues.push(state, result); return 1;
        });
        function("type", state -> { NativeLuaValues.push(state, machine.components().get(state.checkString(1))); return 1; });
        function("slot", state -> number(machine.host().componentSlot(state.checkString(1))));
        function("methods", state -> {
            final Map<String, Object> methods = new LinkedHashMap<>();
            machine.methods(state.checkString(1)).forEach((name, method) -> methods.put(name,
                Map.of("direct", method.direct(), "getter", method.getter(), "setter", method.setter())));
            NativeLuaValues.push(state, methods); return 1;
        });
        function("doc", state -> {
            final var method = machine.methods(state.checkString(1)).get(state.checkString(2));
            NativeLuaValues.push(state, method == null ? null : method.doc()); return 1;
        });
        function("invoke", state -> NativeLuaValues.invoke(state, () ->
            machine.invoke(state.checkString(1), state.checkString(2), NativeLuaValues.arguments(state, 3))));
        lua.setGlobal("component");
    }

    private void function(final String name, final JavaFunction callback) { lua.pushJavaFunction(callback); lua.setField(-2, name); }
    private int bool(final boolean value) { lua.pushBoolean(value); return 1; }
    private int number(final double value) { lua.pushNumber(value); return 1; }

    private int memoryLimit(final int base) {
        return (int) Math.min(Integer.MAX_VALUE, base + Math.ceil(memoryBytes * ramScale));
    }
}
