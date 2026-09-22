package li.cil.oc.common.machine;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.common.ArgumentUtils;
import net.minecraft.world.item.ItemStack;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

/** Shared OC argument validation for Lua value callbacks on both backends. */
record LuaArguments(Object[] values) implements Arguments {
    @Override
    public int count() {
        return values.length;
    }

    @Override
    public Object checkAny(final int index) {
        if (index < 0 || index >= values.length) {
            throw new IllegalArgumentException("missing argument #" + (index + 1));
        }
        return values[index];
    }

    @Override
    public boolean checkBoolean(final int index) {
        final Object value = checkAny(index, "boolean");
        if (!(value instanceof Boolean)) {
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (boolean expected, got " + argumentTypeName(value) + ")");
        }
        return (Boolean) value;
    }

    @Override
    public int checkInteger(final int index) {
        final Object value = checkAny(index, "integer");
        if (value instanceof Double doubleValue) {
            if (Double.isNaN(doubleValue)) {
                throw new IllegalArgumentException("bad argument #" + (index + 1) + " (number has no integer representation)");
            }
            if (doubleValue > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            if (doubleValue < Integer.MIN_VALUE) {
                return Integer.MIN_VALUE;
            }
            return doubleValue.intValue();
        }
        if (value instanceof Float floatValue) {
            if (Float.isNaN(floatValue)) {
                throw new IllegalArgumentException("bad argument #" + (index + 1) + " (number has no integer representation)");
            }
            if (floatValue > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            if (floatValue < Integer.MIN_VALUE) {
                return Integer.MIN_VALUE;
            }
            return floatValue.intValue();
        }
        if (value instanceof Long longValue) {
            if (longValue > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            if (longValue < Integer.MIN_VALUE) {
                return Integer.MIN_VALUE;
            }
            return longValue.intValue();
        }
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (integer expected, got " + argumentTypeName(value) + ")");
        }
        return ((Number) value).intValue();
    }

    @Override
    public long checkLong(final int index) {
        final Object value = checkAny(index, "integer");
        if (value instanceof Double doubleValue) {
            if (Double.isNaN(doubleValue)) {
                throw new IllegalArgumentException("bad argument #" + (index + 1) + " (number has no integer representation)");
            }
            if (doubleValue > Long.MAX_VALUE) {
                return Long.MAX_VALUE;
            }
            if (doubleValue < Long.MIN_VALUE) {
                return Long.MIN_VALUE;
            }
            return doubleValue.longValue();
        }
        if (value instanceof Float floatValue) {
            if (Float.isNaN(floatValue)) {
                throw new IllegalArgumentException("bad argument #" + (index + 1) + " (number has no integer representation)");
            }
            if (floatValue > Long.MAX_VALUE) {
                return Long.MAX_VALUE;
            }
            if (floatValue < Long.MIN_VALUE) {
                return Long.MIN_VALUE;
            }
            return floatValue.longValue();
        }
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (integer expected, got " + argumentTypeName(value) + ")");
        }
        return ((Number) value).longValue();
    }

    @Override
    public double checkDouble(final int index) {
        final Object value = checkAny(index, "number");
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (number expected, got " + argumentTypeName(value) + ")");
        }
        return ((Number) value).doubleValue();
    }

    @Override
    public String checkString(final int index) {
        final Object value = checkAny(index, "string");
        if (value instanceof String string) {
            return string;
        }
        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        throw new IllegalArgumentException("bad argument #" + (index + 1) + " (string expected, got " + argumentTypeName(value) + ")");
    }

    @Override
    public byte[] checkByteArray(final int index) {
        final Object value = checkAny(index, "string");
        if (value instanceof byte[] bytes) {
            return bytes;
        }
        if (value instanceof String string) {
            return string.getBytes(StandardCharsets.UTF_8);
        }
        throw new IllegalArgumentException("bad argument #" + (index + 1) + " (string expected, got " + argumentTypeName(value) + ")");
    }

