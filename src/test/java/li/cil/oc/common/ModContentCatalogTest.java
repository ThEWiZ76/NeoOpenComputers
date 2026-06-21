package li.cil.oc.common;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class ModContentCatalogTest {
    @Test
    void registersMaterialApiItemNames() {
        final ItemRegistry registry = new ItemRegistry();

        ModContentCatalog.registerMaterialItems(
            registry,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);

        assertNotNull(registry.get(ModContentIds.TRANSISTOR));
        assertNotNull(registry.get(ModContentIds.CAPACITOR));
        assertNotNull(registry.get(ModContentIds.CHAMELIUM));
        assertNotNull(registry.get(ModContentIds.INK_CARTRIDGE_EMPTY));
        assertNotNull(registry.get(ModContentIds.INK_CARTRIDGE));
        assertNotNull(registry.get(ModContentIds.PRINTED_CIRCUIT_BOARD));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_MATERIAL_TRANSISTOR));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_CAPACITOR));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_CHAMELIUM));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_INK_CARTRIDGE_EMPTY));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_INK_CARTRIDGE));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_COMPONENT_BUS_TIER1));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_COMPONENT_BUS_TIER2));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_COMPONENT_BUS_TIER3));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_CIRCUIT_CHIP_TIER1));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_MATERIAL_CARD));
        assertEquals(ModContentIds.TRANSISTOR, registry.get(ModContentIds.TRANSISTOR).name());
        assertEquals(ModContentIds.CAPACITOR, registry.get(ModContentIds.CAPACITOR).name());
        assertEquals(ModContentIds.CHAMELIUM, registry.get(ModContentIds.CHAMELIUM).name());
        assertEquals(ModContentIds.INK_CARTRIDGE_EMPTY, registry.get(ModContentIds.INK_CARTRIDGE_EMPTY).name());
        assertEquals(ModContentIds.INK_CARTRIDGE, registry.get(ModContentIds.INK_CARTRIDGE).name());
        assertEquals(ModContentCatalog.COMPAT_MATERIAL_TRANSISTOR, registry.get(ModContentCatalog.COMPAT_MATERIAL_TRANSISTOR).name());
        assertEquals(ModContentCatalog.COMPAT_CAPACITOR, registry.get(ModContentCatalog.COMPAT_CAPACITOR).name());
        assertEquals(ModContentCatalog.COMPAT_CHAMELIUM, registry.get(ModContentCatalog.COMPAT_CHAMELIUM).name());
        assertEquals(ModContentCatalog.COMPAT_INK_CARTRIDGE_EMPTY, registry.get(ModContentCatalog.COMPAT_INK_CARTRIDGE_EMPTY).name());
        assertEquals(ModContentCatalog.COMPAT_INK_CARTRIDGE, registry.get(ModContentCatalog.COMPAT_INK_CARTRIDGE).name());
        assertEquals(ModContentCatalog.COMPAT_COMPONENT_BUS_TIER1, registry.get(ModContentCatalog.COMPAT_COMPONENT_BUS_TIER1).name());
        assertEquals(ModContentCatalog.COMPAT_COMPONENT_BUS_TIER2, registry.get(ModContentCatalog.COMPAT_COMPONENT_BUS_TIER2).name());
        assertEquals(ModContentCatalog.COMPAT_COMPONENT_BUS_TIER3, registry.get(ModContentCatalog.COMPAT_COMPONENT_BUS_TIER3).name());
        assertEquals(ModContentCatalog.COMPAT_CIRCUIT_CHIP_TIER1, registry.get(ModContentCatalog.COMPAT_CIRCUIT_CHIP_TIER1).name());
    }

    @Test
    void registersInitialApiItemNames() throws ReflectiveOperationException {
        final ItemRegistry registry = new ItemRegistry();

        invokeRegister(registry);

        assertNotNull(registry.get(ModContentIds.ADAPTER));
        assertNotNull(registry.get(ModContentIds.ASSEMBLER));
        assertNotNull(registry.get(ModContentIds.MANUAL));
        assertNotNull(registry.get(ModContentIds.ANALYZER));
        assertNotNull(registry.get(ModContentIds.WRENCH));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_WRENCH));
        assertNotNull(registry.get(ModContentIds.TEXTURE_PICKER));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_TEXTURE_PICKER));
        assertNotNull(registry.get(ModContentIds.TERMINAL));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_TERMINAL));
        assertNotNull(registry.get(ModContentIds.TERMINAL_SERVER));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_TERMINAL_SERVER));
        assertNotNull(registry.get(ModContentIds.NANOMACHINES));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_NANOMACHINES));
        assertNotNull(registry.get(ModContentIds.SERVER_TIER1));
        assertNotNull(registry.get(ModContentIds.SERVER_TIER2));
        assertNotNull(registry.get(ModContentIds.SERVER_TIER3));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_SERVER_TIER1));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_SERVER_TIER2));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_SERVER_TIER3));
        assertNotNull(registry.get(ModContentIds.APU_TIER1));
        assertNotNull(registry.get(ModContentIds.APU_TIER2));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_APU_TIER1));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_APU_TIER2));
        assertNotNull(registry.get(ModContentIds.CABLE));
        assertNotNull(registry.get(ModContentIds.COMPUTER_CASE_TIER1));
        assertNotNull(registry.get(ModContentIds.COMPUTER_CASE_TIER2));
        assertNotNull(registry.get(ModContentIds.COMPUTER_CASE_TIER3));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_COMPUTER_CASE_TIER1));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_COMPUTER_CASE_TIER2));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_COMPUTER_CASE_TIER3));
        assertNotNull(registry.get(ModContentIds.DISASSEMBLER));
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
        assertNotNull(registry.get(ModContentIds.CARD_CONTAINER_TIER1));
        assertNotNull(registry.get(ModContentIds.CARD_CONTAINER_TIER2));
        assertNotNull(registry.get(ModContentIds.CARD_CONTAINER_TIER3));
        assertNotNull(registry.get(ModContentIds.TABLET_CASE_TIER1));
        assertNotNull(registry.get(ModContentIds.TABLET_CASE_TIER2));
        assertNotNull(registry.get(ModContentIds.TABLET_CASE_CREATIVE));
        assertNotNull(registry.get(ModContentIds.TABLET));
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
        assertNotNull(registry.get(ModContentIds.BATTERY_UPGRADE_TIER1));
        assertNotNull(registry.get(ModContentIds.BATTERY_UPGRADE_TIER2));
        assertNotNull(registry.get(ModContentIds.BATTERY_UPGRADE_TIER3));
        assertNotNull(registry.get(ModContentIds.INVENTORY_UPGRADE));
        assertNotNull(registry.get(ModContentIds.CRAFTING_UPGRADE));
        assertNotNull(registry.get(ModContentIds.EXPERIENCE_UPGRADE));
        assertNotNull(registry.get(ModContentIds.PISTON_UPGRADE));
        assertNotNull(registry.get(ModContentIds.STICKY_PISTON_UPGRADE));
        assertNotNull(registry.get(ModContentIds.SIGN_UPGRADE));
        assertNotNull(registry.get(ModContentIds.TRADING_UPGRADE));
        assertNotNull(registry.get(ModContentIds.TRACTOR_BEAM_UPGRADE));
        assertNotNull(registry.get(ModContentIds.LEASH_UPGRADE));
        assertNotNull(registry.get(ModContentIds.ANGEL_UPGRADE));
        assertNotNull(registry.get(ModContentIds.CHUNKLOADER_UPGRADE));
        assertNotNull(registry.get(ModContentIds.MFU));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_DATABASE_UPGRADE_TIER1));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_DATABASE_UPGRADE_TIER2));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_DATABASE_UPGRADE_TIER3));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_BATTERY_UPGRADE_TIER1));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_BATTERY_UPGRADE_TIER2));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_BATTERY_UPGRADE_TIER3));
        assertNotNull(registry.get(ModContentIds.INVENTORY_CONTROLLER_UPGRADE));
        assertNotNull(registry.get(ModContentIds.TANK_UPGRADE));
        assertNotNull(registry.get(ModContentIds.TANK_CONTROLLER_UPGRADE));
        assertNotNull(registry.get(ModContentIds.INTERNET_CARD));
        assertNotNull(registry.get(ModContentIds.LINKED_CARD));
        assertNotNull(registry.get(ModContentIds.NAVIGATION_UPGRADE));
        assertNotNull(registry.get(ModContentIds.EEPROM));
        assertNotNull(registry.get(ModContentIds.FLOPPY));
        assertNotNull(registry.get(ModContentIds.GRAPHICS_CARD_TIER1));
        assertNotNull(registry.get(ModContentIds.GRAPHICS_CARD_TIER2));
        assertNotNull(registry.get(ModContentIds.GRAPHICS_CARD_TIER3));
        assertNotNull(registry.get(ModContentIds.HOVER_UPGRADE_TIER1));
        assertNotNull(registry.get(ModContentIds.HOVER_UPGRADE_TIER2));
        assertNotNull(registry.get(ModContentIds.NETWORK_CARD));
        assertNotNull(registry.get(ModContentIds.WIRELESS_NETWORK_CARD_TIER1));
        assertNotNull(registry.get(ModContentIds.WIRELESS_NETWORK_CARD_TIER2));
        assertNotNull(registry.get(ModContentIds.REDSTONE_CARD));
        assertNotNull(registry.get(ModContentIds.SOLAR_GENERATOR_UPGRADE));
        assertNotNull(registry.get(ModContentIds.GENERATOR_UPGRADE));
        assertNotNull(registry.get(ModContentIds.UPGRADE_CONTAINER_TIER1));
        assertNotNull(registry.get(ModContentIds.UPGRADE_CONTAINER_TIER2));
        assertNotNull(registry.get(ModContentIds.UPGRADE_CONTAINER_TIER3));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_CPU_TIER1));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_CPU_TIER2));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_CPU_TIER3));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_CARD_CONTAINER_TIER1));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_CARD_CONTAINER_TIER2));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_CARD_CONTAINER_TIER3));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_TABLET_CASE_TIER1));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_TABLET_CASE_TIER2));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_TABLET_CASE_CREATIVE));
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
        assertNotNull(registry.get(ModContentCatalog.COMPAT_INVENTORY_UPGRADE));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_CRAFTING_UPGRADE));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_EXPERIENCE_UPGRADE));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_PISTON_UPGRADE));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_STICKY_PISTON_UPGRADE));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_SIGN_UPGRADE));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_TRADING_UPGRADE));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_TRACTOR_BEAM_UPGRADE));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_LEASH_UPGRADE));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_ANGEL_UPGRADE));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_CHUNKLOADER_UPGRADE));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_MFU));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_TANK_UPGRADE));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_TANK_CONTROLLER_UPGRADE));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_INTERNET_CARD));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_LINKED_CARD));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_NAVIGATION_UPGRADE));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_GRAPHICS_CARD_TIER1));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_GRAPHICS_CARD_TIER2));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_GRAPHICS_CARD_TIER3));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_HOVER_UPGRADE_TIER1));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_HOVER_UPGRADE_TIER2));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_NETWORK_CARD));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_WIRELESS_NETWORK_CARD_TIER1));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_WIRELESS_NETWORK_CARD_TIER2));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_WIRELESS_NETWORK_CARD));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_REDSTONE_CARD));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_SOLAR_GENERATOR_UPGRADE));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_GENERATOR_UPGRADE));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_UPGRADE_CONTAINER_TIER1));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_UPGRADE_CONTAINER_TIER2));
        assertNotNull(registry.get(ModContentCatalog.COMPAT_UPGRADE_CONTAINER_TIER3));
        assertEquals(ModContentIds.ADAPTER, registry.get(ModContentIds.ADAPTER).name());
        assertEquals(ModContentIds.ASSEMBLER, registry.get(ModContentIds.ASSEMBLER).name());
        assertEquals(ModContentIds.MANUAL, registry.get(ModContentIds.MANUAL).name());
        assertEquals(ModContentIds.ANALYZER, registry.get(ModContentIds.ANALYZER).name());
        assertEquals(ModContentIds.WRENCH, registry.get(ModContentIds.WRENCH).name());
        assertEquals(ModContentCatalog.COMPAT_WRENCH, registry.get(ModContentCatalog.COMPAT_WRENCH).name());
        assertEquals(ModContentIds.TEXTURE_PICKER, registry.get(ModContentIds.TEXTURE_PICKER).name());
        assertEquals(ModContentCatalog.COMPAT_TEXTURE_PICKER, registry.get(ModContentCatalog.COMPAT_TEXTURE_PICKER).name());
        assertEquals(ModContentIds.TERMINAL, registry.get(ModContentIds.TERMINAL).name());
        assertEquals(ModContentCatalog.COMPAT_TERMINAL, registry.get(ModContentCatalog.COMPAT_TERMINAL).name());
        assertEquals(ModContentIds.TERMINAL_SERVER, registry.get(ModContentIds.TERMINAL_SERVER).name());
        assertEquals(ModContentCatalog.COMPAT_TERMINAL_SERVER, registry.get(ModContentCatalog.COMPAT_TERMINAL_SERVER).name());
        assertEquals(ModContentIds.NANOMACHINES, registry.get(ModContentIds.NANOMACHINES).name());
        assertEquals(ModContentCatalog.COMPAT_NANOMACHINES, registry.get(ModContentCatalog.COMPAT_NANOMACHINES).name());
        assertEquals(ModContentIds.SERVER_TIER1, registry.get(ModContentIds.SERVER_TIER1).name());
        assertEquals(ModContentIds.SERVER_TIER2, registry.get(ModContentIds.SERVER_TIER2).name());
        assertEquals(ModContentIds.SERVER_TIER3, registry.get(ModContentIds.SERVER_TIER3).name());
        assertEquals(ModContentCatalog.COMPAT_SERVER_TIER1, registry.get(ModContentCatalog.COMPAT_SERVER_TIER1).name());
        assertEquals(ModContentCatalog.COMPAT_SERVER_TIER2, registry.get(ModContentCatalog.COMPAT_SERVER_TIER2).name());
        assertEquals(ModContentCatalog.COMPAT_SERVER_TIER3, registry.get(ModContentCatalog.COMPAT_SERVER_TIER3).name());
        assertEquals(ModContentIds.APU_TIER1, registry.get(ModContentIds.APU_TIER1).name());
        assertEquals(ModContentIds.APU_TIER2, registry.get(ModContentIds.APU_TIER2).name());
        assertEquals(ModContentCatalog.COMPAT_APU_TIER1, registry.get(ModContentCatalog.COMPAT_APU_TIER1).name());
        assertEquals(ModContentCatalog.COMPAT_APU_TIER2, registry.get(ModContentCatalog.COMPAT_APU_TIER2).name());
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
        assertEquals(ModContentIds.CARD_CONTAINER_TIER1, registry.get(ModContentIds.CARD_CONTAINER_TIER1).name());
        assertEquals(ModContentIds.CARD_CONTAINER_TIER2, registry.get(ModContentIds.CARD_CONTAINER_TIER2).name());
        assertEquals(ModContentIds.CARD_CONTAINER_TIER3, registry.get(ModContentIds.CARD_CONTAINER_TIER3).name());
        assertEquals(ModContentIds.TABLET_CASE_TIER1, registry.get(ModContentIds.TABLET_CASE_TIER1).name());
        assertEquals(ModContentIds.TABLET_CASE_TIER2, registry.get(ModContentIds.TABLET_CASE_TIER2).name());
        assertEquals(ModContentIds.TABLET_CASE_CREATIVE, registry.get(ModContentIds.TABLET_CASE_CREATIVE).name());
        assertEquals(ModContentIds.TABLET, registry.get(ModContentIds.TABLET).name());
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
        assertEquals(ModContentIds.BATTERY_UPGRADE_TIER1, registry.get(ModContentIds.BATTERY_UPGRADE_TIER1).name());
        assertEquals(ModContentIds.BATTERY_UPGRADE_TIER2, registry.get(ModContentIds.BATTERY_UPGRADE_TIER2).name());
        assertEquals(ModContentIds.BATTERY_UPGRADE_TIER3, registry.get(ModContentIds.BATTERY_UPGRADE_TIER3).name());
        assertEquals(ModContentIds.INVENTORY_UPGRADE, registry.get(ModContentIds.INVENTORY_UPGRADE).name());
        assertEquals(ModContentIds.CRAFTING_UPGRADE, registry.get(ModContentIds.CRAFTING_UPGRADE).name());
        assertEquals(ModContentIds.EXPERIENCE_UPGRADE, registry.get(ModContentIds.EXPERIENCE_UPGRADE).name());
        assertEquals(ModContentIds.PISTON_UPGRADE, registry.get(ModContentIds.PISTON_UPGRADE).name());
        assertEquals(ModContentIds.STICKY_PISTON_UPGRADE, registry.get(ModContentIds.STICKY_PISTON_UPGRADE).name());
        assertEquals(ModContentIds.SIGN_UPGRADE, registry.get(ModContentIds.SIGN_UPGRADE).name());
        assertEquals(ModContentIds.TRADING_UPGRADE, registry.get(ModContentIds.TRADING_UPGRADE).name());
        assertEquals(ModContentIds.TRACTOR_BEAM_UPGRADE, registry.get(ModContentIds.TRACTOR_BEAM_UPGRADE).name());
        assertEquals(ModContentIds.LEASH_UPGRADE, registry.get(ModContentIds.LEASH_UPGRADE).name());
        assertEquals(ModContentIds.ANGEL_UPGRADE, registry.get(ModContentIds.ANGEL_UPGRADE).name());
        assertEquals(ModContentIds.CHUNKLOADER_UPGRADE, registry.get(ModContentIds.CHUNKLOADER_UPGRADE).name());
        assertEquals(ModContentIds.MFU, registry.get(ModContentIds.MFU).name());
        assertEquals(ModContentCatalog.COMPAT_DATABASE_UPGRADE_TIER1, registry.get(ModContentCatalog.COMPAT_DATABASE_UPGRADE_TIER1).name());
        assertEquals(ModContentCatalog.COMPAT_DATABASE_UPGRADE_TIER2, registry.get(ModContentCatalog.COMPAT_DATABASE_UPGRADE_TIER2).name());
        assertEquals(ModContentCatalog.COMPAT_DATABASE_UPGRADE_TIER3, registry.get(ModContentCatalog.COMPAT_DATABASE_UPGRADE_TIER3).name());
        assertEquals(ModContentCatalog.COMPAT_BATTERY_UPGRADE_TIER1, registry.get(ModContentCatalog.COMPAT_BATTERY_UPGRADE_TIER1).name());
        assertEquals(ModContentCatalog.COMPAT_BATTERY_UPGRADE_TIER2, registry.get(ModContentCatalog.COMPAT_BATTERY_UPGRADE_TIER2).name());
        assertEquals(ModContentCatalog.COMPAT_BATTERY_UPGRADE_TIER3, registry.get(ModContentCatalog.COMPAT_BATTERY_UPGRADE_TIER3).name());
        assertEquals(ModContentIds.INVENTORY_CONTROLLER_UPGRADE, registry.get(ModContentIds.INVENTORY_CONTROLLER_UPGRADE).name());
        assertEquals(ModContentIds.TANK_UPGRADE, registry.get(ModContentIds.TANK_UPGRADE).name());
        assertEquals(ModContentIds.TANK_CONTROLLER_UPGRADE, registry.get(ModContentIds.TANK_CONTROLLER_UPGRADE).name());
        assertEquals(ModContentIds.INTERNET_CARD, registry.get(ModContentIds.INTERNET_CARD).name());
        assertEquals(ModContentIds.LINKED_CARD, registry.get(ModContentIds.LINKED_CARD).name());
        assertEquals(ModContentIds.NAVIGATION_UPGRADE, registry.get(ModContentIds.NAVIGATION_UPGRADE).name());
        assertEquals(ModContentIds.EEPROM, registry.get(ModContentIds.EEPROM).name());
        assertEquals(ModContentIds.FLOPPY, registry.get(ModContentIds.FLOPPY).name());
        assertEquals(ModContentIds.GRAPHICS_CARD_TIER1, registry.get(ModContentIds.GRAPHICS_CARD_TIER1).name());
        assertEquals(ModContentIds.GRAPHICS_CARD_TIER2, registry.get(ModContentIds.GRAPHICS_CARD_TIER2).name());
        assertEquals(ModContentIds.GRAPHICS_CARD_TIER3, registry.get(ModContentIds.GRAPHICS_CARD_TIER3).name());
        assertEquals(ModContentIds.HOVER_UPGRADE_TIER1, registry.get(ModContentIds.HOVER_UPGRADE_TIER1).name());
        assertEquals(ModContentIds.HOVER_UPGRADE_TIER2, registry.get(ModContentIds.HOVER_UPGRADE_TIER2).name());
        assertEquals(ModContentIds.NETWORK_CARD, registry.get(ModContentIds.NETWORK_CARD).name());
        assertEquals(ModContentIds.WIRELESS_NETWORK_CARD_TIER1, registry.get(ModContentIds.WIRELESS_NETWORK_CARD_TIER1).name());
        assertEquals(ModContentIds.WIRELESS_NETWORK_CARD_TIER2, registry.get(ModContentIds.WIRELESS_NETWORK_CARD_TIER2).name());
        assertEquals(ModContentIds.REDSTONE_CARD, registry.get(ModContentIds.REDSTONE_CARD).name());
        assertEquals(ModContentIds.SOLAR_GENERATOR_UPGRADE, registry.get(ModContentIds.SOLAR_GENERATOR_UPGRADE).name());
        assertEquals(ModContentIds.GENERATOR_UPGRADE, registry.get(ModContentIds.GENERATOR_UPGRADE).name());
        assertEquals(ModContentIds.UPGRADE_CONTAINER_TIER1, registry.get(ModContentIds.UPGRADE_CONTAINER_TIER1).name());
        assertEquals(ModContentIds.UPGRADE_CONTAINER_TIER2, registry.get(ModContentIds.UPGRADE_CONTAINER_TIER2).name());
        assertEquals(ModContentIds.UPGRADE_CONTAINER_TIER3, registry.get(ModContentIds.UPGRADE_CONTAINER_TIER3).name());
        assertEquals(ModContentCatalog.COMPAT_CPU_TIER1, registry.get(ModContentCatalog.COMPAT_CPU_TIER1).name());
        assertEquals(ModContentCatalog.COMPAT_CPU_TIER2, registry.get(ModContentCatalog.COMPAT_CPU_TIER2).name());
        assertEquals(ModContentCatalog.COMPAT_CPU_TIER3, registry.get(ModContentCatalog.COMPAT_CPU_TIER3).name());
        assertEquals(ModContentCatalog.COMPAT_CARD_CONTAINER_TIER1, registry.get(ModContentCatalog.COMPAT_CARD_CONTAINER_TIER1).name());
        assertEquals(ModContentCatalog.COMPAT_CARD_CONTAINER_TIER2, registry.get(ModContentCatalog.COMPAT_CARD_CONTAINER_TIER2).name());
        assertEquals(ModContentCatalog.COMPAT_CARD_CONTAINER_TIER3, registry.get(ModContentCatalog.COMPAT_CARD_CONTAINER_TIER3).name());
        assertEquals(ModContentCatalog.COMPAT_TABLET_CASE_TIER1, registry.get(ModContentCatalog.COMPAT_TABLET_CASE_TIER1).name());
        assertEquals(ModContentCatalog.COMPAT_TABLET_CASE_TIER2, registry.get(ModContentCatalog.COMPAT_TABLET_CASE_TIER2).name());
        assertEquals(ModContentCatalog.COMPAT_TABLET_CASE_CREATIVE, registry.get(ModContentCatalog.COMPAT_TABLET_CASE_CREATIVE).name());
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
        assertEquals(ModContentCatalog.COMPAT_INVENTORY_UPGRADE, registry.get(ModContentCatalog.COMPAT_INVENTORY_UPGRADE).name());
        assertEquals(ModContentCatalog.COMPAT_CRAFTING_UPGRADE, registry.get(ModContentCatalog.COMPAT_CRAFTING_UPGRADE).name());
        assertEquals(ModContentCatalog.COMPAT_EXPERIENCE_UPGRADE, registry.get(ModContentCatalog.COMPAT_EXPERIENCE_UPGRADE).name());
        assertEquals(ModContentCatalog.COMPAT_PISTON_UPGRADE, registry.get(ModContentCatalog.COMPAT_PISTON_UPGRADE).name());
        assertEquals(ModContentCatalog.COMPAT_STICKY_PISTON_UPGRADE, registry.get(ModContentCatalog.COMPAT_STICKY_PISTON_UPGRADE).name());
        assertEquals(ModContentCatalog.COMPAT_SIGN_UPGRADE, registry.get(ModContentCatalog.COMPAT_SIGN_UPGRADE).name());
        assertEquals(ModContentCatalog.COMPAT_TRADING_UPGRADE, registry.get(ModContentCatalog.COMPAT_TRADING_UPGRADE).name());
        assertEquals(ModContentCatalog.COMPAT_TRACTOR_BEAM_UPGRADE, registry.get(ModContentCatalog.COMPAT_TRACTOR_BEAM_UPGRADE).name());
        assertEquals(ModContentCatalog.COMPAT_LEASH_UPGRADE, registry.get(ModContentCatalog.COMPAT_LEASH_UPGRADE).name());
        assertEquals(ModContentCatalog.COMPAT_ANGEL_UPGRADE, registry.get(ModContentCatalog.COMPAT_ANGEL_UPGRADE).name());
        assertEquals(ModContentCatalog.COMPAT_CHUNKLOADER_UPGRADE, registry.get(ModContentCatalog.COMPAT_CHUNKLOADER_UPGRADE).name());
        assertEquals(ModContentCatalog.COMPAT_MFU, registry.get(ModContentCatalog.COMPAT_MFU).name());
        assertEquals(ModContentCatalog.COMPAT_TANK_UPGRADE, registry.get(ModContentCatalog.COMPAT_TANK_UPGRADE).name());
        assertEquals(ModContentCatalog.COMPAT_TANK_CONTROLLER_UPGRADE, registry.get(ModContentCatalog.COMPAT_TANK_CONTROLLER_UPGRADE).name());
        assertEquals(ModContentCatalog.COMPAT_INTERNET_CARD, registry.get(ModContentCatalog.COMPAT_INTERNET_CARD).name());
        assertEquals(ModContentCatalog.COMPAT_LINKED_CARD, registry.get(ModContentCatalog.COMPAT_LINKED_CARD).name());
        assertEquals(ModContentCatalog.COMPAT_NAVIGATION_UPGRADE, registry.get(ModContentCatalog.COMPAT_NAVIGATION_UPGRADE).name());
        assertEquals(ModContentCatalog.COMPAT_GRAPHICS_CARD_TIER1, registry.get(ModContentCatalog.COMPAT_GRAPHICS_CARD_TIER1).name());
        assertEquals(ModContentCatalog.COMPAT_GRAPHICS_CARD_TIER2, registry.get(ModContentCatalog.COMPAT_GRAPHICS_CARD_TIER2).name());
        assertEquals(ModContentCatalog.COMPAT_GRAPHICS_CARD_TIER3, registry.get(ModContentCatalog.COMPAT_GRAPHICS_CARD_TIER3).name());
        assertEquals(ModContentCatalog.COMPAT_HOVER_UPGRADE_TIER1, registry.get(ModContentCatalog.COMPAT_HOVER_UPGRADE_TIER1).name());
        assertEquals(ModContentCatalog.COMPAT_HOVER_UPGRADE_TIER2, registry.get(ModContentCatalog.COMPAT_HOVER_UPGRADE_TIER2).name());
        assertEquals(ModContentCatalog.COMPAT_NETWORK_CARD, registry.get(ModContentCatalog.COMPAT_NETWORK_CARD).name());
        assertEquals(ModContentCatalog.COMPAT_WIRELESS_NETWORK_CARD_TIER1, registry.get(ModContentCatalog.COMPAT_WIRELESS_NETWORK_CARD_TIER1).name());
        assertEquals(ModContentCatalog.COMPAT_WIRELESS_NETWORK_CARD_TIER2, registry.get(ModContentCatalog.COMPAT_WIRELESS_NETWORK_CARD_TIER2).name());
        assertEquals(ModContentCatalog.COMPAT_WIRELESS_NETWORK_CARD, registry.get(ModContentCatalog.COMPAT_WIRELESS_NETWORK_CARD).name());
        assertEquals(ModContentCatalog.COMPAT_REDSTONE_CARD, registry.get(ModContentCatalog.COMPAT_REDSTONE_CARD).name());
        assertEquals(ModContentCatalog.COMPAT_SOLAR_GENERATOR_UPGRADE, registry.get(ModContentCatalog.COMPAT_SOLAR_GENERATOR_UPGRADE).name());
        assertEquals(ModContentCatalog.COMPAT_GENERATOR_UPGRADE, registry.get(ModContentCatalog.COMPAT_GENERATOR_UPGRADE).name());
        assertEquals(ModContentCatalog.COMPAT_UPGRADE_CONTAINER_TIER1, registry.get(ModContentCatalog.COMPAT_UPGRADE_CONTAINER_TIER1).name());
        assertEquals(ModContentCatalog.COMPAT_UPGRADE_CONTAINER_TIER2, registry.get(ModContentCatalog.COMPAT_UPGRADE_CONTAINER_TIER2).name());
        assertEquals(ModContentCatalog.COMPAT_UPGRADE_CONTAINER_TIER3, registry.get(ModContentCatalog.COMPAT_UPGRADE_CONTAINER_TIER3).name());
    }

    private static void invokeRegister(final ItemRegistry registry) throws ReflectiveOperationException {
        Method register = null;
        for (Method method : ModContentCatalog.class.getDeclaredMethods()) {
            if ("register".equals(method.getName()) && method.getParameterTypes()[0] == ItemRegistry.class) {
                register = method;
                break;
            }
        }
        assertNotNull(register);
        Object[] arguments = new Object[register.getParameterCount()];
        arguments[0] = registry;
        register.invoke(null, arguments);
    }
}
