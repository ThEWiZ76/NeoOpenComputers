package li.cil.oc.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class ModContentCatalogTest {
    @Test
    void registersInitialApiItemNames() {
        final ItemRegistry registry = new ItemRegistry();

        ModContentCatalog.register(registry, null, null, null, null, null, null, null, null);

        assertNotNull(registry.get(ModContentIds.MANUAL));
        assertNotNull(registry.get(ModContentIds.COMPUTER_CASE_TIER1));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_COMPUTER_CASE_TIER1));
        assertNotNull(registry.get(ModContentIds.CPU_TIER1));
        assertNotNull(registry.get(ModContentIds.MEMORY_TIER1));
        assertNotNull(registry.get(ModContentIds.HDD_TIER1));
        assertNotNull(registry.get(ModContentIds.EEPROM));
        assertNotNull(registry.get(ModContentIds.GRAPHICS_CARD_TIER1));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_CPU_TIER1));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_MEMORY_TIER1));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_HDD_TIER1));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_GRAPHICS_CARD_TIER1));
        assertEquals(ModContentIds.MANUAL, registry.get(ModContentIds.MANUAL).name());
        assertEquals(ModContentIds.COMPUTER_CASE_TIER1, registry.get(ModContentIds.COMPUTER_CASE_TIER1).name());
        assertEquals(ModContentCatalog.COMPAT_COMPUTER_CASE_TIER1, registry.get(ModContentCatalog.COMPAT_COMPUTER_CASE_TIER1).name());
        assertEquals(ModContentIds.CPU_TIER1, registry.get(ModContentIds.CPU_TIER1).name());
        assertEquals(ModContentIds.MEMORY_TIER1, registry.get(ModContentIds.MEMORY_TIER1).name());
        assertEquals(ModContentIds.HDD_TIER1, registry.get(ModContentIds.HDD_TIER1).name());
        assertEquals(ModContentIds.EEPROM, registry.get(ModContentIds.EEPROM).name());
        assertEquals(ModContentIds.GRAPHICS_CARD_TIER1, registry.get(ModContentIds.GRAPHICS_CARD_TIER1).name());
        assertEquals(ModContentCatalog.COMPAT_CPU_TIER1, registry.get(ModContentCatalog.COMPAT_CPU_TIER1).name());
        assertEquals(ModContentCatalog.COMPAT_MEMORY_TIER1, registry.get(ModContentCatalog.COMPAT_MEMORY_TIER1).name());
        assertEquals(ModContentCatalog.COMPAT_HDD_TIER1, registry.get(ModContentCatalog.COMPAT_HDD_TIER1).name());
        assertEquals(ModContentCatalog.COMPAT_GRAPHICS_CARD_TIER1, registry.get(ModContentCatalog.COMPAT_GRAPHICS_CARD_TIER1).name());
    }
}
