package li.cil.oc.common.machine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

final class NativeLuaStateTest {
    @Test
    void selectsKnownPlatformNamesAndRejectsUnknownTargets() {
        assertEquals("libjnlua52-windows-x86_64.dll", NativeLuaState.libraryName(NativeLuaState.Version.LUA52, "Windows 11", "amd64"));
        assertEquals("libjnlua53-linux-aarch64.so", NativeLuaState.libraryName(NativeLuaState.Version.LUA53, "Linux", "arm64"));
        assertEquals("libjnlua54-darwin-aarch64.dylib", NativeLuaState.libraryName(NativeLuaState.Version.LUA54, "Mac OS X", "aarch64"));
        assertThrows(IllegalArgumentException.class, () -> NativeLuaState.libraryName(NativeLuaState.Version.LUA52, "unknown", "amd64"));
        assertThrows(IllegalArgumentException.class, () -> NativeLuaState.libraryName(NativeLuaState.Version.LUA52, "Linux", "../foreign"));
        assertThrows(IllegalArgumentException.class, () -> NativeLuaState.create(NativeLuaState.Version.LUA52, 0));
    }

    @ParameterizedTest
    @EnumSource(NativeLuaState.Version.class)
    void opensBoundedVmWithoutHostIoOrBinaryLoading(final NativeLuaState.Version version) throws Exception {
        final var owner = NativeLuaState.create(version, 1024 * 1024);
        final var lua = owner.state();
        try (owner) {
            assertEquals(1024 * 1024, lua.getTotalMemory());
            assertTrue(lua.getFreeMemory() > 0);
            lua.load("""
                assert(io == nil and os == nil and package == nil and java == nil)
                assert(debug == nil and eris == nil and dofile == nil and loadfile == nil)
                assert(coroutine and string and math and table)
                assert(load("return 21 * 2")() == 42)
                local bytecode = string.dump(function() return 1 end)
                assert(load(bytecode, "binary", "b") == nil)
                return _VERSION
                """, "=sandbox-test");
            lua.call(0, 1);
            assertEquals(switch (version) {
                case LUA52 -> "Lua+Eris 5.2";
                case LUA53 -> "Lua+Eris 5.3";
                case LUA54 -> "Lua+Eris 5.4";
            }, lua.toString(-1));
            lua.pop(1);
            owner.pushHostLibrary("eris");
            assertTrue(lua.isTable(-1));
            lua.pop(1);
            owner.pushHostLibrary("debug");
            assertTrue(lua.isTable(-1));
            lua.pop(1);
            assertThrows(IllegalArgumentException.class, () -> owner.pushHostLibrary("io"));
            assertEquals(0, lua.getTop());
        }
        assertFalse(lua.isOpen());
        owner.close();
    }

    @ParameterizedTest
    @EnumSource(NativeLuaState.Version.class)
    void rejectsAllocationBeyondConfiguredMemory(final NativeLuaState.Version version) throws Exception {
        try (final var owner = NativeLuaState.create(version, 1024 * 1024)) {
            final var lua = owner.state();
            lua.load("return string.rep('x', 8 * 1024 * 1024)", "=memory-test");
            final RuntimeException failure = assertThrows(RuntimeException.class, () -> lua.call(0, 1));
            assertTrue(failure.getMessage().toLowerCase(java.util.Locale.ROOT).contains("memory"), failure::toString);
        }
    }
}
