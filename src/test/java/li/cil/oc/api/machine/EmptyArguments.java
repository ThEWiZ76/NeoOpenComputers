package li.cil.oc.api.machine;

import net.minecraft.world.item.ItemStack;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class EmptyArguments implements Arguments {
    @Override
    public int count() {
        return 0;
    }

    @Override
    public Object checkAny(final int index) {
        throw new IllegalArgumentException();
    }

    @Override
    public boolean checkBoolean(final int index) {
        throw new IllegalArgumentException();
    }

    @Override
    public int checkInteger(final int index) {
        throw new IllegalArgumentException();
    }

    @Override
    public long checkLong(final int index) {
        throw new IllegalArgumentException();
    }

    @Override
    public double checkDouble(final int index) {
        throw new IllegalArgumentException();
    }

    @Override
    public String checkString(final int index) {
        throw new IllegalArgumentException();
    }

    @Override
    public byte[] checkByteArray(final int index) {
        throw new IllegalArgumentException();
    }

    @Override
    public Map checkTable(final int index) {
        throw new IllegalArgumentException();
    }

    @Override
    public ItemStack checkItemStack(final int index) {
        throw new IllegalArgumentException();
    }

    @Override
    public Object optAny(final int index, final Object def) {
        return def;
    }

    @Override
    public boolean optBoolean(final int index, final boolean def) {
        return def;
    }

    @Override
    public int optInteger(final int index, final int def) {
        return def;
    }

    @Override
    public long optLong(final int index, final long def) {
        return def;
    }

    @Override
    public double optDouble(final int index, final double def) {
        return def;
    }

    @Override
    public String optString(final int index, final String def) {
        return def;
    }

    @Override
    public byte[] optByteArray(final int index, final byte[] def) {
        return def;
    }

    @Override
    public Map optTable(final int index, final Map def) {
        return def;
    }

    @Override
    public ItemStack optItemStack(final int index, final ItemStack def) {
        return def;
    }

    @Override
    public boolean isBoolean(final int index) {
        return false;
    }

    @Override
    public boolean isInteger(final int index) {
        return false;
    }

    @Override
    public boolean isLong(final int index) {
        return false;
    }

    @Override
    public boolean isDouble(final int index) {
        return false;
    }

    @Override
    public boolean isString(final int index) {
        return false;
    }

    @Override
    public boolean isByteArray(final int index) {
        return false;
    }

    @Override
    public boolean isTable(final int index) {
        return false;
    }

    @Override
    public boolean isItemStack(final int index) {
        return false;
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public Iterator<Object> iterator() {
        return List.of().iterator();
    }
}