    @Override
    public Map checkTable(final int index) {
        final Object value = checkAny(index, "table");
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (table expected, got " + argumentTypeName(value) + ")");
        }
        return (Map) value;
    }

    @Override
    public ItemStack checkItemStack(final int index) {
        return ArgumentUtils.checkItemStack(index, checkAny(index));
    }

    @Override
    public Object optAny(final int index, final Object def) {
        return isDefined(index) ? checkAny(index) : def;
    }

    @Override
    public boolean optBoolean(final int index, final boolean def) {
        return isDefined(index) ? checkBoolean(index) : def;
    }

    @Override
    public int optInteger(final int index, final int def) {
        return isDefined(index) ? checkInteger(index) : def;
    }

    @Override
    public long optLong(final int index, final long def) {
        return isDefined(index) ? checkLong(index) : def;
    }

    @Override
    public double optDouble(final int index, final double def) {
        return isDefined(index) ? checkDouble(index) : def;
    }

    @Override
    public String optString(final int index, final String def) {
        return isDefined(index) ? checkString(index) : def;
    }

    @Override
    public byte[] optByteArray(final int index, final byte[] def) {
        return isDefined(index) ? checkByteArray(index) : def;
    }

    @Override
    public Map optTable(final int index, final Map def) {
        return isDefined(index) ? checkTable(index) : def;
    }

    @Override
    public ItemStack optItemStack(final int index, final ItemStack def) {
        return isDefined(index) ? checkItemStack(index) : def;
    }

    @Override
    public boolean isBoolean(final int index) {
        return index >= 0 && index < values.length && values[index] instanceof Boolean;
    }

    @Override
    public boolean isInteger(final int index) {
        return index >= 0 && index < values.length && values[index] instanceof Number number && !Double.isNaN(number.doubleValue());
    }

    @Override
    public boolean isLong(final int index) {
        return index >= 0 && index < values.length && values[index] instanceof Number number && !Double.isNaN(number.doubleValue());
    }

    @Override
    public boolean isDouble(final int index) {
        return index >= 0 && index < values.length && values[index] instanceof Number;
    }

    @Override
    public boolean isString(final int index) {
        return index >= 0 && index < values.length && (values[index] instanceof String || values[index] instanceof byte[]);
    }

    @Override
    public boolean isByteArray(final int index) {
        return index >= 0 && index < values.length && (values[index] instanceof String || values[index] instanceof byte[]);
    }

    @Override
    public boolean isTable(final int index) {
        return index >= 0 && index < values.length && values[index] instanceof Map;
    }

    @Override
    public boolean isItemStack(final int index) {
        return index >= 0 && index < values.length && ArgumentUtils.isItemStack(values[index]);
    }

    @Override
    public Object[] toArray() {
        final Object[] result = Arrays.copyOf(values, values.length);
        for (int index = 0; index < result.length; index++) {
            if (result[index] instanceof byte[] bytes) {
                result[index] = new String(bytes, StandardCharsets.UTF_8);
            }
        }
        return result;
    }

    @Override
    public java.util.Iterator<Object> iterator() {
        return Arrays.asList(values).iterator();
    }

    private boolean isDefined(final int index) {
        return index >= 0 && index < values.length && values[index] != null;
    }

    private static String argumentTypeName(final Object value) {
        if (value == null) {
            return "nil";
        }
        if (value instanceof Boolean) {
            return "boolean";
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return "integer";
        }
        if (value instanceof Number) {
            return "number";
        }
        if (value instanceof String || value instanceof byte[]) {
            return "string";
        }
        if (value instanceof Map) {
            return "table";
        }
        return "userdata";
    }

    private Object checkAny(final int index, final String type) {
        if (index < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (index >= values.length) {
            throw new IllegalArgumentException("bad arguments #" + (index + 1) + " (" + type + " expected, got no value)");
        }
        return values[index];
    }
}
