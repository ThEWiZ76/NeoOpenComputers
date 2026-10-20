package li.cil.oc.common.machine;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class NativeLuaArgumentsTest {
    @ParameterizedTest @EnumSource(NativeLuaState.Version.class)
    void distinctBinaryTableKeysDoNotOverwriteEachOther(NativeLuaState.Version version) throws Exception {
        try (var owner = NativeLuaState.create(version, 2 * 1024 * 1024)) {
            final var lua = owner.state();
            lua.load("return {[string.char(255)]=731,[string.char(254)]=9,label='ok'}", "=binary-keys");
            lua.call(0, 1);
            final Map<?, ?> table = (Map<?, ?>) NativeLuaValues.arguments(lua, 1)[0];
            assertEquals(3, table.size());
            for (final var entry : table.entrySet()) {
                if (entry.getKey() instanceof byte[] key) {
                    assertEquals(1, key.length);
                    assertEquals(key[0] == (byte) 255 ? 731 : 9, ((Number) entry.getValue()).intValue());
                    assertTrue(key[0] == (byte) 255 || key[0] == (byte) 254);
                } else {
                    assertEquals("label", entry.getKey());
                    assertArrayEquals(new byte[]{'o', 'k'}, (byte[]) entry.getValue());
                }
            }
            assertEquals(1, lua.getTop());
        }
    }

    @ParameterizedTest @EnumSource(NativeLuaState.Version.class)
    void unsupportedArgumentsBecomeNull(NativeLuaState.Version version) throws Exception {
        try (var owner = NativeLuaState.create(version, 2 * 1024 * 1024)) {
            final var lua = owner.state();
            lua.load("return function() end, coroutine.create(function() end), nil, 'after'", "=argument-types");
            lua.call(0, 4);
            final Object[] arguments = NativeLuaValues.arguments(lua, 1);
            assertEquals(4, arguments.length);
            assertNull(arguments[0]); assertNull(arguments[1]); assertNull(arguments[2]);
            assertArrayEquals(new byte[]{'a', 'f', 't', 'e', 'r'}, (byte[]) arguments[3]);
            assertEquals(4, lua.getTop());
        }
    }

    @ParameterizedTest @EnumSource(NativeLuaState.Version.class)
    void sharedTableValuesKeepIdentity(NativeLuaState.Version version) throws Exception {
        try (var owner = NativeLuaState.create(version, 2 * 1024 * 1024)) {
            final var lua = owner.state();
            lua.load("local child={value=731}; return {left=child,right=child}", "=shared-arguments");
            lua.call(0, 1);
            final Map<?, ?> graph = (Map<?, ?>) NativeLuaValues.arguments(lua, 1)[0];
            assertSame(graph.get("left"), graph.get("right"));
            assertEquals(731, ((Number) ((Map<?, ?>) graph.get("left")).get("value")).intValue());
            assertEquals(1, lua.getTop());
        }
    }

    @ParameterizedTest @EnumSource(NativeLuaState.Version.class)
    void cyclicTableValuesKeepIdentity(NativeLuaState.Version version) throws Exception {
        try (var owner = NativeLuaState.create(version, 2 * 1024 * 1024)) {
            final var lua = owner.state();
            lua.load("local graph={}; local child={parent=graph}; graph.self=graph; graph.child=child; return graph", "=cyclic-arguments");
            lua.call(0, 1);
            final Map<?, ?> graph = (Map<?, ?>) NativeLuaValues.arguments(lua, 1)[0];
            assertSame(graph, graph.get("self"));
            assertSame(graph, ((Map<?, ?>) graph.get("child")).get("parent"));
            assertEquals(1, lua.getTop());
        }
    }

    @ParameterizedTest @EnumSource(NativeLuaState.Version.class)
    void scalarAndBinaryArgumentsStayDetached(NativeLuaState.Version version) throws Exception {
        try (var owner = NativeLuaState.create(version, 2 * 1024 * 1024)) {
            final var lua = owner.state();
            lua.load("return false, 731, 1.25, string.char(0,128,255,42), {text='héllo', payload=string.char(0,255)}", "=scalar-arguments");
            lua.call(0, 5);
            final Object[] arguments = NativeLuaValues.arguments(lua, 1);
            assertEquals(false, arguments[0]); assertEquals(731, ((Number) arguments[1]).intValue());
            assertEquals(1.25, ((Number) arguments[2]).doubleValue());
            assertArrayEquals(new byte[]{0, (byte) 128, (byte) 255, 42}, (byte[]) arguments[3]);
            final Map<?, ?> table = (Map<?, ?>) arguments[4];
            lua.setTop(0); lua.close();
            assertArrayEquals("héllo".getBytes(java.nio.charset.StandardCharsets.UTF_8), (byte[]) table.get("text"));
            assertArrayEquals(new byte[]{0, (byte) 255}, (byte[]) table.get("payload"));
        }
    }

    @ParameterizedTest @EnumSource(NativeLuaState.Version.class)
    void excessiveNestingRestoresTheOriginalStack(NativeLuaState.Version version) throws Exception {
        try (var owner = NativeLuaState.create(version, 2 * 1024 * 1024)) {
            final var lua = owner.state();
            lua.load("local graph={}; local next=graph; for i=1,70 do next.child={}; next=next.child end; return 'sentinel',graph", "=deep-arguments");
            lua.call(0, 2);
            assertThrows(IllegalArgumentException.class, () -> NativeLuaValues.arguments(lua, 2));
            assertEquals(2, lua.getTop()); assertEquals("sentinel", lua.toString(1));
            assertTrue(lua.isTable(2));
        }
    }
}
