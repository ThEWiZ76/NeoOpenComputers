package li.cil.oc.common;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ModContentRegistrationTest {
    @Test
    void manualItemIdIsStable() {
        assertEquals("manual", ModContentIds.MANUAL);
    }

    @Test
    void creativeTabIdIsStable() {
        assertEquals("main", ModContentIds.MAIN_CREATIVE_TAB);
    }

    @Test
    void adapterBlockIdIsStable() {
        assertEquals("adapter", ModContentIds.ADAPTER);
    }

    @Test
    void adapterBlockEntityIdIsStable() {
        assertEquals("adapter", ModContentIds.ADAPTER_BLOCK_ENTITY);
    }

    @Test
    void assemblerBlockIdIsStable() {
        assertEquals("assembler", ModContentIds.ASSEMBLER);
    }

    @Test
    void assemblerBlockEntityIdIsStable() {
        assertEquals("assembler", ModContentIds.ASSEMBLER_BLOCK_ENTITY);
    }

    @Test
    void materialItemIdsAreStable() {
        Map<String, String> ids = Map.ofEntries(
            Map.entry("cutting_wire", ModContentIds.CUTTING_WIRE),
            Map.entry("acid", ModContentIds.ACID),
            Map.entry("raw_circuit_board", ModContentIds.RAW_CIRCUIT_BOARD),
            Map.entry("circuit_board", ModContentIds.CIRCUIT_BOARD),
            Map.entry("printed_circuit_board", ModContentIds.PRINTED_CIRCUIT_BOARD),
            Map.entry("card", ModContentIds.CARD),
            Map.entry("chamelium", ModContentIds.CHAMELIUM),
            Map.entry("transistor", ModContentIds.TRANSISTOR),
            Map.entry("capacitor", ModContentIds.CAPACITOR),
            Map.entry("component_bus_tier1", ModContentIds.COMPONENT_BUS_TIER1),
            Map.entry("component_bus_tier2", ModContentIds.COMPONENT_BUS_TIER2),
            Map.entry("component_bus_tier3", ModContentIds.COMPONENT_BUS_TIER3),
            Map.entry("microchip_tier1", ModContentIds.MICROCHIP_TIER1),
            Map.entry("microchip_tier2", ModContentIds.MICROCHIP_TIER2),
            Map.entry("microchip_tier3", ModContentIds.MICROCHIP_TIER3),
            Map.entry("alu", ModContentIds.ALU),
            Map.entry("control_unit", ModContentIds.CONTROL_UNIT),
            Map.entry("disk_platter", ModContentIds.DISK_PLATTER),
            Map.entry("interweb", ModContentIds.INTERWEB),
            Map.entry("button_group", ModContentIds.BUTTON_GROUP),
            Map.entry("arrow_keys", ModContentIds.ARROW_KEYS),
            Map.entry("num_pad", ModContentIds.NUM_PAD));

        ids.forEach((expected, actual) -> assertEquals(expected, actual));
    }

    @Test
    void analyzerItemIdIsStable() {
        assertEquals("analyzer", ModContentIds.ANALYZER);
    }

    @Test
    void computerCaseBlockIdIsStable() {
        assertEquals("computer_case_tier1", ModContentIds.COMPUTER_CASE_TIER1);
    }

    @Test
    void computerCaseTier2BlockIdIsStable() {
        assertEquals("computer_case_tier2", ModContentIds.COMPUTER_CASE_TIER2);
    }

    @Test
    void computerCaseTier3BlockIdIsStable() {
        assertEquals("computer_case_tier3", ModContentIds.COMPUTER_CASE_TIER3);
    }

    @Test
    void computerCaseBlockEntityIdIsStable() {
        assertEquals("computer_case", ModContentIds.COMPUTER_CASE_BLOCK_ENTITY);
    }

    @Test
    void diskDriveBlockIdIsStable() {
        assertEquals("disk_drive", ModContentIds.DISK_DRIVE);
    }

    @Test
    void diskDriveBlockEntityIdIsStable() {
        assertEquals("disk_drive", ModContentIds.DISK_DRIVE_BLOCK_ENTITY);
    }

    @Test
    void disassemblerBlockIdIsStable() {
        assertEquals("disassembler", ModContentIds.DISASSEMBLER);
    }

    @Test
    void disassemblerBlockEntityIdIsStable() {
        assertEquals("disassembler", ModContentIds.DISASSEMBLER_BLOCK_ENTITY);
    }

    @Test
    void cpuTier1ItemIdIsStable() {
        assertEquals("cpu_tier1", ModContentIds.CPU_TIER1);
    }

    @Test
    void cpuTier2ItemIdIsStable() {
        assertEquals("cpu_tier2", ModContentIds.CPU_TIER2);
    }

    @Test
    void cpuTier3ItemIdIsStable() {
        assertEquals("cpu_tier3", ModContentIds.CPU_TIER3);
    }

    @Test
    void cardContainerTier1ItemIdIsStable() {
        assertEquals("card_container_tier1", ModContentIds.CARD_CONTAINER_TIER1);
    }

    @Test
    void cardContainerTier2ItemIdIsStable() {
        assertEquals("card_container_tier2", ModContentIds.CARD_CONTAINER_TIER2);
    }

    @Test
    void cardContainerTier3ItemIdIsStable() {
        assertEquals("card_container_tier3", ModContentIds.CARD_CONTAINER_TIER3);
    }

    @Test
    void tabletCaseTier1ItemIdIsStable() {
        assertEquals("tablet_case_tier1", ModContentIds.TABLET_CASE_TIER1);
    }

    @Test
    void tabletCaseTier2ItemIdIsStable() {
        assertEquals("tablet_case_tier2", ModContentIds.TABLET_CASE_TIER2);
    }

    @Test
    void tabletCaseCreativeItemIdIsStable() {
        assertEquals("tablet_case_creative", ModContentIds.TABLET_CASE_CREATIVE);
    }

    @Test
    void tabletItemIdIsStable() {
        assertEquals("tablet", ModContentIds.TABLET);
    }

    @Test
    void upgradeContainerTier1ItemIdIsStable() {
        assertEquals("upgrade_container_tier1", ModContentIds.UPGRADE_CONTAINER_TIER1);
    }

    @Test
    void upgradeContainerTier2ItemIdIsStable() {
        assertEquals("upgrade_container_tier2", ModContentIds.UPGRADE_CONTAINER_TIER2);
    }

    @Test
    void upgradeContainerTier3ItemIdIsStable() {
        assertEquals("upgrade_container_tier3", ModContentIds.UPGRADE_CONTAINER_TIER3);
    }

    @Test
    void memoryTier1ItemIdIsStable() {
        assertEquals("memory_tier1", ModContentIds.MEMORY_TIER1);
    }

    @Test
    void memoryTier2ItemIdIsStable() {
        assertEquals("memory_tier2", ModContentIds.MEMORY_TIER2);
    }

    @Test
    void memoryTier3ItemIdIsStable() {
        assertEquals("memory_tier3", ModContentIds.MEMORY_TIER3);
    }

    @Test
    void hardDiskDriveTier1ItemIdIsStable() {
        assertEquals("hdd_tier1", ModContentIds.HDD_TIER1);
    }

    @Test
    void hardDiskDriveTier2ItemIdIsStable() {
        assertEquals("hdd_tier2", ModContentIds.HDD_TIER2);
    }

    @Test
    void hardDiskDriveTier3ItemIdIsStable() {
        assertEquals("hdd_tier3", ModContentIds.HDD_TIER3);
    }

    @Test
    void databaseUpgradeTier1ItemIdIsStable() {
        assertEquals("database_upgrade_tier1", ModContentIds.DATABASE_UPGRADE_TIER1);
    }

    @Test
    void databaseUpgradeTier2ItemIdIsStable() {
        assertEquals("database_upgrade_tier2", ModContentIds.DATABASE_UPGRADE_TIER2);
    }

    @Test
    void databaseUpgradeTier3ItemIdIsStable() {
        assertEquals("database_upgrade_tier3", ModContentIds.DATABASE_UPGRADE_TIER3);
    }

    @Test
    void dataCardTier1ItemIdIsStable() {
        assertEquals("data_card_tier1", ModContentIds.DATA_CARD_TIER1);
    }

    @Test
    void dataCardTier2ItemIdIsStable() {
        assertEquals("data_card_tier2", ModContentIds.DATA_CARD_TIER2);
    }

    @Test
    void dataCardTier3ItemIdIsStable() {
        assertEquals("data_card_tier3", ModContentIds.DATA_CARD_TIER3);
    }

    @Test
    void batteryUpgradeTier1ItemIdIsStable() {
        assertEquals("battery_upgrade_tier1", ModContentIds.BATTERY_UPGRADE_TIER1);
    }

    @Test
    void batteryUpgradeTier2ItemIdIsStable() {
        assertEquals("battery_upgrade_tier2", ModContentIds.BATTERY_UPGRADE_TIER2);
    }

    @Test
    void batteryUpgradeTier3ItemIdIsStable() {
        assertEquals("battery_upgrade_tier3", ModContentIds.BATTERY_UPGRADE_TIER3);
    }

    @Test
    void eepromItemIdIsStable() {
        assertEquals("eeprom", ModContentIds.EEPROM);
    }

    @Test
    void floppyItemIdIsStable() {
        assertEquals("floppy", ModContentIds.FLOPPY);
    }

    @Test
    void graphicsCardTier1ItemIdIsStable() {
        assertEquals("graphics_card_tier1", ModContentIds.GRAPHICS_CARD_TIER1);
    }

    @Test
    void graphicsCardTier2ItemIdIsStable() {
        assertEquals("graphics_card_tier2", ModContentIds.GRAPHICS_CARD_TIER2);
    }

    @Test
    void graphicsCardTier3ItemIdIsStable() {
        assertEquals("graphics_card_tier3", ModContentIds.GRAPHICS_CARD_TIER3);
    }

    @Test
    void hoverUpgradeTier1ItemIdIsStable() {
        assertEquals("hover_upgrade_tier1", ModContentIds.HOVER_UPGRADE_TIER1);
    }

    @Test
    void hoverUpgradeTier2ItemIdIsStable() {
        assertEquals("hover_upgrade_tier2", ModContentIds.HOVER_UPGRADE_TIER2);
    }

    @Test
    void internetCardItemIdIsStable() {
        assertEquals("internet_card", ModContentIds.INTERNET_CARD);
    }

    @Test
    void inventoryUpgradeItemIdIsStable() {
        assertEquals("inventory_upgrade", ModContentIds.INVENTORY_UPGRADE);
    }

    @Test
    void craftingUpgradeItemIdIsStable() {
        assertEquals("crafting_upgrade", ModContentIds.CRAFTING_UPGRADE);
    }

    @Test
    void experienceUpgradeItemIdIsStable() {
        assertEquals("experience_upgrade", ModContentIds.EXPERIENCE_UPGRADE);
    }

    @Test
    void worldInteractionUpgradeItemIdsAreStable() {
        assertEquals("piston_upgrade", ModContentIds.PISTON_UPGRADE);
        assertEquals("sticky_piston_upgrade", ModContentIds.STICKY_PISTON_UPGRADE);
        assertEquals("sign_upgrade", ModContentIds.SIGN_UPGRADE);
        assertEquals("trading_upgrade", ModContentIds.TRADING_UPGRADE);
        assertEquals("tractor_beam_upgrade", ModContentIds.TRACTOR_BEAM_UPGRADE);
        assertEquals("leash_upgrade", ModContentIds.LEASH_UPGRADE);
        assertEquals("angel_upgrade", ModContentIds.ANGEL_UPGRADE);
        assertEquals("chunkloader_upgrade", ModContentIds.CHUNKLOADER_UPGRADE);
    }

    @Test
    void tankUpgradeItemIdIsStable() {
        assertEquals("tank_upgrade", ModContentIds.TANK_UPGRADE);
    }

    @Test
    void tankControllerUpgradeItemIdIsStable() {
        assertEquals("tank_controller_upgrade", ModContentIds.TANK_CONTROLLER_UPGRADE);
    }

    @Test
    void solarGeneratorUpgradeItemIdIsStable() {
        assertEquals("solar_generator_upgrade", ModContentIds.SOLAR_GENERATOR_UPGRADE);
    }

    @Test
    void generatorUpgradeItemIdIsStable() throws ReflectiveOperationException {
        assertEquals("generator_upgrade", ModContentIds.class.getField("GENERATOR_UPGRADE").get(null));
    }

    @Test
    void networkCardItemIdIsStable() {
        assertEquals("network_card", ModContentIds.NETWORK_CARD);
    }

    @Test
    void redstoneIoBlockIdIsStable() {
        assertEquals("redstone", ModContentIds.REDSTONE_IO);
    }

    @Test
    void redstoneIoBlockEntityIdIsStable() {
        assertEquals("redstone", ModContentIds.REDSTONE_IO_BLOCK_ENTITY);
    }

    @Test
    void transposerBlockIdIsStable() {
        assertEquals("transposer", ModContentIds.TRANSPOSER);
    }

    @Test
    void transposerBlockEntityIdIsStable() {
        assertEquals("transposer", ModContentIds.TRANSPOSER_BLOCK_ENTITY);
    }

    @Test
    void wirelessNetworkCardTier1ItemIdIsStable() {
        assertEquals("wireless_network_card_tier1", ModContentIds.WIRELESS_NETWORK_CARD_TIER1);
    }

    @Test
    void wirelessNetworkCardTier2ItemIdIsStable() {
        assertEquals("wireless_network_card_tier2", ModContentIds.WIRELESS_NETWORK_CARD_TIER2);
    }

    @Test
    void screenTier1BlockIdIsStable() {
        assertEquals("screen_tier1", ModContentIds.SCREEN_TIER1);
    }

    @Test
    void screenTier2BlockIdIsStable() {
        assertEquals("screen_tier2", ModContentIds.SCREEN_TIER2);
    }

    @Test
    void screenTier3BlockIdIsStable() {
        assertEquals("screen_tier3", ModContentIds.SCREEN_TIER3);
    }

    @Test
    void keyboardBlockIdIsStable() {
        assertEquals("keyboard", ModContentIds.KEYBOARD);
    }

    @Test
    void linkedCardItemIdIsStable() {
        assertEquals("linked_card", ModContentIds.LINKED_CARD);
    }

    @Test
    void navigationUpgradeItemIdIsStable() {
        assertEquals("navigation_upgrade", ModContentIds.NAVIGATION_UPGRADE);
    }

    @Test
    void geolyzerBlockIdIsStable() {
        assertEquals("geolyzer", ModContentIds.GEOLYZER);
    }

    @Test
    void geolyzerBlockEntityIdIsStable() {
        assertEquals("geolyzer", ModContentIds.GEOLYZER_BLOCK_ENTITY);
    }

    @Test
    void hologramTier1BlockIdIsStable() {
        assertEquals("hologram_tier1", ModContentIds.HOLOGRAM_TIER1);
    }

    @Test
    void hologramTier2BlockIdIsStable() {
        assertEquals("hologram_tier2", ModContentIds.HOLOGRAM_TIER2);
    }

    @Test
    void hologramBlockEntityIdIsStable() {
        assertEquals("hologram", ModContentIds.HOLOGRAM_BLOCK_ENTITY);
    }

    @Test
    void waypointBlockIdIsStable() {
        assertEquals("waypoint", ModContentIds.WAYPOINT);
    }

    @Test
    void waypointBlockEntityIdIsStable() {
        assertEquals("waypoint", ModContentIds.WAYPOINT_BLOCK_ENTITY);
    }

    @Test
    void motionSensorBlockIdIsStable() {
        assertEquals("motion_sensor", ModContentIds.MOTION_SENSOR);
    }

    @Test
    void motionSensorBlockEntityIdIsStable() {
        assertEquals("motion_sensor", ModContentIds.MOTION_SENSOR_BLOCK_ENTITY);
    }

    @Test
    void computerCaseMenuIdIsStable() {
        assertEquals("computer_case", ModContentIds.COMPUTER_CASE_MENU);
    }

    @Test
    void diskDriveMenuIdIsStable() {
        assertEquals("disk_drive", ModContentIds.DISK_DRIVE_MENU);
    }
}
