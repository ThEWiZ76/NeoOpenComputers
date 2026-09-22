package li.cil.oc.common.machine;

import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.ExecutionResult;
import li.cil.oc.api.machine.Machine;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

final class NativeLuaArchitectureTest {
    @Callback(direct = true) public void get() {}
    @Callback public void write() {}

    @ParameterizedTest
    @ValueSource(strings = {
        "while true do end",
        "while true do pcall(function() while true do end end) end",
        "while true do xpcall(function() while true do end end, function() while true do end end) end",
        "local co = coroutine.create(function() while true do end end); local ok, reason = coroutine.resume(co); assert(ok, tostring(reason))"
    })
    void terminatesNonYieldingProgramsWithDeterministicClock(final String program) throws Exception {
        final var architecture = architecture(program, new AtomicInteger());
        try {
            assertTrue(architecture.initialize());
            assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));
            final var error = assertInstanceOf(ExecutionResult.Error.class, architecture.runThreaded(false));
            assertTrue(error.message.contains("too long without yielding"), error.message);
        } finally { architecture.close(); }
    }

    @Test
    void sandboxHasNoHostLibrariesOrBinaryLoader() throws Exception {
        final var architecture = architecture("""
            assert(io == nil and package == nil and java == nil and eris == nil)
            assert(debug.getregistry == nil and debug.sethook == nil and debug.setupvalue == nil)
            local secret = {value = 42}
            local function closure() return secret end
            assert(select("#", debug.getupvalue(closure, 1)) == 1)
            assert(debug.getinfo(closure, "f").func == nil)
            assert(dofile == nil and loadfile == nil)
            assert(load(string.dump(function() return 42 end)) == nil)
            computer.shutdown()
            """, new AtomicInteger());
        try {
            assertTrue(architecture.initialize());
            architecture.runThreaded(false);
            final var result = architecture.runThreaded(false);
            assertInstanceOf(ExecutionResult.Shutdown.class, result,
                result instanceof ExecutionResult.Error error ? error.message : "Expected normal shutdown");
        } finally { architecture.close(); }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void restoresPendingWorldCallbackWithoutRepeatingIt(final boolean afterCallback) throws Exception {
        final String program = """
            local retained = {value = 731}
            retained.self = retained
            local result = component.invoke("eeprom", "write")
            assert(result == 42 and retained.value == 731 and retained.self == retained)
            assert(("").find == string.find, "string metatable was not restored")
            computer.shutdown()
            """;
        final AtomicInteger calls = new AtomicInteger();
        final CompoundTag saved = new CompoundTag();
        final var original = architecture(program, calls);
        try {
            assertTrue(original.initialize());
            original.runThreaded(false);
            assertInstanceOf(ExecutionResult.Sleep.class, original.runThreaded(false));
            assertTrue(original.hasPendingSynchronizedCall());
            if (afterCallback) original.runSynchronized();
            original.save(saved);
            assertEquals(afterCallback ? 1 : 0, calls.get(), "Saving must not invoke the callback");
        } finally { original.close(); }
        final var restored = architecture(program, calls);
        try {
            restored.load(saved);
            assertEquals(!afterCallback, restored.hasPendingSynchronizedCall());
            assertEquals(afterCallback, restored.hasSynchronizedReturn());
            if (!afterCallback) restored.runSynchronized();
            final var result = restored.runThreaded(true);
            assertInstanceOf(ExecutionResult.Shutdown.class, result,
                result instanceof ExecutionResult.Error error ? error.message : "Expected normal shutdown");
            assertEquals(1, calls.get());
        } finally { restored.close(); }
    }

    private static NativeLuaArchitecture architecture(final String program, final AtomicInteger writes) throws Exception {
        final AtomicLong clock = new AtomicLong();
        final var architecture = new NativeLuaArchitecture(() -> clock.getAndAdd(100_000_000L));
        final var memory = NativeLuaArchitecture.class.getDeclaredField("memoryBytes");
        memory.setAccessible(true);
        memory.setInt(architecture, 512 * 1024);
        final Map<String, Callback> callbacks = Map.of(
            "get", NativeLuaArchitectureTest.class.getMethod("get").getAnnotation(Callback.class),
            "write", NativeLuaArchitectureTest.class.getMethod("write").getAnnotation(Callback.class));
        architecture.bind((Machine) Proxy.newProxyInstance(Machine.class.getClassLoader(), new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "components" -> Map.of("eeprom", "eeprom");
                case "methods" -> callbacks;
                case "popSignal" -> null;
                case "upTime", "cpuTime" -> 0D;
                case "invoke" -> {
                    if (args[1].equals("get")) yield new Object[]{program};
                    if (args[1].equals("write")) { writes.incrementAndGet(); yield new Object[]{42}; }
                    throw new AssertionError("Unexpected callback " + args[1]);
                }
                case "crash" -> throw new AssertionError(args[0]);
                default -> throw new AssertionError("Unexpected machine call " + method.getName());
            }));
        return architecture;
    }
}
