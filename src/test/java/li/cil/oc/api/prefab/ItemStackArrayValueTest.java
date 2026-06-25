package li.cil.oc.api.prefab;

import li.cil.oc.api.machine.Arguments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class ItemStackArrayValueTest {
    @Test
    void iteratesNullSlotsAsEmptyTables() {
        ItemStackArrayValue value = new ItemStackArrayValue(new ItemStack[]{null});

        assertArrayEquals(new Object[]{Collections.emptyMap()}, value.call(null, new TestArguments()));
        assertNull(value.call(null, new TestArguments()));
    }

    @Test
    void supportsLuaStyleIndexAndCountAccess() {
        ItemStackArrayValue value = new ItemStackArrayValue(new ItemStack[]{null});

        assertNull(value.apply(null, new TestArguments(1)));
        assertEquals(1, value.apply(null, new TestArguments("n")));
    }

    @Test
    void loadWithoutArrayClearsValueAndKeepsZeroCount() throws Exception {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Index", 1);

        ItemStackArrayValue loaded = new ItemStackArrayValue();
        loaded.load(tag);

        assertArrayEquals(new Object[]{0}, loaded.count(null, new TestArguments()));
        assertNull(loaded.call(null, new TestArguments()));
    }

    @Test
    void returnsUsefulStringName() {
        assertEquals("{ItemStack Array}", new ItemStackArrayValue().toString());
    }

    private static final class TestArguments implements Arguments {
        private final Object[] values;

        private TestArguments(final Object... values) {
            this.values = values;
        }

        @Override
        public int count() {
            return values.length;
        }

        @Override
        public Object checkAny(final int index) {
            return values[index];
        }

        @Override
        public int checkInteger(final int index) {
            return (Integer) values[index];
        }

        @Override
        public String checkString(final int index) {
            return (String) values[index];
        }

        @Override
        public boolean isInteger(final int index) {
            return values[index] instanceof Integer;
        }

        @Override
        public boolean isString(final int index) {
            return values[index] instanceof String;
        }

        @Override
        public Object[] toArray() {
            return values.clone();
        }

        @Override public boolean checkBoolean(final int index) { throw new UnsupportedOperationException(); }
        @Override public long checkLong(final int index) { throw new UnsupportedOperationException(); }
        @Override public double checkDouble(final int index) { throw new UnsupportedOperationException(); }
        @Override public byte[] checkByteArray(final int index) { throw new UnsupportedOperationException(); }
        @Override public Map checkTable(final int index) { throw new UnsupportedOperationException(); }
        @Override public ItemStack checkItemStack(final int index) { throw new UnsupportedOperationException(); }
        @Override public Object optAny(final int index, final Object def) { throw new UnsupportedOperationException(); }
        @Override public boolean optBoolean(final int index, final boolean def) { throw new UnsupportedOperationException(); }
        @Override public int optInteger(final int index, final int def) { throw new UnsupportedOperationException(); }
        @Override public long optLong(final int index, final long def) { throw new UnsupportedOperationException(); }
        @Override public double optDouble(final int index, final double def) { throw new UnsupportedOperationException(); }
        @Override public String optString(final int index, final String def) { throw new UnsupportedOperationException(); }
        @Override public byte[] optByteArray(final int index, final byte[] def) { throw new UnsupportedOperationException(); }
        @Override public Map optTable(final int index, final Map def) { throw new UnsupportedOperationException(); }
        @Override public ItemStack optItemStack(final int index, final ItemStack def) { throw new UnsupportedOperationException(); }
        @Override public boolean isBoolean(final int index) { throw new UnsupportedOperationException(); }
        @Override public boolean isLong(final int index) { throw new UnsupportedOperationException(); }
        @Override public boolean isDouble(final int index) { throw new UnsupportedOperationException(); }
        @Override public boolean isByteArray(final int index) { throw new UnsupportedOperationException(); }
        @Override public boolean isTable(final int index) { throw new UnsupportedOperationException(); }
        @Override public boolean isItemStack(final int index) { throw new UnsupportedOperationException(); }
        @Override public Iterator<Object> iterator() { return java.util.Arrays.asList(values).iterator(); }
    }
}
