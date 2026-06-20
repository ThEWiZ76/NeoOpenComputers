package li.cil.oc.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class ModContentCatalogTest {
    @Test
    void registersInitialApiItemNames() {
        final ItemRegistry registry = new ItemRegistry();

        ModContentCatalog.register(registry,
            null, null,
            null, null,
            null, null,
            null, null,
            null, null,
            null, null,
            null, null,
            null, null,
            null, null,
            null, null,
            null, null,
            null, null,
            null, null,
            null, null,
            null, null,
            null, null,
            null,
            null, null, null,
            null, null, null,
            null, null, null,
            null, null, null,
            null, null,
            null,
            null,
            null,
            null,
            null,
            null,
            null, null, null,
            null, null,
            null, null,
            null,
            null);

        assertNotNull(registry.get(ModContentIds.ADAPTER));
        assertNotNull(registry.get(ModContentIds.MANUAL));
        assertNotNull(registry.get(ModContentIds.CABLE));
        assertNotNull(registry.get(ModContentIds.COMPUTER_CASE_TIER1));
        assertNotNull(registry.get(ModContentIds.COMPUTER_CASE_TIER2));
        assertNotNull(registry.get(ModContentIds.COMPUTER_CASE_TIER3));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_COMPUTER_CASE_TIER1));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_COMPUTER_CASE_TIER2));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_COMPUTER_CASE_TIER3));
        assertNotNull(registry.get(ModContentIds.DISK_DRIVE));
        assertNotNull(registry.get(ModContentIds.SCREEN_TIER1));
        assertNotNull(registry.get(ModContentIds.SCREEN_TIER2));
        assertNotNull(registry.get(ModContentIds.SCREEN_TIER3));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_SCREEN_TIER1));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_SCREEN_TIER2));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_SCREEN_TIER3));
        assertNotNull(registry.get(ModContentIds.GEOLYZER));
        assertNotNull(registry.get(ModContentIds.KEYBOARD));
        assertNotNull(registry.get(ModContentIds.MOTION_SENSOR));
        assertNotNull(registry.get(ModContentIds.REDSTONE_IO));
        assertNotNull(registry.get(ModContentIds.TRANSPOSER));
        assertNotNull(registry.get(ModContentIds.HOLOGRAM_TIER1));
        assertNotNull(registry.get(ModContentIds.HOLOGRAM_TIER2));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_HOLOGRAM_TIER1));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_HOLOGRAM_TIER2));
        assertNotNull(registry.get(ModContentIds.WAYPOINT));
        assertNotNull(registry.get(ModContentIds.CPU_TIER1));
        assertNotNull(registry.get(ModContentIds.CPU_TIER2));
        assertNotNull(registry.get(ModContentIds.CPU_TIER3));
        assertNotNull(registry.get(ModContentIds.DATA_CARD_TIER1));
        assertNotNull(registry.get(ModContentIds.DATA_CARD_TIER2));
        assertNotNull(registry.get(ModContentIds.DATA_CARD_TIER3));
        assertNotNull(registry.get(ModContentIds.MEMORY_TIER1));
        assertNotNull(registry.get(ModContentIds.MEMORY_TIER2));
        assertNotNull(registry.get(ModContentIds.MEMORY_TIER3));
        assertNotNull(registry.get(ModContentIds.HDD_TIER1));
        assertNotNull(registry.get(ModContentIds.HDD_TIER2));
        assertNotNull(registry.get(ModContentIds.HDD_TIER3));
        assertNotNull(registry.get(ModContentIds.DATABASE_UPGRADE_TIER1));
        assertNotNull(registry.get(ModContentIds.DATABASE_UPGRADE_TIER2));
        assertNotNull(registry.get(ModContentIds.DATABASE_UPGRADE_TIER3));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_DATABASE_UPGRADE_TIER1));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_DATABASE_UPGRADE_TIER2));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_DATABASE_UPGRADE_TIER3));
        assertNotNull(registry.get(ModContentIds.INVENTORY_CONTROLLER_UPGRADE));
        assertNotNull(registry.get(ModContentIds.INTERNET_CARD));
        assertNotNull(registry.get(ModContentIds.LINKED_CARD));
        assertNotNull(registry.get(ModContentIds.EEPROM));
        assertNotNull(registry.get(ModContentIds.FLOPPY));
        assertNotNull(registry.get(ModContentIds.GRAPHICS_CARD_TIER1));
        assertNotNull(registry.get(ModContentIds.GRAPHICS_CARD_TIER2));
        assertNotNull(registry.get(ModContentIds.GRAPHICS_CARD_TIER3));
        assertNotNull(registry.get(ModContentIds.NETWORK_CARD));
        assertNotNull(registry.get(ModContentIds.WIRELESS_NETWORK_CARD_TIER1));
        assertNotNull(registry.get(ModContentIds.WIRELESS_NETWORK_CARD_TIER2));
        assertNotNull(registry.get(ModContentIds.REDSTONE_CARD));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_CPU_TIER1));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_CPU_TIER2));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_CPU_TIER3));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_DATA_CARD_TIER1));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_DATA_CARD_TIER2));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_DATA_CARD_TIER3));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_DATA_CARD));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_MEMORY_TIER1));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_MEMORY_TIER2));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_MEMORY_TIER3));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_HDD_TIER1));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_HDD_TIER2));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_HDD_TIER3));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_INTERNET_CARD));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_LINKED_CARD));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_GRAPHICS_CARD_TIER1));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_GRAPHICS_CARD_TIER2));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_GRAPHICS_CARD_TIER3));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_NETWORK_CARD));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_WIRELESS_NETWORK_CARD_TIER1));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_WIRELESS_NETWORK_CARD_TIER2));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_WIRELESS_NETWORK_CARD));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_REDSTONE_CARD));
        assertEquals(ModContentIds.ADAPTER, registry.get(ModContentIds.ADAPTER).name());
        assertEquals(ModContentIds.MANUAL, registry.get(ModContentIds.MANUAL).name());
        assertEquals(ModContentIds.CABLE, registry.get(ModContentIds.CABLE).name());
        assertEquals(ModContentIds.COMPUTER_CASE_TIER1, registry.get(ModContentIds.COMPUTER_CASE_TIER1).name());
        assertEquals(ModContentIds.COMPUTER_CASE_TIER2, registry.get(ModContentIds.COMPUTER_CASE_TIER2).name());
        assertEquals(ModContentIds.COMPUTER_CASE_TIER3, registry.get(ModContentIds.COMPUTER_CASE_TIER3).name());
        assertEquals(ModContentCatalog.COMPAT_COMPUTER_CASE_TIER1, registry.get(ModContentCatalog.COMPAT_COMPUTER_CASE_TIER1).name());
        assertEquals(ModContentCatalog.COMPAT_COMPUTER_CASE_TIER2, registry.get(ModContentCatalog.COMPAT_COMPUTER_CASE_TIER2).name());
        assertEquals(ModContentCatalog.COMPAT_COMPUTER_CASE_TIER3, registry.get(ModContentCatalog.COMPAT_COMPUTER_CASE_TIER3).name());
        assertEquals(ModContentIds.DISK_DRIVE, registry.get(ModContentIds.DISK_DRIVE).name());
        assertEquals(ModContentIds.SCREEN_TIER1, registry.get(ModContentIds.SCREEN_TIER1).name());
        assertEquals(ModContentIds.SCREEN_TIER2, registry.get(ModContentIds.SCREEN_TIER2).name());
        assertEquals(ModContentIds.SCREEN_TIER3, registry.get(ModContentIds.SCREEN_TIER3).name());
        assertEquals(ModContentCatalog.COMPAT_SCREEN_TIER1, registry.get(ModContentCatalog.COMPAT_SCREEN_TIER1).name());
        assertEquals(ModContentCatalog.COMPAT_SCREEN_TIER2, registry.get(ModContentCatalog.COMPAT_SCREEN_TIER2).name());
        assertEquals(ModContentCatalog.COMPAT_SCREEN_TIER3, registry.get(ModContentCatalog.COMPAT_SCREEN_TIER3).name());
        assertEquals(ModContentIds.GEOLYZER, registry.get(ModContentIds.GEOLYZER).name());
        assertEquals(ModContentIds.KEYBOARD, registry.get(ModContentIds.KEYBOARD).name());
        assertEquals(ModContentIds.MOTION_SENSOR, registry.get(ModContentIds.MOTION_SENSOR).name());
        assertEquals(ModContentIds.REDSTONE_IO, registry.get(ModContentIds.REDSTONE_IO).name());
        assertEquals(ModContentIds.TRANSPOSER, registry.get(ModContentIds.TRANSPOSER).name());
        assertEquals(ModContentIds.HOLOGRAM_TIER1, registry.get(ModContentIds.HOLOGRAM_TIER1).name());
        assertEquals(ModContentIds.HOLOGRAM_TIER2, registry.get(ModContentIds.HOLOGRAM_TIER2).name());
        assertEquals(ModContentCatalog.COMPAT_HOLOGRAM_TIER1, registry.get(ModContentCatalog.COMPAT_HOLOGRAM_TIER1).name());
        assertEquals(ModContentCatalog.COMPAT_HOLOGRAM_TIER2, registry.get(ModContentCatalog.COMPAT_HOLOGRAM_TIER2).name());
        assertEquals(ModContentIds.WAYPOINT, registry.get(ModContentIds.WAYPOINT).name());
        assertEquals(ModContentIds.CPU_TIER1, registry.get(ModContentIds.CPU_TIER1).name());
        assertEquals(ModContentIds.CPU_TIER2, registry.get(ModContentIds.CPU_TIER2).name());
        assertEquals(ModContentIds.CPU_TIER3, registry.get(ModContentIds.CPU_TIER3).name());
        assertEquals(ModContentIds.DATA_CARD_TIER1, registry.get(ModContentIds.DATA_CARD_TIER1).name());
        assertEquals(ModContentIds.DATA_CARD_TIER2, registry.get(ModContentIds.DATA_CARD_TIER2).name());
        assertEquals(ModContentIds.DATA_CARD_TIER3, registry.get(ModContentIds.DATA_CARD_TIER3).name());
        assertEquals(ModContentIds.MEMORY_TIER1, registry.get(ModContentIds.MEMORY_TIER1).name());
        assertEquals(ModContentIds.MEMORY_TIER2, registry.get(ModContentIds.MEMORY_TIER2).name());
        assertEquals(ModContentIds.MEMORY_TIER3, registry.get(ModContentIds.MEMORY_TIER3).name());
        assertEquals(ModContentIds.HDD_TIER1, registry.get(ModContentIds.HDD_TIER1).name());
        assertEquals(ModContentIds.HDD_TIER2, registry.get(ModContentIds.HDD_TIER2).name());
        assertEquals(ModContentIds.HDD_TIER3, registry.get(ModContentIds.HDD_TIER3).name());
        assertEquals(ModContentIds.DATABASE_UPGRADE_TIER1, registry.get(ModContentIds.DATABASE_UPGRADE_TIER1).name());
        assertEquals(ModContentIds.DATABASE_UPGRADE_TIER2, registry.get(ModContentIds.DATABASE_UPGRADE_TIER2).name());
        assertEquals(ModContentIds.DATABASE_UPGRADE_TIER3, registry.get(ModContentIds.DATABASE_UPGRADE_TIER3).name());
        assertEquals(ModContentCatalog.COMPAT_DATABASE_UPGRADE_TIER1, registry.get(ModContentCatalog.COMPAT_DATABASE_UPGRADE_TIER1).name());
        assertEquals(ModContentCatalog.COMPAT_DATABASE_UPGRADE_TIER2, registry.get(ModContentCatalog.COMPAT_DATABASE_UPGRADE_TIER2).name());
        assertEquals(ModContentCatalog.COMPAT_DATABASE_UPGRADE_TIER3, registry.get(ModContentCatalog.COMPAT_DATABASE_UPGRADE_TIER3).name());
        assertEquals(ModContentIds.INVENTORY_CONTROLLER_UPGRADE, registry.get(ModContentIds.INVENTORY_CONTROLLER_UPGRADE).name());
        assertEquals(ModContentIds.INTERNET_CARD, registry.get(ModContentIds.INTERNET_CARD).name());
        assertEquals(ModContentIds.LINKED_CARD, registry.get(ModContentIds.LINKED_CARD).name());
        assertEquals(ModContentIds.EEPROM, registry.get(ModContentIds.EEPROM).name());
        assertEquals(ModContentIds.FLOPPY, registry.get(ModContentIds.FLOPPY).name());
        assertEquals(ModContentIds.GRAPHICS_CARD_TIER1, registry.get(ModContentIds.GRAPHICS_CARD_TIER1).name());
        assertEquals(ModContentIds.GRAPHICS_CARD_TIER2, registry.get(ModContentIds.GRAPHICS_CARD_TIER2).name());
        assertEquals(ModContentIds.GRAPHICS_CARD_TIER3, registry.get(ModContentIds.GRAPHICS_CARD_TIER3).name());
        assertEquals(ModContentIds.NETWORK_CARD, registry.get(ModContentIds.NETWORK_CARD).name());
        assertEquals(ModContentIds.WIRELESS_NETWORK_CARD_TIER1, registry.get(ModContentIds.WIRELESS_NETWORK_CARD_TIER1).name());
        assertEquals(ModContentIds.WIRELESS_NETWORK_CARD_TIER2, registry.get(ModContentIds.WIRELESS_NETWORK_CARD_TIER2).name());
        assertEquals(ModContentIds.REDSTONE_CARD, registry.get(ModContentIds.REDSTONE_CARD).name());
        assertEquals(ModContentCatalog.COMPAT_CPU_TIER1, registry.get(ModContentCatalog.COMPAT_CPU_TIER1).name());
        assertEquals(ModContentCatalog.COMPAT_CPU_TIER2, registry.get(ModContentCatalog.COMPAT_CPU_TIER2).name());
        assertEquals(ModContentCatalog.COMPAT_CPU_TIER3, registry.get(ModContentCatalog.COMPAT_CPU_TIER3).name());
        assertEquals(ModContentCatalog.COMPAT_DATA_CARD_TIER1, registry.get(ModContentCatalog.COMPAT_DATA_CARD_TIER1).name());
        assertEquals(ModContentCatalog.COMPAT_DATA_CARD_TIER2, registry.get(ModContentCatalog.COMPAT_DATA_CARD_TIER2).name());
        assertEquals(ModContentCatalog.COMPAT_DATA_CARD_TIER3, registry.get(ModContentCatalog.COMPAT_DATA_CARD_TIER3).name());
        assertEquals(ModContentCatalog.COMPAT_DATA_CARD, registry.get(ModContentCatalog.COMPAT_DATA_CARD).name());
        assertEquals(ModContentCatalog.COMPAT_MEMORY_TIER1, registry.get(ModContentCatalog.COMPAT_MEMORY_TIER1).name());
        assertEquals(ModContentCatalog.COMPAT_MEMORY_TIER2, registry.get(ModContentCatalog.COMPAT_MEMORY_TIER2).name());
        assertEquals(ModContentCatalog.COMPAT_MEMORY_TIER3, registry.get(ModContentCatalog.COMPAT_MEMORY_TIER3).name());
        assertEquals(ModContentCatalog.COMPAT_HDD_TIER1, registry.get(ModContentCatalog.COMPAT_HDD_TIER1).name());
        assertEquals(ModContentCatalog.COMPAT_HDD_TIER2, registry.get(ModContentCatalog.COMPAT_HDD_TIER2).name());
        assertEquals(ModContentCatalog.COMPAT_HDD_TIER3, registry.get(ModContentCatalog.COMPAT_HDD_TIER3).name());
        assertEquals(ModContentCatalog.COMPAT_INTERNET_CARD, registry.get(ModContentCatalog.COMPAT_INTERNET_CARD).name());
        assertEquals(ModContentCatalog.COMPAT_LINKED_CARD, registry.get(ModContentCatalog.COMPAT_LINKED_CARD).name());
        assertEquals(ModContentCatalog.COMPAT_GRAPHICS_CARD_TIER1, registry.get(ModContentCatalog.COMPAT_GRAPHICS_CARD_TIER1).name());
        assertEquals(ModContentCatalog.COMPAT_GRAPHICS_CARD_TIER2, registry.get(ModContentCatalog.COMPAT_GRAPHICS_CARD_TIER2).name());
        assertEquals(ModContentCatalog.COMPAT_GRAPHICS_CARD_TIER3, registry.get(ModContentCatalog.COMPAT_GRAPHICS_CARD_TIER3).name());
        assertEquals(ModContentCatalog.COMPAT_NETWORK_CARD, registry.get(ModContentCatalog.COMPAT_NETWORK_CARD).name());
        assertEquals(ModContentCatalog.COMPAT_WIRELESS_NETWORK_CARD_TIER1, registry.get(ModContentCatalog.COMPAT_WIRELESS_NETWORK_CARD_TIER1).name());
        assertEquals(ModContentCatalog.COMPAT_WIRELESS_NETWORK_CARD_TIER2, registry.get(ModContentCatalog.COMPAT_WIRELESS_NETWORK_CARD_TIER2).name());
        assertEquals(ModContentCatalog.COMPAT_WIRELESS_NETWORK_CARD, registry.get(ModContentCatalog.COMPAT_WIRELESS_NETWORK_CARD).name());
        assertEquals(ModContentCatalog.COMPAT_REDSTONE_CARD, registry.get(ModContentCatalog.COMPAT_REDSTONE_CARD).name());
    }
}
