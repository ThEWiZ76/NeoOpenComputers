package li.cil.oc.common;

import li.cil.oc.api.API;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModContentCatalog {
    public static final String COMPAT_BATTERY_UPGRADE_TIER1 = "batteryUpgrade1";
    public static final String COMPAT_BATTERY_UPGRADE_TIER2 = "batteryUpgrade2";
    public static final String COMPAT_BATTERY_UPGRADE_TIER3 = "batteryUpgrade3";
    public static final String COMPAT_CIRCUIT_CHIP_TIER1 = "circuitChip1";
    public static final String COMPAT_CIRCUIT_CHIP_TIER2 = "circuitChip2";
    public static final String COMPAT_CIRCUIT_CHIP_TIER3 = "circuitChip3";
    public static final String COMPAT_CARD_CONTAINER_TIER1 = "cardcontainer1";
    public static final String COMPAT_CARD_CONTAINER_TIER2 = "cardcontainer2";
    public static final String COMPAT_CARD_CONTAINER_TIER3 = "cardcontainer3";
    public static final String COMPAT_TABLET_CASE_TIER1 = "tabletcase1";
    public static final String COMPAT_TABLET_CASE_TIER2 = "tabletcase2";
    public static final String COMPAT_TABLET_CASE_CREATIVE = "tabletcasecreative";
    public static final String COMPAT_COMPUTER_CASE_TIER1 = "case1";
    public static final String COMPAT_COMPUTER_CASE_TIER2 = "case2";
    public static final String COMPAT_COMPUTER_CASE_TIER3 = "case3";
    public static final String COMPAT_CPU_TIER1 = "cpu1";
    public static final String COMPAT_CPU_TIER2 = "cpu2";
    public static final String COMPAT_CPU_TIER3 = "cpu3";
    public static final String COMPAT_DATA_CARD_TIER1 = "datacard1";
    public static final String COMPAT_DATA_CARD_TIER2 = "datacard2";
    public static final String COMPAT_DATA_CARD_TIER3 = "datacard3";
    public static final String COMPAT_DATA_CARD = "datacard";
    public static final String COMPAT_DATABASE_UPGRADE_TIER1 = "databaseUpgrade1";
    public static final String COMPAT_DATABASE_UPGRADE_TIER2 = "databaseUpgrade2";
    public static final String COMPAT_DATABASE_UPGRADE_TIER3 = "databaseUpgrade3";
    public static final String COMPAT_GRAPHICS_CARD_TIER1 = "graphicscard1";
    public static final String COMPAT_GRAPHICS_CARD_TIER2 = "graphicscard2";
    public static final String COMPAT_GRAPHICS_CARD_TIER3 = "graphicscard3";
    public static final String COMPAT_HOVER_UPGRADE_TIER1 = "hoverUpgrade1";
    public static final String COMPAT_HOVER_UPGRADE_TIER2 = "hoverUpgrade2";
    public static final String COMPAT_HOLOGRAM_TIER1 = "hologram1";
    public static final String COMPAT_HOLOGRAM_TIER2 = "hologram2";
    public static final String COMPAT_HDD_TIER1 = "hdd1";
    public static final String COMPAT_HDD_TIER2 = "hdd2";
    public static final String COMPAT_HDD_TIER3 = "hdd3";
    public static final String COMPAT_INVENTORY_UPGRADE = "inventoryUpgrade";
    public static final String COMPAT_INTERNET_CARD = "internetcard";
    public static final String COMPAT_LINKED_CARD = "linkedcard";
    public static final String COMPAT_MEMORY_TIER1 = "ram1";
    public static final String COMPAT_MEMORY_TIER2 = "ram2";
    public static final String COMPAT_MEMORY_TIER3 = "ram3";
    public static final String COMPAT_NAVIGATION_UPGRADE = "navigationUpgrade";
    public static final String COMPAT_NETWORK_CARD = "lancard";
    public static final String COMPAT_REDSTONE_CARD = "redstone";
    public static final String COMPAT_SOLAR_GENERATOR_UPGRADE = "solarGeneratorUpgrade";
    public static final String COMPAT_TANK_CONTROLLER_UPGRADE = "tankControllerUpgrade";
    public static final String COMPAT_TANK_UPGRADE = "tankUpgrade";
    public static final String COMPAT_UPGRADE_CONTAINER_TIER1 = "upgradecontainer1";
    public static final String COMPAT_UPGRADE_CONTAINER_TIER2 = "upgradecontainer2";
    public static final String COMPAT_UPGRADE_CONTAINER_TIER3 = "upgradecontainer3";
    public static final String COMPAT_WIRELESS_NETWORK_CARD_TIER1 = "wlancard1";
    public static final String COMPAT_WIRELESS_NETWORK_CARD_TIER2 = "wlancard2";
    public static final String COMPAT_WIRELESS_NETWORK_CARD = "wlancard";
    public static final String COMPAT_SCREEN_TIER1 = "screen1";
    public static final String COMPAT_SCREEN_TIER2 = "screen2";
    public static final String COMPAT_SCREEN_TIER3 = "screen3";
    public static final String COMPAT_MATERIAL_ACID = "materialAcid";
    public static final String COMPAT_MATERIAL_ALU = "materialALU";
    public static final String COMPAT_MATERIAL_ARROW_KEY = "materialArrowKey";
    public static final String COMPAT_MATERIAL_BUTTON_GROUP = "materialButtonGroup";
    public static final String COMPAT_MATERIAL_CARD = "materialCard";
    public static final String COMPAT_MATERIAL_CIRCUIT_BOARD = "materialCircuitBoard";
    public static final String COMPAT_MATERIAL_CIRCUIT_BOARD_PRINTED = "materialCircuitBoardPrinted";
    public static final String COMPAT_MATERIAL_CIRCUIT_BOARD_RAW = "materialCircuitBoardRaw";
    public static final String COMPAT_MATERIAL_CONTROL_UNIT = "materialCU";
    public static final String COMPAT_MATERIAL_CUTTING_WIRE = "materialCuttingWire";
    public static final String COMPAT_MATERIAL_DISK = "materialDisk";
    public static final String COMPAT_MATERIAL_INTERWEB = "materialInterweb";
    public static final String COMPAT_MATERIAL_NUM_PAD = "materialNumPad";
    public static final String COMPAT_MATERIAL_TRANSISTOR = "materialTransistor";

    public static void registerDefaults() {
        if (API.items instanceof ItemRegistry registry) {
            register(
                registry,
                ModBlocks.ADAPTER.get(),
                ModItems.ADAPTER.get(),
                ModBlocks.ASSEMBLER.get(),
                ModItems.ASSEMBLER.get(),
                ModBlocks.CABLE.get(),
                ModItems.CABLE.get(),
                ModBlocks.COMPUTER_CASE_TIER1.get(),
                ModItems.COMPUTER_CASE_TIER1.get(),
                ModBlocks.COMPUTER_CASE_TIER2.get(),
                ModItems.COMPUTER_CASE_TIER2.get(),
                ModBlocks.COMPUTER_CASE_TIER3.get(),
                ModItems.COMPUTER_CASE_TIER3.get(),
                ModBlocks.DISASSEMBLER.get(),
                ModItems.DISASSEMBLER.get(),
                ModBlocks.DISK_DRIVE.get(),
                ModItems.DISK_DRIVE.get(),
                ModBlocks.GEOLYZER.get(),
                ModItems.GEOLYZER.get(),
                ModBlocks.SCREEN_TIER1.get(),
                ModItems.SCREEN_TIER1.get(),
                ModBlocks.SCREEN_TIER2.get(),
                ModItems.SCREEN_TIER2.get(),
                ModBlocks.SCREEN_TIER3.get(),
                ModItems.SCREEN_TIER3.get(),
                ModBlocks.KEYBOARD.get(),
                ModItems.KEYBOARD.get(),
                ModBlocks.MOTION_SENSOR.get(),
                ModItems.MOTION_SENSOR.get(),
                ModBlocks.REDSTONE_IO.get(),
                ModItems.REDSTONE_IO.get(),
                ModBlocks.TRANSPOSER.get(),
                ModItems.TRANSPOSER.get(),
                ModBlocks.HOLOGRAM_TIER1.get(),
                ModItems.HOLOGRAM_TIER1.get(),
                ModBlocks.HOLOGRAM_TIER2.get(),
                ModItems.HOLOGRAM_TIER2.get(),
                ModBlocks.WAYPOINT.get(),
                ModItems.WAYPOINT.get(),
                ModItems.MANUAL.get(),
                ModItems.ANALYZER.get(),
                ModItems.CPU_TIER1.get(),
                ModItems.CPU_TIER2.get(),
                ModItems.CPU_TIER3.get(),
                ModItems.CARD_CONTAINER_TIER1.get(),
                ModItems.CARD_CONTAINER_TIER2.get(),
                ModItems.CARD_CONTAINER_TIER3.get(),
                ModItems.TABLET_CASE_TIER1.get(),
                ModItems.TABLET_CASE_TIER2.get(),
                ModItems.TABLET_CASE_CREATIVE.get(),
                ModItems.TABLET.get(),
                ModItems.DATA_CARD_TIER1.get(),
                ModItems.DATA_CARD_TIER2.get(),
                ModItems.DATA_CARD_TIER3.get(),
                ModItems.DATABASE_UPGRADE_TIER1.get(),
                ModItems.DATABASE_UPGRADE_TIER2.get(),
                ModItems.DATABASE_UPGRADE_TIER3.get(),
                ModItems.BATTERY_UPGRADE_TIER1.get(),
                ModItems.BATTERY_UPGRADE_TIER2.get(),
                ModItems.BATTERY_UPGRADE_TIER3.get(),
                ModItems.MEMORY_TIER1.get(),
                ModItems.MEMORY_TIER2.get(),
                ModItems.MEMORY_TIER3.get(),
                ModItems.HDD_TIER1.get(),
                ModItems.HDD_TIER2.get(),
                ModItems.HDD_TIER3.get(),
                ModItems.INVENTORY_CONTROLLER_UPGRADE.get(),
                ModItems.INVENTORY_UPGRADE.get(),
                ModItems.INTERNET_CARD.get(),
                ModItems.LINKED_CARD.get(),
                ModItems.NAVIGATION_UPGRADE.get(),
                ModItems.EEPROM.get(),
                ModItems.FLOPPY.get(),
                ModItems.GRAPHICS_CARD_TIER1.get(),
                ModItems.GRAPHICS_CARD_TIER2.get(),
                ModItems.GRAPHICS_CARD_TIER3.get(),
                ModItems.HOVER_UPGRADE_TIER1.get(),
                ModItems.HOVER_UPGRADE_TIER2.get(),
                ModItems.NETWORK_CARD.get(),
                ModItems.WIRELESS_NETWORK_CARD_TIER1.get(),
                ModItems.WIRELESS_NETWORK_CARD_TIER2.get(),
                ModItems.REDSTONE_CARD.get(),
                ModItems.SOLAR_GENERATOR_UPGRADE.get(),
                ModItems.TANK_UPGRADE.get(),
                ModItems.TANK_CONTROLLER_UPGRADE.get(),
                ModItems.UPGRADE_CONTAINER_TIER1.get(),
                ModItems.UPGRADE_CONTAINER_TIER2.get(),
                ModItems.UPGRADE_CONTAINER_TIER3.get());
            registerMaterialItems(
                registry,
                ModItems.CUTTING_WIRE.get(),
                ModItems.ACID.get(),
                ModItems.RAW_CIRCUIT_BOARD.get(),
                ModItems.CIRCUIT_BOARD.get(),
                ModItems.PRINTED_CIRCUIT_BOARD.get(),
                ModItems.CARD.get(),
                ModItems.TRANSISTOR.get(),
                ModItems.MICROCHIP_TIER1.get(),
                ModItems.MICROCHIP_TIER2.get(),
                ModItems.MICROCHIP_TIER3.get(),
                ModItems.ALU.get(),
                ModItems.CONTROL_UNIT.get(),
                ModItems.DISK_PLATTER.get(),
                ModItems.INTERWEB.get(),
                ModItems.BUTTON_GROUP.get(),
                ModItems.ARROW_KEYS.get(),
                ModItems.NUM_PAD.get());
        }
    }

    static void registerMaterialItems(
        final ItemRegistry registry,
        final Item cuttingWireItem,
        final Item acidItem,
        final Item rawCircuitBoardItem,
        final Item circuitBoardItem,
        final Item printedCircuitBoardItem,
        final Item cardItem,
        final Item transistorItem,
        final Item microchipTier1Item,
        final Item microchipTier2Item,
        final Item microchipTier3Item,
        final Item aluItem,
        final Item controlUnitItem,
        final Item diskPlatterItem,
        final Item interwebItem,
        final Item buttonGroupItem,
        final Item arrowKeysItem,
        final Item numPadItem) {
        registry.register(ModContentIds.CUTTING_WIRE, null, cuttingWireItem);
        registry.register(ModContentIds.ACID, null, acidItem);
        registry.register(ModContentIds.RAW_CIRCUIT_BOARD, null, rawCircuitBoardItem);
        registry.register(ModContentIds.CIRCUIT_BOARD, null, circuitBoardItem);
        registry.register(ModContentIds.PRINTED_CIRCUIT_BOARD, null, printedCircuitBoardItem);
        registry.register(ModContentIds.CARD, null, cardItem);
        registry.register(ModContentIds.TRANSISTOR, null, transistorItem);
        registry.register(ModContentIds.MICROCHIP_TIER1, null, microchipTier1Item);
        registry.register(ModContentIds.MICROCHIP_TIER2, null, microchipTier2Item);
        registry.register(ModContentIds.MICROCHIP_TIER3, null, microchipTier3Item);
        registry.register(ModContentIds.ALU, null, aluItem);
        registry.register(ModContentIds.CONTROL_UNIT, null, controlUnitItem);
        registry.register(ModContentIds.DISK_PLATTER, null, diskPlatterItem);
        registry.register(ModContentIds.INTERWEB, null, interwebItem);
        registry.register(ModContentIds.BUTTON_GROUP, null, buttonGroupItem);
        registry.register(ModContentIds.ARROW_KEYS, null, arrowKeysItem);
        registry.register(ModContentIds.NUM_PAD, null, numPadItem);
        registry.register(COMPAT_MATERIAL_CUTTING_WIRE, null, cuttingWireItem);
        registry.register(COMPAT_MATERIAL_ACID, null, acidItem);
        registry.register(COMPAT_MATERIAL_CIRCUIT_BOARD_RAW, null, rawCircuitBoardItem);
        registry.register(COMPAT_MATERIAL_CIRCUIT_BOARD, null, circuitBoardItem);
        registry.register(COMPAT_MATERIAL_CIRCUIT_BOARD_PRINTED, null, printedCircuitBoardItem);
        registry.register(COMPAT_MATERIAL_CARD, null, cardItem);
        registry.register(COMPAT_MATERIAL_TRANSISTOR, null, transistorItem);
        registry.register(COMPAT_CIRCUIT_CHIP_TIER1, null, microchipTier1Item);
        registry.register(COMPAT_CIRCUIT_CHIP_TIER2, null, microchipTier2Item);
        registry.register(COMPAT_CIRCUIT_CHIP_TIER3, null, microchipTier3Item);
        registry.register(COMPAT_MATERIAL_ALU, null, aluItem);
        registry.register(COMPAT_MATERIAL_CONTROL_UNIT, null, controlUnitItem);
        registry.register(COMPAT_MATERIAL_DISK, null, diskPlatterItem);
        registry.register(COMPAT_MATERIAL_INTERWEB, null, interwebItem);
        registry.register(COMPAT_MATERIAL_BUTTON_GROUP, null, buttonGroupItem);
        registry.register(COMPAT_MATERIAL_ARROW_KEY, null, arrowKeysItem);
        registry.register(COMPAT_MATERIAL_NUM_PAD, null, numPadItem);
    }

    static void register(
        final ItemRegistry registry,
        final Block adapter,
        final Item adapterItem,
        final Block assembler,
        final Item assemblerItem,
        final Block cable,
        final Item cableItem,
        final Block computerCaseTier1,
        final Item computerCaseTier1Item,
        final Block computerCaseTier2,
        final Item computerCaseTier2Item,
        final Block computerCaseTier3,
        final Item computerCaseTier3Item,
        final Block disassembler,
        final Item disassemblerItem,
        final Block diskDrive,
        final Item diskDriveItem,
        final Block geolyzer,
        final Item geolyzerItem,
        final Block screenTier1,
        final Item screenTier1Item,
        final Block screenTier2,
        final Item screenTier2Item,
        final Block screenTier3,
        final Item screenTier3Item,
        final Block keyboard,
        final Item keyboardItem,
        final Block motionSensor,
        final Item motionSensorItem,
        final Block redstoneIo,
        final Item redstoneIoItem,
        final Block transposer,
        final Item transposerItem,
        final Block hologramTier1,
        final Item hologramTier1Item,
        final Block hologramTier2,
        final Item hologramTier2Item,
        final Block waypoint,
        final Item waypointItem,
        final Item manualItem,
        final Item analyzerItem,
        final Item cpuTier1Item,
        final Item cpuTier2Item,
        final Item cpuTier3Item,
        final Item cardContainerTier1Item,
        final Item cardContainerTier2Item,
        final Item cardContainerTier3Item,
        final Item tabletCaseTier1Item,
        final Item tabletCaseTier2Item,
        final Item tabletCaseCreativeItem,
        final Item tabletItem,
        final Item dataCardTier1Item,
        final Item dataCardTier2Item,
        final Item dataCardTier3Item,
        final Item databaseUpgradeTier1Item,
        final Item databaseUpgradeTier2Item,
        final Item databaseUpgradeTier3Item,
        final Item batteryUpgradeTier1Item,
        final Item batteryUpgradeTier2Item,
        final Item batteryUpgradeTier3Item,
        final Item memoryTier1Item,
        final Item memoryTier2Item,
        final Item memoryTier3Item,
        final Item hddTier1Item,
        final Item hddTier2Item,
        final Item hddTier3Item,
        final Item inventoryControllerUpgradeItem,
        final Item inventoryUpgradeItem,
        final Item internetCardItem,
        final Item linkedCardItem,
        final Item navigationUpgradeItem,
        final Item eepromItem,
        final Item floppyItem,
        final Item graphicsCardTier1Item,
        final Item graphicsCardTier2Item,
        final Item graphicsCardTier3Item,
        final Item hoverUpgradeTier1Item,
        final Item hoverUpgradeTier2Item,
        final Item networkCardItem,
        final Item wirelessNetworkCardTier1Item,
        final Item wirelessNetworkCardTier2Item,
        final Item redstoneCardItem,
        final Item solarGeneratorUpgradeItem,
        final Item tankUpgradeItem,
        final Item tankControllerUpgradeItem,
        final Item upgradeContainerTier1Item,
        final Item upgradeContainerTier2Item,
        final Item upgradeContainerTier3Item) {
        registry.register(ModContentIds.MANUAL, null, manualItem);
        registry.register(ModContentIds.ANALYZER, null, analyzerItem);
        registry.register(ModContentIds.ADAPTER, adapter, adapterItem);
        registry.register(ModContentIds.ASSEMBLER, assembler, assemblerItem);
        registry.register(ModContentIds.CABLE, cable, cableItem);
        registry.register(ModContentIds.COMPUTER_CASE_TIER1, computerCaseTier1, computerCaseTier1Item);
        registry.register(ModContentIds.COMPUTER_CASE_TIER2, computerCaseTier2, computerCaseTier2Item);
        registry.register(ModContentIds.COMPUTER_CASE_TIER3, computerCaseTier3, computerCaseTier3Item);
        registry.register(COMPAT_COMPUTER_CASE_TIER1, computerCaseTier1, computerCaseTier1Item);
        registry.register(COMPAT_COMPUTER_CASE_TIER2, computerCaseTier2, computerCaseTier2Item);
        registry.register(COMPAT_COMPUTER_CASE_TIER3, computerCaseTier3, computerCaseTier3Item);
        registry.register(ModContentIds.DISASSEMBLER, disassembler, disassemblerItem);
        registry.register(ModContentIds.DISK_DRIVE, diskDrive, diskDriveItem);
        registry.register(ModContentIds.GEOLYZER, geolyzer, geolyzerItem);
        registry.register(ModContentIds.SCREEN_TIER1, screenTier1, screenTier1Item);
        registry.register(ModContentIds.SCREEN_TIER2, screenTier2, screenTier2Item);
        registry.register(ModContentIds.SCREEN_TIER3, screenTier3, screenTier3Item);
        registry.register(COMPAT_SCREEN_TIER1, screenTier1, screenTier1Item);
        registry.register(COMPAT_SCREEN_TIER2, screenTier2, screenTier2Item);
        registry.register(COMPAT_SCREEN_TIER3, screenTier3, screenTier3Item);
        registry.register(ModContentIds.KEYBOARD, keyboard, keyboardItem);
        registry.register(ModContentIds.MOTION_SENSOR, motionSensor, motionSensorItem);
        registry.register(ModContentIds.REDSTONE_IO, redstoneIo, redstoneIoItem);
        registry.register(ModContentIds.TRANSPOSER, transposer, transposerItem);
        registry.register(ModContentIds.HOLOGRAM_TIER1, hologramTier1, hologramTier1Item);
        registry.register(ModContentIds.HOLOGRAM_TIER2, hologramTier2, hologramTier2Item);
        registry.register(ModContentIds.WAYPOINT, waypoint, waypointItem);
        registry.register(ModContentIds.CPU_TIER1, null, cpuTier1Item);
        registry.register(ModContentIds.CPU_TIER2, null, cpuTier2Item);
        registry.register(ModContentIds.CPU_TIER3, null, cpuTier3Item);
        registry.register(ModContentIds.CARD_CONTAINER_TIER1, null, cardContainerTier1Item);
        registry.register(ModContentIds.CARD_CONTAINER_TIER2, null, cardContainerTier2Item);
        registry.register(ModContentIds.CARD_CONTAINER_TIER3, null, cardContainerTier3Item);
        registry.register(ModContentIds.TABLET_CASE_TIER1, null, tabletCaseTier1Item);
        registry.register(ModContentIds.TABLET_CASE_TIER2, null, tabletCaseTier2Item);
        registry.register(ModContentIds.TABLET_CASE_CREATIVE, null, tabletCaseCreativeItem);
        registry.register(ModContentIds.TABLET, null, tabletItem);
        registry.register(ModContentIds.DATA_CARD_TIER1, null, dataCardTier1Item);
        registry.register(ModContentIds.DATA_CARD_TIER2, null, dataCardTier2Item);
        registry.register(ModContentIds.DATA_CARD_TIER3, null, dataCardTier3Item);
        registry.register(ModContentIds.DATABASE_UPGRADE_TIER1, null, databaseUpgradeTier1Item);
        registry.register(ModContentIds.DATABASE_UPGRADE_TIER2, null, databaseUpgradeTier2Item);
        registry.register(ModContentIds.DATABASE_UPGRADE_TIER3, null, databaseUpgradeTier3Item);
        registry.register(ModContentIds.BATTERY_UPGRADE_TIER1, null, batteryUpgradeTier1Item);
        registry.register(ModContentIds.BATTERY_UPGRADE_TIER2, null, batteryUpgradeTier2Item);
        registry.register(ModContentIds.BATTERY_UPGRADE_TIER3, null, batteryUpgradeTier3Item);
        registry.register(ModContentIds.MEMORY_TIER1, null, memoryTier1Item);
        registry.register(ModContentIds.MEMORY_TIER2, null, memoryTier2Item);
        registry.register(ModContentIds.MEMORY_TIER3, null, memoryTier3Item);
        registry.register(ModContentIds.HDD_TIER1, null, hddTier1Item);
        registry.register(ModContentIds.HDD_TIER2, null, hddTier2Item);
        registry.register(ModContentIds.HDD_TIER3, null, hddTier3Item);
        registry.register(ModContentIds.INVENTORY_CONTROLLER_UPGRADE, null, inventoryControllerUpgradeItem);
        registry.register(ModContentIds.INVENTORY_UPGRADE, null, inventoryUpgradeItem);
        registry.register(ModContentIds.INTERNET_CARD, null, internetCardItem);
        registry.register(ModContentIds.LINKED_CARD, null, linkedCardItem);
        registry.register(ModContentIds.NAVIGATION_UPGRADE, null, navigationUpgradeItem);
        registry.register(ModContentIds.EEPROM, null, eepromItem);
        registry.register(ModContentIds.FLOPPY, null, floppyItem);
        registry.register(ModContentIds.GRAPHICS_CARD_TIER1, null, graphicsCardTier1Item);
        registry.register(ModContentIds.GRAPHICS_CARD_TIER2, null, graphicsCardTier2Item);
        registry.register(ModContentIds.GRAPHICS_CARD_TIER3, null, graphicsCardTier3Item);
        registry.register(ModContentIds.HOVER_UPGRADE_TIER1, null, hoverUpgradeTier1Item);
        registry.register(ModContentIds.HOVER_UPGRADE_TIER2, null, hoverUpgradeTier2Item);
        registry.register(ModContentIds.NETWORK_CARD, null, networkCardItem);
        registry.register(ModContentIds.WIRELESS_NETWORK_CARD_TIER1, null, wirelessNetworkCardTier1Item);
        registry.register(ModContentIds.WIRELESS_NETWORK_CARD_TIER2, null, wirelessNetworkCardTier2Item);
        registry.register(ModContentIds.REDSTONE_CARD, null, redstoneCardItem);
        registry.register(ModContentIds.SOLAR_GENERATOR_UPGRADE, null, solarGeneratorUpgradeItem);
        registry.register(ModContentIds.TANK_UPGRADE, null, tankUpgradeItem);
        registry.register(ModContentIds.TANK_CONTROLLER_UPGRADE, null, tankControllerUpgradeItem);
        registry.register(ModContentIds.UPGRADE_CONTAINER_TIER1, null, upgradeContainerTier1Item);
        registry.register(ModContentIds.UPGRADE_CONTAINER_TIER2, null, upgradeContainerTier2Item);
        registry.register(ModContentIds.UPGRADE_CONTAINER_TIER3, null, upgradeContainerTier3Item);
        registry.register(COMPAT_CPU_TIER1, null, cpuTier1Item);
        registry.register(COMPAT_CPU_TIER2, null, cpuTier2Item);
        registry.register(COMPAT_CPU_TIER3, null, cpuTier3Item);
        registry.register(COMPAT_CARD_CONTAINER_TIER1, null, cardContainerTier1Item);
        registry.register(COMPAT_CARD_CONTAINER_TIER2, null, cardContainerTier2Item);
        registry.register(COMPAT_CARD_CONTAINER_TIER3, null, cardContainerTier3Item);
        registry.register(COMPAT_TABLET_CASE_TIER1, null, tabletCaseTier1Item);
        registry.register(COMPAT_TABLET_CASE_TIER2, null, tabletCaseTier2Item);
        registry.register(COMPAT_TABLET_CASE_CREATIVE, null, tabletCaseCreativeItem);
        registry.register(COMPAT_DATA_CARD_TIER1, null, dataCardTier1Item);
        registry.register(COMPAT_DATA_CARD_TIER2, null, dataCardTier2Item);
        registry.register(COMPAT_DATA_CARD_TIER3, null, dataCardTier3Item);
        registry.register(COMPAT_DATA_CARD, null, dataCardTier1Item);
        registry.register(COMPAT_DATABASE_UPGRADE_TIER1, null, databaseUpgradeTier1Item);
        registry.register(COMPAT_DATABASE_UPGRADE_TIER2, null, databaseUpgradeTier2Item);
        registry.register(COMPAT_DATABASE_UPGRADE_TIER3, null, databaseUpgradeTier3Item);
        registry.register(COMPAT_BATTERY_UPGRADE_TIER1, null, batteryUpgradeTier1Item);
        registry.register(COMPAT_BATTERY_UPGRADE_TIER2, null, batteryUpgradeTier2Item);
        registry.register(COMPAT_BATTERY_UPGRADE_TIER3, null, batteryUpgradeTier3Item);
        registry.register(COMPAT_MEMORY_TIER1, null, memoryTier1Item);
        registry.register(COMPAT_MEMORY_TIER2, null, memoryTier2Item);
        registry.register(COMPAT_MEMORY_TIER3, null, memoryTier3Item);
        registry.register(COMPAT_HDD_TIER1, null, hddTier1Item);
        registry.register(COMPAT_HDD_TIER2, null, hddTier2Item);
        registry.register(COMPAT_HDD_TIER3, null, hddTier3Item);
        registry.register(COMPAT_INVENTORY_UPGRADE, null, inventoryUpgradeItem);
        registry.register(COMPAT_INTERNET_CARD, null, internetCardItem);
        registry.register(COMPAT_LINKED_CARD, null, linkedCardItem);
        registry.register(COMPAT_NAVIGATION_UPGRADE, null, navigationUpgradeItem);
        registry.register(COMPAT_GRAPHICS_CARD_TIER1, null, graphicsCardTier1Item);
        registry.register(COMPAT_GRAPHICS_CARD_TIER2, null, graphicsCardTier2Item);
        registry.register(COMPAT_GRAPHICS_CARD_TIER3, null, graphicsCardTier3Item);
        registry.register(COMPAT_HOVER_UPGRADE_TIER1, null, hoverUpgradeTier1Item);
        registry.register(COMPAT_HOVER_UPGRADE_TIER2, null, hoverUpgradeTier2Item);
        registry.register(COMPAT_HOLOGRAM_TIER1, hologramTier1, hologramTier1Item);
        registry.register(COMPAT_HOLOGRAM_TIER2, hologramTier2, hologramTier2Item);
        registry.register(COMPAT_NETWORK_CARD, null, networkCardItem);
        registry.register(COMPAT_WIRELESS_NETWORK_CARD_TIER1, null, wirelessNetworkCardTier1Item);
        registry.register(COMPAT_WIRELESS_NETWORK_CARD_TIER2, null, wirelessNetworkCardTier2Item);
        registry.register(COMPAT_WIRELESS_NETWORK_CARD, null, wirelessNetworkCardTier2Item);
        registry.register(COMPAT_REDSTONE_CARD, null, redstoneCardItem);
        registry.register(COMPAT_SOLAR_GENERATOR_UPGRADE, null, solarGeneratorUpgradeItem);
        registry.register(COMPAT_TANK_UPGRADE, null, tankUpgradeItem);
        registry.register(COMPAT_TANK_CONTROLLER_UPGRADE, null, tankControllerUpgradeItem);
        registry.register(COMPAT_UPGRADE_CONTAINER_TIER1, null, upgradeContainerTier1Item);
        registry.register(COMPAT_UPGRADE_CONTAINER_TIER2, null, upgradeContainerTier2Item);
        registry.register(COMPAT_UPGRADE_CONTAINER_TIER3, null, upgradeContainerTier3Item);
    }

    private ModContentCatalog() {
    }
}
