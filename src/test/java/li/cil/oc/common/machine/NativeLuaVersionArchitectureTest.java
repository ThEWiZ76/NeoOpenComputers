package li.cil.oc.common.machine;

import li.cil.oc.api.machine.ExecutionResult;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

final class NativeLuaVersionArchitectureTest {
    @ParameterizedTest
    @EnumSource(value = NativeLuaState.Version.class, names = {"LUA53", "LUA54"})
    void pendingWorldCallbackSurvivesSaveBeforeAndAfterExecution(final NativeLuaState.Version version) throws Exception {
        for (final boolean afterCallback : new boolean[]{false, true}) {
            final String program = """
                local retained = {value=731}; retained.self=retained
                assert(component.invoke('eeprom', 'write') == 42)
                assert(retained.value == 731 and retained.self == retained)
                computer.shutdown()
                """;
            final AtomicInteger calls = new AtomicInteger();
            final CompoundTag saved = new CompoundTag();
            final var original = NativeLuaArchitectureTest.architecture(program, calls, null, version);
            try {
                assertTrue(original.initialize());
                original.runThreaded(false);
                assertInstanceOf(ExecutionResult.Sleep.class, original.runThreaded(false));
                assertTrue(original.hasPendingSynchronizedCall());
                if (afterCallback) original.runSynchronized();
                original.save(saved);
                assertEquals(version.name(), saved.getCompound("snapshot").getString("lua"));
                assertEquals(afterCallback ? 1 : 0, calls.get());
            } finally { original.close(); }
            final var restored = NativeLuaArchitectureTest.architecture(program, calls, null, version);
            try {
                restored.load(saved);
                assertEquals(!afterCallback, restored.hasPendingSynchronizedCall());
                assertEquals(afterCallback, restored.hasSynchronizedReturn());
                if (!afterCallback) restored.runSynchronized();
                assertInstanceOf(ExecutionResult.Shutdown.class, restored.runThreaded(true));
                assertEquals(1, calls.get(), "Restoring must not repeat world mutations");
            } finally { restored.close(); }
        }
    }

    @ParameterizedTest
    @EnumSource(value = NativeLuaState.Version.class, names = {"LUA53", "LUA54"})
    void enforcesSandboxAndInstalledMemory(final NativeLuaState.Version version) throws Exception {
        final var architecture = NativeLuaArchitectureTest.architecture("""
            assert(io == nil and package == nil and java == nil and eris == nil)
            assert(debug.getregistry == nil and debug.sethook == nil and debug.setupvalue == nil)
            assert(dofile == nil and loadfile == nil)
            assert(load(string.dump(function() return 42 end)) == nil)
            assert(computer.totalMemory() == 512 * 1024)
            assert(computer.freeMemory() > 0 and computer.freeMemory() <= computer.totalMemory())
            assert(not pcall(string.rep, 'x', 8 * 1024 * 1024))
            computer.shutdown()
            """, new AtomicInteger(), null, version);
        try {
            assertTrue(architecture.initialize());
            architecture.runThreaded(false);
            final var result = architecture.runThreaded(false);
            assertInstanceOf(ExecutionResult.Shutdown.class, result,
                result instanceof ExecutionResult.Error error ? error.message : "Expected normal shutdown");
        } finally { architecture.close(); }
    }

    @ParameterizedTest
    @EnumSource(value = NativeLuaState.Version.class, names = {"LUA53", "LUA54"})
    void deadlineCannotBeSwallowedByProtectedCallsOrNestedCoroutines(final NativeLuaState.Version version) throws Exception {
        for (final String program : new String[]{
            "while true do end",
            "while true do pcall(function() while true do end end) end",
            "while true do xpcall(function() while true do end end, function() while true do end end) end",
            "local co = coroutine.create(function() while true do end end); local ok, reason = coroutine.resume(co); assert(ok, tostring(reason))"
        }) {
            final var architecture = NativeLuaArchitectureTest.architecture(program, new AtomicInteger(), null, version);
            try {
                assertTrue(architecture.initialize());
                assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));
                final var error = assertInstanceOf(ExecutionResult.Error.class, architecture.runThreaded(false));
                assertTrue(error.message.contains("too long without yielding"), error.message);
            } finally { architecture.close(); }
        }
    }
}
