package li.cil.oc.common.machine;

import li.cil.repack.com.naef.jnlua.LuaState;
import li.cil.oc.api.machine.Value;
import li.cil.oc.api.machine.LimitReachedException;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/** Binary-safe values for the native component/signal boundary. */
final class NativeLuaValues {
    private NativeLuaValues() {}

    static void push(final LuaState lua, final Object value) {
        final int top = lua.getTop();
        final var references = new IdentityHashMap<Object, Integer>();
        try {
            push(lua, value, 0, references);
        } catch (RuntimeException | Error failure) {
            lua.setTop(top);
            throw failure;
        } finally {
            for (final int reference : references.values()) lua.unref(lua.getRegistryIndex(), reference);
        }
    }

    private static void push(final LuaState lua, final Object value, final int depth, final IdentityHashMap<Object, Integer> references) {
        final Integer reference = references.get(value);
        if (reference != null) {
            lua.rawGet(lua.getRegistryIndex(), reference);
            return;
        }
        if (depth > 64) throw new IllegalArgumentException("component value nesting too deep");
        if (value == null) lua.pushNil();
        else if (value instanceof Value object) lua.pushJavaObjectRaw(object);
        else if (value instanceof Boolean bool) lua.pushBoolean(bool);
        else if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) lua.pushInteger(((Number) value).longValue());
        else if (value instanceof Number number) lua.pushNumber(number.doubleValue());
        else if (value instanceof String text) lua.pushString(text);
        else if (value instanceof Character character) lua.pushString(character.toString());
        else if (value instanceof byte[] bytes) lua.pushByteArray(bytes);
        else if (value instanceof Map<?, ?> map) {
            lua.newTable();
            final int table = lua.getTop();
            retain(lua, value, table, references);
            for (final var entry : map.entrySet()) {
                if (entry.getKey() == null) continue;
                push(lua, entry.getKey(), depth + 1, references);
                push(lua, entry.getValue(), depth + 1, references);
                lua.rawSet(table);
            }
        } else if (value.getClass().isArray()) {
            lua.newTable();
            final int table = lua.getTop();
            retain(lua, value, table, references);
            for (int i = 0; i < Array.getLength(value); i++) {
                push(lua, Array.get(value, i), depth + 1, references);
                lua.rawSet(table, i + 1);
            }
        } else throw new IllegalArgumentException("Unsupported native component value: " + value.getClass().getName());
    }

    private static void retain(final LuaState lua, final Object value, final int table, final IdentityHashMap<Object, Integer> references) {
        // Registry roots preserve graph identity without retaining extra Lua stack slots.
        lua.pushValue(table);
        references.put(value, lua.ref(lua.getRegistryIndex()));
    }

    static Object[] arguments(final LuaState lua, final int start) {
        final Object[] values = new Object[Math.max(0, lua.getTop() - start + 1)];
        for (int i = 0; i < values.length; i++) values[i] = read(lua, start + i, 0, new HashMap<>());
        return values;
    }

    private static Object read(final LuaState lua, final int index, final int depth, final Map<Long, Map<Object, Object>> references) {
        if (lua.isTable(index)) {
            final Map<Object, Object> cached = references.get(lua.toPointer(index));
            if (cached != null) return cached;
        }
        if (depth > 64) throw new IllegalArgumentException("argument nesting too deep");
        if (lua.isNil(index)) return null;
        if (lua.isJavaObjectRaw(index) && lua.toJavaObjectRaw(index) instanceof Value value) return value;
        if (lua.isBoolean(index)) return lua.toBoolean(index);
        if (lua.isNumber(index) && !lua.isString(index)) return lua.isInteger(index) ? (Object) lua.toInteger(index) : lua.toNumber(index);
        // Lua's isString also accepts numbers; inspect the actual type.
        if (lua.type(index) == li.cil.repack.com.naef.jnlua.LuaType.NUMBER) return lua.isInteger(index) ? (Object) lua.toInteger(index) : lua.toNumber(index);
        if (lua.isString(index)) return lua.toByteArray(index);
        if (lua.isTable(index)) {
            final int table = lua.absIndex(index);
            final int top = lua.getTop();
            final Map<Object, Object> map = new LinkedHashMap<>();
            references.put(lua.toPointer(table), map);
            try {
                lua.pushNil();
                while (lua.next(table)) {
                    Object key = read(lua, -2, depth + 1, references);
                    if (key instanceof byte[] bytes) {
                        final String text = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                        if (java.util.Arrays.equals(bytes, text.getBytes(java.nio.charset.StandardCharsets.UTF_8))) key = text;
                    }
                    map.put(key, read(lua, -1, depth + 1, references));
                    lua.pop(1);
                }
            } finally { lua.setTop(top); }
            return map;
        }
        // Upstream's toSimpleJavaObject maps functions, threads and other unsupported values to null.
        return null;
    }

    /** Upstream invocation protocol: no results means retry synchronously after a budget limit. */
    static int invoke(final LuaState lua, final Callable<Object[]> callback) {
        final int top = lua.getTop();
        try {
            final Object[] results = callback.call();
            lua.pushBoolean(true);
            if (results != null) for (final Object result : results) push(lua, result);
            return 1 + (results == null ? 0 : results.length);
        } catch (LimitReachedException limit) { lua.setTop(top); return 0; }
        catch (Exception failure) {
            lua.setTop(top);
            final boolean soft = !(failure instanceof IllegalArgumentException);
            lua.pushBoolean(soft);
            if (soft) lua.pushNil();
            lua.pushString(failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage());
            return soft ? 3 : 2;
        }
    }
}
