package li.cil.oc.common.integration;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

final class DescriptionFilterTest {
    @Test
    void matchesLuaStringsNestedTablesAndArrayEntries() {
        final var value = Map.of("name", "minecraft:stone", "enchantments", new Object[]{Map.of("level", 3, "label", "test")});
        assertTrue(DescriptionFilter.matches(value, Map.of("name", "minecraft:stone".getBytes(StandardCharsets.UTF_8),
            "enchantments", Map.of(1.0, Map.of("level", 3.0)))));
        assertFalse(DescriptionFilter.matches(value, Map.of("enchantments", Map.of(2, Map.of("level", 3)))));
        assertFalse(DescriptionFilter.matches(value, Map.of("missing", false)));
    }

    @Test
    void quantitiesDoNotWrapOrTruncate() {
        assertTrue(DescriptionFilter.matches(4_294_967_297L, 4_294_967_297.0));
        assertFalse(DescriptionFilter.matches(4_294_967_297L, 1));
        assertFalse(DescriptionFilter.matches(1, 1.5));
        assertFalse(DescriptionFilter.matches(9_007_199_254_740_993L, 9_007_199_254_740_992.0));
    }
}
