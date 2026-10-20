package li.cil.oc.common.integration;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/** Subset matching for OC descriptions and Lua tables, retaining long quantities. */
final class DescriptionFilter {
    private DescriptionFilter() { }

    static boolean matches(final Object value, final Object filter) {
        if (filter instanceof Map<?, ?> table) {
            for (final var entry : table.entrySet()) {
                final Object actual;
                if (value instanceof Map<?, ?> description) {
                    final Object key = string(entry.getKey());
                    if (!description.containsKey(key)) return false;
                    actual = description.get(key);
                } else if (value != null && value.getClass().isArray() && entry.getKey() instanceof Number index) {
                    final int slot = index.intValue() - 1;
                    if (slot < 0 || slot >= Array.getLength(value) || index.doubleValue() != slot + 1.0) return false;
                    actual = Array.get(value, slot);
                } else return false;
                if (!matches(actual, entry.getValue())) return false;
            }
            return true;
        }
        if (value instanceof Number actual && filter instanceof Number expected) {
            try { return new BigDecimal(actual.toString()).compareTo(new BigDecimal(expected.toString())) == 0; }
            catch (NumberFormatException exception) { return false; }
        }
        return Objects.equals(string(value), string(filter));
    }

    private static Object string(final Object value) {
        return value instanceof byte[] bytes ? new String(bytes, StandardCharsets.UTF_8) : value;
    }
}
