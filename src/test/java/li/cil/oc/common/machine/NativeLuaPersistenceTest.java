package li.cil.oc.common.machine;

import li.cil.repack.com.naef.jnlua.LuaState;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

final class NativeLuaPersistenceTest {
    private static final String TEST_KEY = "__oc_persist_test_fixed";

    @ParameterizedTest
    @EnumSource(NativeLuaState.Version.class)
    void restoresGlobalsClosuresNestedThreadsCyclesAndFreshCallbacks(final NativeLuaState.Version version) throws Exception {
        final AtomicInteger firstCalls = new AtomicInteger();
        final CompoundTag saved;
        try (final var state = NativeLuaState.create(version, 4 * 1024 * 1024)) {
            installCallbacks(state.state(), firstCalls, false);
            try (final var persistence = new NativeLuaPersistence(state, TEST_KEY)) {
                final var lua = state.state();
                execute(lua, """
                    savedGlobal = {value = 731, binary = string.char(0, 255, 128, 65)}
                    savedGlobal.self = savedGlobal
                    customLoad = load
                    local thread = coroutine.create(function()
                        local state = savedGlobal
                        local child = coroutine.create(function()
                            local count = 40
                            coroutine.yield("nested")
                            return count + 2
                        end)
                        local ok, value = coroutine.resume(child)
                        assert(ok and value == "nested")
                        local bound = hostA
                        local command = coroutine.yield("waiting")
                        assert(command == "continue")
                        local childOk, childValue = coroutine.resume(child)
                        assert(childOk and childValue == 42)
                        state.value = state.value + 1
                        assert(state == savedGlobal and state.self == state)
                        assert(state.binary == string.char(0, 255, 128, 65))
                        assert(bound(7) == 12 and hostB(2) == 7)
                        assert(customLoad("return savedGlobal.value")() == 732)
                        return "resumed"
                    end)
                    local ok, value = coroutine.resume(thread)
                    assert(ok and value == "waiting")
                    return {globals = _G, thread = thread}
                    """, 1);
                saved = persistence.save(-1);
                assertEquals(1, lua.getTop());
                assertEquals(0, firstCalls.get(), "Saving must not replay the program");
            }
        }
        final AtomicInteger restoredCalls = new AtomicInteger();
        try (final var state = NativeLuaState.create(version, 4 * 1024 * 1024)) {
            // Binding traversal must be independent of insertion order and Java identities.
            installCallbacks(state.state(), restoredCalls, true);
            try (final var persistence = new NativeLuaPersistence(state, "__oc_persist_fresh_fixed")) {
                final var lua = state.state();
                persistence.restore(saved);
                assertEquals(1, lua.getTop());
                final CompoundTag savedAgain = persistence.save(-1);
                assertEquals(TEST_KEY, savedAgain.getString("persistKey"));
                lua.pop(1);
                persistence.restore(savedAgain);
                lua.getField(-1, "globals");
                lua.rawSet(lua.getRegistryIndex(), LuaState.RIDX_GLOBALS);
                lua.getField(-1, "thread");
                lua.setGlobal("restoredThread");
                lua.pop(1);
                execute(lua, """
                    local ok, value = coroutine.resume(restoredThread, "continue")
                    assert(ok, value)
                    assert(value == "resumed" and savedGlobal.value == 732)
                    assert(coroutine.status(restoredThread) == "dead")
                    """, 0);
                assertEquals(2, restoredCalls.get());
                assertEquals(0, firstCalls.get());
                assertEquals(0, lua.getTop());
            }
        }
    }

    @ParameterizedTest
    @EnumSource(NativeLuaState.Version.class)
    void doesNotRunUserPersistMetamethodsAndRejectsBadEnvelope(final NativeLuaState.Version version) throws Exception {
        try (final var state = NativeLuaState.create(version, 4 * 1024 * 1024);
             final var persistence = new NativeLuaPersistence(state, TEST_KEY)) {
            final var lua = state.state();
            execute(lua, "return setmetatable({value = 42}, {__persist = function() error('user hook executed') end})", 1);
            final var saved = persistence.save(-1);
            persistence.restore(saved);
            assertEquals(2, lua.getTop());
            lua.getField(-1, "value");
            assertEquals(42, lua.toInteger(-1));
            lua.setTop(0);
            final var incompatible = saved.copy();
            incompatible.putString("lua", "other-version");
            assertThrows(IllegalArgumentException.class, () -> persistence.restore(incompatible));
            final var empty = saved.copy();
            empty.putByteArray("data", new byte[0]);
            assertThrows(IllegalArgumentException.class, () -> persistence.restore(empty));
            assertEquals(0, lua.getTop());
            persistence.close();
            assertThrows(IllegalStateException.class, () -> persistence.restore(saved));
        }
    }

    @ParameterizedTest
    @EnumSource(NativeLuaState.Version.class)
    void failedNativeRestoreDoesNotLeakStackEntries(final NativeLuaState.Version version) throws Exception {
        try (final var state = NativeLuaState.create(version, 4 * 1024 * 1024);
             final var persistence = new NativeLuaPersistence(state, TEST_KEY)) {
            final var bad = new CompoundTag();
            bad.putInt("format", 1);
            bad.putString("lua", version.name());
            bad.putString("persistKey", TEST_KEY);
            bad.putByteArray("data", new byte[]{1, 2, 3});
            state.state().pushInteger(99);
            assertThrows(RuntimeException.class, () -> persistence.restore(bad));
            assertEquals(1, state.state().getTop());
            assertEquals(99, state.state().toInteger(-1));
        }
    }

    @ParameterizedTest
    @EnumSource(NativeLuaState.Version.class)
    void missingHostBindingFailsInsteadOfSubstitutingAnotherCallback(final NativeLuaState.Version version) throws Exception {
        final CompoundTag saved;
        final AtomicInteger calls = new AtomicInteger();
        try (final var source = NativeLuaState.create(version, 4 * 1024 * 1024)) {
            installCallbacks(source.state(), calls, false);
            try (final var persistence = new NativeLuaPersistence(source, TEST_KEY)) {
                source.state().getGlobal("hostA");
                saved = persistence.save(-1);
            }
        }
        try (final var target = NativeLuaState.create(version, 4 * 1024 * 1024);
             final var persistence = new NativeLuaPersistence(target, TEST_KEY)) {
            target.state().pushInteger(99);
            assertThrows(RuntimeException.class, () -> persistence.restore(saved));
            assertEquals(1, target.state().getTop());
            assertEquals(99, target.state().toInteger(-1));
            assertEquals(0, calls.get());
        }
    }

    private static void execute(final LuaState lua, final String source, final int returns) {
        lua.load(source, "=persistence-test");
        lua.call(0, returns);
    }

    private static void installCallbacks(final LuaState lua, final AtomicInteger calls, final boolean reverse) {
        for (final String name : reverse ? new String[]{"hostB", "hostA"} : new String[]{"hostA", "hostB"}) {
            lua.pushJavaFunction(state -> {
                calls.incrementAndGet();
                state.pushInteger(state.checkInteger(1) + 5);
                return 1;
            });
            lua.setGlobal(name);
        }
    }
}
