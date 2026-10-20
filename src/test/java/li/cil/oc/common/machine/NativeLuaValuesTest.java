package li.cil.oc.common.machine;

import li.cil.repack.com.naef.jnlua.LuaState;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class NativeLuaValuesTest {
    @ParameterizedTest @EnumSource(NativeLuaState.Version.class)
    void primitiveArraysAndCharactersMatchLegacyValues(NativeLuaState.Version version) throws Exception {
        try (var owner = NativeLuaState.create(version, 2 * 1024 * 1024)) {
            final var lua = owner.state();
            final Map<String, Object> values = new LinkedHashMap<>();
            values.put("booleans", new boolean[]{true, false});
            values.put("chars", new char[]{'x', '水'});
            values.put("shorts", new short[]{-2, 7});
            values.put("ints", new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE});
            values.put("longs", new long[]{731, -9, Long.MAX_VALUE});
            values.put("floats", new float[]{1.25F});
            values.put("doubles", new double[]{2.5D});
            values.put("bytes", new byte[]{0, (byte) 128, (byte) 255});
            values.put("character", '水');
            values.put("boxed", new Character[]{'a'});
            values.put("matrix", new int[][]{{1, 2}, {3, 4}});
            NativeLuaValues.push(lua, values);
            assertEquals(1, lua.getTop()); lua.setGlobal("values");
            lua.load("""
                assert(values.booleans[1]==true and values.booleans[2]==false)
                assert(values.chars[1]=='x' and values.chars[2]=='水' and values.character=='水' and values.boxed[1]=='a')
                assert(values.shorts[1]==-2 and values.shorts[2]==7)
                assert(values.ints[1]==-2147483648 and values.ints[2]==2147483647)
                assert(values.longs[1]==731 and values.longs[2]==-9)
                assert(values.floats[1]==1.25 and values.doubles[1]==2.5)
                assert(type(values.bytes)=='string' and values.bytes==string.char(0,128,255))
                assert(values.matrix[1][2]==2 and values.matrix[2][1]==3)
                """, "=primitive-values");
            lua.call(0, 0); assertEquals(0, lua.getTop());
            if (version != NativeLuaState.Version.LUA52) {
                lua.load("assert(math.type(values.longs[3])=='integer' and string.format('%d',values.longs[3])=='9223372036854775807')", "=integer-values");
                lua.call(0, 0); assertEquals(0, lua.getTop());
            }
        }
    }

    @ParameterizedTest @EnumSource(NativeLuaState.Version.class)
    void sharedArrayAndTableReferencesStayShared(NativeLuaState.Version version) throws Exception {
        try (var owner = NativeLuaState.create(version, 2 * 1024 * 1024)) {
            final var lua = owner.state();
            final var child = Map.of("value", 731);
            final Object[] array = {child, child};
            NativeLuaValues.push(lua, Map.of("left", array, "right", array)); lua.setGlobal("graph");
            lua.load("assert(rawequal(graph.left,graph.right) and rawequal(graph.left[1],graph.left[2]) and graph.right[2].value==731)", "=shared-values");
            lua.call(0, 0); assertEquals(0, lua.getTop());
        }
    }

    @ParameterizedTest @EnumSource(NativeLuaState.Version.class)
    void cyclesSurviveConversion(NativeLuaState.Version version) throws Exception {
        try (var owner = NativeLuaState.create(version, 2 * 1024 * 1024)) {
            final var lua = owner.state();
            final Map<String, Object> graph = new LinkedHashMap<>();
            final Object[] array = new Object[2];
            graph.put("self", graph); graph.put("array", array);
            array[0] = graph; array[1] = array;
            NativeLuaValues.push(lua, graph); lua.setGlobal("graph");
            lua.load("assert(rawequal(graph,graph.self) and rawequal(graph,graph.array[1]) and rawequal(graph.array,graph.array[2]))", "=cyclic-values");
            lua.call(0, 0); assertEquals(0, lua.getTop());
        }
    }

    @ParameterizedTest @EnumSource(NativeLuaState.Version.class)
    void failedPushRestoresItsOriginalStack(NativeLuaState.Version version) throws Exception {
        try (var owner = NativeLuaState.create(version, 2 * 1024 * 1024)) {
            final var lua = owner.state(); lua.pushString("sentinel");
            final Map<String, Object> graph = new LinkedHashMap<>();
            graph.put("valid", 731); graph.put("invalid", new Object());
            assertThrows(IllegalArgumentException.class, () -> NativeLuaValues.push(lua, graph));
            assertEquals(1, lua.getTop()); assertEquals("sentinel", lua.toString(1));
            NativeLuaValues.push(lua, "after");
            assertEquals(2, lua.getTop()); assertEquals("after", lua.toString(2));
        }
    }

    @ParameterizedTest @EnumSource(NativeLuaState.Version.class)
    void temporaryReferencesDoNotRetainCompletedValues(NativeLuaState.Version version) throws Exception {
        try (var owner = NativeLuaState.create(version, 2 * 1024 * 1024)) {
            final var lua = owner.state();
            for (int i = 0; i < 64; i++) {
                final byte[] bytes = new byte[128 * 1024]; bytes[0] = (byte) i;
                NativeLuaValues.push(lua, Map.of("payload", bytes));
                assertEquals(1, lua.getTop()); lua.pop(1);
                lua.gc(LuaState.GcAction.COLLECT, 0);
            }
            assertEquals(0, lua.getTop());
        }
    }
}
