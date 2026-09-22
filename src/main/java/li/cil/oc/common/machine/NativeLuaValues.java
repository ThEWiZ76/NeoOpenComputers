package li.cil.oc.common.machine;

import li.cil.repack.com.naef.jnlua.LuaState;
import java.util.LinkedHashMap;
import java.util.Map;

/** Binary-safe values for the native component/signal boundary. */
final class NativeLuaValues {
    private NativeLuaValues() {}

    static void push(final LuaState lua, final Object value) { push(lua, value, 0); }

    private static void push(final LuaState lua, final Object value, final int depth) {
        if (depth > 64) throw new IllegalArgumentException("component value nesting too deep");
        if (value == null) lua.pushNil();
        else if (value instanceof Boolean bool) lua.pushBoolean(bool);
        else if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) lua.pushInteger(((Number) value).longValue());
        else if (value instanceof Number number) lua.pushNumber(number.doubleValue());
        else if (value instanceof String text) lua.pushString(text);
        else if (value instanceof byte[] bytes) lua.pushByteArray(bytes);
        else if (value instanceof Map<?, ?> map) {
            lua.newTable();
            for (final var entry : map.entrySet()) {
                push(lua, entry.getKey(), depth + 1);
                push(lua, entry.getValue(), depth + 1);
                lua.rawSet(-3);
            }
        } else if (value instanceof Object[] array) {
            lua.newTable();
            for (int i = 0; i < array.length; i++) {
                push(lua, array[i], depth + 1);
                lua.rawSet(-2, i + 1);
            }
        } else throw new IllegalArgumentException("Unsupported native component value: " + value.getClass().getName());
    }

    static Object[] arguments(final LuaState lua, final int start) {
        final Object[] values = new Object[Math.max(0, lua.getTop() - start + 1)];
        for (int i = 0; i < values.length; i++) values[i] = read(lua, start + i, 0);
        return values;
    }

    private static Object read(final LuaState lua, final int index, final int depth) {
        if (depth > 64) throw new IllegalArgumentException("argument nesting too deep");
        if (lua.isNil(index)) return null;
        if (lua.isBoolean(index)) return lua.toBoolean(index);
        if (lua.isNumber(index) && !lua.isString(index)) return lua.isInteger(index) ? (Object) lua.toInteger(index) : lua.toNumber(index);
        // Lua's isString also accepts numbers; inspect the actual type.
        if (lua.type(index) == li.cil.repack.com.naef.jnlua.LuaType.NUMBER) return lua.isInteger(index) ? (Object) lua.toInteger(index) : lua.toNumber(index);
        if (lua.isString(index)) return lua.toByteArray(index);
        if (lua.isTable(index)) {
            final int table = lua.absIndex(index);
            final int top = lua.getTop();
            final Map<Object, Object> map = new LinkedHashMap<>();
            try {
                lua.pushNil();
                while (lua.next(table)) {
                    Object key = read(lua, -2, depth + 1);
                    if (key instanceof byte[] bytes) key = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                    map.put(key, read(lua, -1, depth + 1));
                    lua.pop(1);
                }
            } finally { lua.setTop(top); }
            return map;
        }
        throw new IllegalArgumentException("Unsupported native Lua argument: " + lua.typeName(index));
    }
}
