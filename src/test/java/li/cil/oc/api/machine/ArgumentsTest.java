package li.cil.oc.api.machine;

import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ArgumentsTest {
    @Test
    void exposesCheckedOptionalAndTypeQueryAccessors() {
        ItemStack stack = null;
        byte[] bytes = "text".getBytes(StandardCharsets.UTF_8);
        Map<String, String> table = Map.of("key", "value");
        Arguments arguments = new TestArguments(new Object[]{true, 2.9, 3L, bytes, table, stack});

        assertEquals(6, arguments.count());
        assertEquals(true, arguments.checkAny(0));
        assertTrue(arguments.checkBoolean(0));
        assertEquals(2, arguments.checkInteger(1));
        assertEquals(3L, arguments.checkLong(2));
        assertEquals(2.9, arguments.checkDouble(1));
        assertEquals("text", arguments.checkString(3));
        assertArrayEquals(bytes, arguments.checkByteArray(3));
        assertEquals(table, arguments.checkTable(4));
        assertNull(arguments.checkItemStack(5));
        assertEquals("fallback", arguments.optString(9, "fallback"));
        assertSame(stack, arguments.optItemStack(9, stack));
        assertTrue(arguments.isBoolean(0));
        assertTrue(arguments.isInteger(1));
        assertTrue(arguments.isLong(2));
        assertTrue(arguments.isDouble(1));
        assertTrue(arguments.isString(3));
        assertTrue(arguments.isByteArray(3));
        assertTrue(arguments.isTable(4));
        assertFalse(arguments.isItemStack(5));
        assertFalse(arguments.isBoolean(9));
        assertArrayEquals(new Object[]{true, 2.9, 3L, "text", table, stack}, arguments.toArray());
    }

    private record TestArguments(Object[] values) implements Arguments {
        @Override
        public int count() {
            return values.length;
        }

        @Override
        public Object checkAny(final int index) {
            if (index >= values.length) {
                throw new IllegalArgumentException();
            }
            return values[index];
        }

        @Override
        public boolean checkBoolean(final int index) {
            return (Boolean) checkAny(index);
        }

        @Override
        public int checkInteger(final int index) {
            return ((Number) checkAny(index)).intValue();
        }

        @Override
        public long checkLong(final int index) {
            return ((Number) checkAny(index)).longValue();
        }

        @Override
        public double checkDouble(final int index) {
            return ((Number) checkAny(index)).doubleValue();
        }

        @Override
        public String checkString(final int index) {
            return new String(checkByteArray(index), StandardCharsets.UTF_8);
        }

        @Override
        public byte[] checkByteArray(final int index) {
            return (byte[]) checkAny(index);
        }

        @Override
        public Map checkTable(final int index) {
            return (Map) checkAny(index);
        }

        @Override
        public ItemStack checkItemStack(final int index) {
            return (ItemStack) checkAny(index);
        }

        @Override
        public Object optAny(final int index, final Object def) {
            return index < values.length ? checkAny(index) : def;
        }

        @Override
        public boolean optBoolean(final int index, final boolean def) {
            return index < values.length ? checkBoolean(index) : def;
        }

        @Override
        public int optInteger(final int index, final int def) {
            return index < values.length ? checkInteger(index) : def;
        }

        @Override
        public long optLong(final int index, final long def) {
            return index < values.length ? checkLong(index) : def;
        }

        @Override
        public double optDouble(final int index, final double def) {
            return index < values.length ? checkDouble(index) : def;
        }

        @Override
        public String optString(final int index, final String def) {
            return index < values.length ? checkString(index) : def;
        }

        @Override
        public byte[] optByteArray(final int index, final byte[] def) {
            return index < values.length ? checkByteArray(index) : def;
        }

        @Override
        public Map optTable(final int index, final Map def) {
            return index < values.length ? checkTable(index) : def;
        }

        @Override
        public ItemStack optItemStack(final int index, final ItemStack def) {
            return index < values.length ? checkItemStack(index) : def;
        }

        @Override
        public boolean isBoolean(final int index) {
            return index < values.length && values[index] instanceof Boolean;
        }

        @Override
        public boolean isInteger(final int index) {
            return index < values.length && values[index] instanceof Number;
        }

        @Override
        public boolean isLong(final int index) {
            return index < values.length && values[index] instanceof Number;
        }

        @Override
        public boolean isDouble(final int index) {
            return index < values.length && values[index] instanceof Number;
        }

        @Override
        public boolean isString(final int index) {
            return isByteArray(index);
        }

        @Override
        public boolean isByteArray(final int index) {
            return index < values.length && values[index] instanceof byte[];
        }

        @Override
        public boolean isTable(final int index) {
            return index < values.length && values[index] instanceof Map;
        }

        @Override
        public boolean isItemStack(final int index) {
            return index < values.length && values[index] instanceof ItemStack;
        }

        @Override
        public Object[] toArray() {
            return Arrays.stream(values)
                    .map(value -> value instanceof byte[] bytes ? new String(bytes, StandardCharsets.UTF_8) : value)
                    .toArray();
        }

        @Override
        public Iterator<Object> iterator() {
            return Arrays.asList(values).iterator();
        }
    }
}
