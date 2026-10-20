package li.cil.oc.common.integration.computercraft;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaValues;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** The Java peripheral API uses Unicode text, while getBytes must retain OC's raw Lua bytes. */
final class ComputerCraftArguments implements IArguments {
    private final Object[] raw;
    private final Object[] text;

    ComputerCraftArguments(Object[] values) {
        raw = (Object[]) convert(values, false, new IdentityHashMap<>());
        text = (Object[]) convert(raw, true, new IdentityHashMap<>());
    }

    private ComputerCraftArguments(Object[] raw, Object[] text) { this.raw = raw; this.text = text; }
    @Override public int count() { return raw.length; }
    @Override public Object get(int index) { return index < 0 || index >= text.length ? null : text[index]; }
    @Override public String getType(int index) { return LuaValues.getType(get(index)); }
    @Override public IArguments drop(int count) {
        if (count < 0) throw new IllegalArgumentException("negative argument count");
        int start = Math.min(count, raw.length);
        return new ComputerCraftArguments(Arrays.copyOfRange(raw, start, raw.length), Arrays.copyOfRange(text, start, text.length));
    }
    @Override public ByteBuffer getBytes(int index) throws LuaException {
        Object value = index < 0 || index >= raw.length ? null : raw[index];
        if (value instanceof byte[] bytes) return ByteBuffer.wrap(bytes).asReadOnlyBuffer();
        if (value instanceof String string) return ByteBuffer.wrap(string.getBytes(StandardCharsets.UTF_8)).asReadOnlyBuffer();
        throw LuaValues.badArgumentOf(this, index, "string");
    }

    static Object[] results(Object[] values) {
        return values == null ? new Object[0] : (Object[]) convert(values, false, new IdentityHashMap<>());
    }

    static Object[] taskCompletion(long id, Object[] values) {
        final var results = results(values);
        final Object[] event = new Object[results.length + 3];
        event[0] = "task_complete"; event[1] = id; event[2] = true;
        System.arraycopy(results, 0, event, 3, results.length);
        return event;
    }

    private static Object convert(Object value, boolean decode, IdentityHashMap<Object, Object> seen) {
        if (value == null) return null;
        if (seen.containsKey(value)) return seen.get(value);
        if (value instanceof ByteBuffer buffer) {
            final var copy = buffer.duplicate(); final byte[] bytes = new byte[copy.remaining()]; copy.get(bytes);
            Object result = decode ? new String(bytes, StandardCharsets.UTF_8) : bytes;
            seen.put(value, result); return result;
        }
        if (value instanceof byte[] bytes) {
            Object result = decode ? new String(bytes, StandardCharsets.UTF_8) : bytes.clone();
            seen.put(value, result); return result;
        }
        if (value instanceof Map<?, ?> map) {
            final var copy = new LinkedHashMap<Object, Object>(); seen.put(value, copy);
            map.forEach((key, item) -> copy.put(convert(key, decode, seen), convert(item, decode, seen)));
            return copy;
        }
        if (value instanceof Object[] array) {
            final var copy = new Object[array.length]; seen.put(value, copy);
            for (int i = 0; i < array.length; i++) copy[i] = convert(array[i], decode, seen);
            return copy;
        }
        if (value instanceof Collection<?> collection) {
            final var copy = new ArrayList<Object>(collection.size()); seen.put(value, copy);
            for (var item : collection) copy.add(convert(item, decode, seen));
            return copy;
        }
        return value;
    }
}
