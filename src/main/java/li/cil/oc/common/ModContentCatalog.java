package li.cil.oc.common;

import li.cil.oc.api.API;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModContentCatalog {
    public static final String COMPAT_BATTERY_UPGRADE_TIER1 = "batteryUpgrade1";
    public static final String COMPAT_BATTERY_UPGRADE_TIER2 = "batteryUpgrade2";
    public static final String COMPAT_BATTERY_UPGRADE_TIER3 = "batteryUpgrade3";
    public static final String COMPAT_BATTERY_UPGRADE_TIER1_UPSTREAM = "batteryupgrade1";
    public static final String COMPAT_BATTERY_UPGRADE_TIER2_UPSTREAM = "batteryupgrade2";
    public static final String COMPAT_BATTERY_UPGRADE_TIER3_UPSTREAM = "batteryupgrade3";
    public static final String COMPAT_BARCODE_READER_UPGRADE = "barcodeReader";
    public static final String COMPAT_ANGEL_UPGRADE = "angelUpgrade";
    public static final String COMPAT_ANGEL_UPGRADE_UPSTREAM = "angelupgrade";
    public static final String COMPAT_APU_TIER1 = "apu1";
    public static final String COMPAT_APU_TIER2 = "apu2";
    public static final String COMPAT_CHUNKLOADER_UPGRADE = "chunkloaderUpgrade";
    public static final String COMPAT_CHUNKLOADER_UPGRADE_UPSTREAM = "chunkloaderupgrade";
    public static final String COMPAT_CIRCUIT_CHIP_TIER1 = "circuitChip1";
    public static final String COMPAT_CIRCUIT_CHIP_TIER2 = "circuitChip2";
    public static final String COMPAT_CIRCUIT_CHIP_TIER3 = "circuitChip3";
    public static final String COMPAT_CIRCUIT_CHIP_TIER1_UPSTREAM = "chip1";
    public static final String COMPAT_CIRCUIT_CHIP_TIER2_UPSTREAM = "chip2";
    public static final String COMPAT_CIRCUIT_CHIP_TIER3_UPSTREAM = "chip3";
    public static final String COMPAT_DIAMOND_CHIP = "chipDiamond";
    public static final String COMPAT_DIAMOND_CHIP_UPSTREAM = "chipdiamond";
    public static final String COMPAT_COMPONENT_BUS_TIER1 = "componentBus1";
    public static final String COMPAT_COMPONENT_BUS_TIER2 = "componentBus2";
    public static final String COMPAT_COMPONENT_BUS_TIER3 = "componentBus3";
    public static final String COMPAT_COMPONENT_BUS_TIER1_UPSTREAM = "componentbus1";
    public static final String COMPAT_COMPONENT_BUS_TIER2_UPSTREAM = "componentbus2";
    public static final String COMPAT_COMPONENT_BUS_TIER3_UPSTREAM = "componentbus3";
    public static final String COMPAT_COMPONENT_BUS_CREATIVE = "componentbuscreative";
    public static final String COMPAT_CARD_CONTAINER_TIER1 = "cardcontainer1";
    public static final String COMPAT_CARD_CONTAINER_TIER2 = "cardcontainer2";
    public static final String COMPAT_CARD_CONTAINER_TIER3 = "cardcontainer3";
    public static final String COMPAT_MICROCONTROLLER_CASE_TIER1 = "microcontrollerCase1";
    public static final String COMPAT_MICROCONTROLLER_CASE_TIER2 = "microcontrollerCase2";
    public static final String COMPAT_MICROCONTROLLER_CASE_TIER1_UPSTREAM = "microcontrollercase1";
    public static final String COMPAT_MICROCONTROLLER_CASE_TIER2_UPSTREAM = "microcontrollercase2";
    public static final String COMPAT_MICROCONTROLLER_CASE_CREATIVE_UPSTREAM = "microcontrollercasecreative";
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
    public static final String COMPAT_DEBUG_CARD = "debugCard";
    public static final String COMPAT_DEBUG_CARD_UPSTREAM = "debugcard";
    public static final String COMPAT_DISK_DRIVE_MOUNTABLE = "diskDriveMountable";
    public static final String COMPAT_DISK_DRIVE_MOUNTABLE_UPSTREAM = "diskdrivemountable";
    public static final String COMPAT_DISK_DRIVE_UPSTREAM = "diskdrive";
    public static final String COMPAT_DATABASE_UPGRADE_TIER1 = "databaseUpgrade1";
    public static final String COMPAT_DATABASE_UPGRADE_TIER2 = "databaseUpgrade2";
    public static final String COMPAT_DATABASE_UPGRADE_TIER3 = "databaseUpgrade3";
    public static final String COMPAT_DATABASE_UPGRADE_TIER1_UPSTREAM = "databaseupgrade1";
    public static final String COMPAT_DATABASE_UPGRADE_TIER2_UPSTREAM = "databaseupgrade2";
    public static final String COMPAT_DATABASE_UPGRADE_TIER3_UPSTREAM = "databaseupgrade3";
    public static final String COMPAT_GRAPHICS_CARD_TIER1 = "graphicscard1";
    public static final String COMPAT_GRAPHICS_CARD_TIER2 = "graphicscard2";
    public static final String COMPAT_GRAPHICS_CARD_TIER3 = "graphicscard3";
    public static final String COMPAT_GENERATOR_UPGRADE = "generatorUpgrade";
    public static final String COMPAT_GENERATOR_UPGRADE_UPSTREAM = "generatorupgrade";
    public static final String COMPAT_HOVER_UPGRADE_TIER1 = "hoverUpgrade1";
    public static final String COMPAT_HOVER_UPGRADE_TIER2 = "hoverUpgrade2";
    public static final String COMPAT_HOVER_UPGRADE_TIER1_UPSTREAM = "hoverupgrade1";
    public static final String COMPAT_HOVER_UPGRADE_TIER2_UPSTREAM = "hoverupgrade2";
    public static final String COMPAT_HOLOGRAM_TIER1 = "hologram1";
    public static final String COMPAT_HOLOGRAM_TIER2 = "hologram2";
    public static final String COMPAT_HDD_TIER1 = "hdd1";
    public static final String COMPAT_HDD_TIER2 = "hdd2";
    public static final String COMPAT_HDD_TIER3 = "hdd3";
    public static final String COMPAT_CRAFTING_UPGRADE = "craftingUpgrade";
    public static final String COMPAT_EXPERIENCE_UPGRADE = "experienceUpgrade";
    public static final String COMPAT_INVENTORY_UPGRADE = "inventoryUpgrade";
    public static final String COMPAT_INVENTORY_CONTROLLER_UPGRADE_UPSTREAM = "inventorycontrollerupgrade";
    public static final String COMPAT_CRAFTING_UPGRADE_UPSTREAM = "craftingupgrade";
    public static final String COMPAT_EXPERIENCE_UPGRADE_UPSTREAM = "experienceupgrade";
    public static final String COMPAT_INVENTORY_UPGRADE_UPSTREAM = "inventoryupgrade";
    public static final String COMPAT_INTERNET_CARD = "internetcard";
    public static final String COMPAT_LEASH_UPGRADE = "leashUpgrade";
    public static final String COMPAT_LEASH_UPGRADE_UPSTREAM = "leashupgrade";
    public static final String COMPAT_LINKED_CARD = "linkedcard";
    public static final String COMPAT_MEMORY_TIER1 = "ram1";
    public static final String COMPAT_MEMORY_TIER2 = "ram2";
    public static final String COMPAT_MEMORY_TIER3 = "ram3";
    public static final String COMPAT_MEMORY_TIER4 = "ram4";
    public static final String COMPAT_MEMORY_TIER5 = "ram5";
    public static final String COMPAT_MEMORY_TIER6 = "ram6";
    public static final String COMPAT_MFU = "mfu";
    public static final String COMPAT_NANOMACHINES = "nanomachines";
    public static final String COMPAT_NAVIGATION_UPGRADE = "navigationUpgrade";
    public static final String COMPAT_NAVIGATION_UPGRADE_UPSTREAM = "navigationupgrade";
    public static final String COMPAT_NETWORK_CARD = "lancard";
    public static final String COMPAT_MOTION_SENSOR_UPSTREAM = "motionsensor";
    public static final String COMPAT_PISTON_UPGRADE = "pistonUpgrade";
    public static final String COMPAT_POWER_CONVERTER = "powerConverter";
    public static final String COMPAT_POWER_DISTRIBUTOR = "powerDistributor";
    public static final String COMPAT_POWER_CONVERTER_UPSTREAM = "powerconverter";
    public static final String COMPAT_POWER_DISTRIBUTOR_UPSTREAM = "powerdistributor";
    public static final String COMPAT_RACK = "rack";
    public static final String COMPAT_RAID = "raid";
    public static final String COMPAT_REDSTONE_CARD = "redstone";
    public static final String COMPAT_REDSTONE_CARD_TIER1_UPSTREAM = "redstonecard1";
    public static final String COMPAT_RELAY = "relay";
    public static final String COMPAT_NET_SPLITTER = "netSplitter";
    public static final String COMPAT_NET_SPLITTER_UPSTREAM = "netsplitter";
    public static final String COMPAT_SERVER_TIER1 = "server1";
    public static final String COMPAT_SERVER_TIER2 = "server2";
    public static final String COMPAT_SERVER_TIER3 = "server3";
    public static final String COMPAT_SIGN_UPGRADE = "signUpgrade";
    public static final String COMPAT_SOLAR_GENERATOR_UPGRADE = "solarGeneratorUpgrade";
    public static final String COMPAT_STICKY_PISTON_UPGRADE = "stickyPistonUpgrade";
    public static final String COMPAT_TANK_CONTROLLER_UPGRADE = "tankControllerUpgrade";
    public static final String COMPAT_TANK_UPGRADE = "tankUpgrade";
    public static final String COMPAT_PISTON_UPGRADE_UPSTREAM = "pistonupgrade";
    public static final String COMPAT_SIGN_UPGRADE_UPSTREAM = "signupgrade";
    public static final String COMPAT_SOLAR_GENERATOR_UPGRADE_UPSTREAM = "solargeneratorupgrade";
    public static final String COMPAT_STICKY_PISTON_UPGRADE_UPSTREAM = "stickypistonupgrade";
    public static final String COMPAT_TANK_CONTROLLER_UPGRADE_UPSTREAM = "tankcontrollerupgrade";
    public static final String COMPAT_TANK_UPGRADE_UPSTREAM = "tankupgrade";
    public static final String COMPAT_TERMINAL = "terminal";
    public static final String COMPAT_TERMINAL_SERVER = "terminalServer";
    public static final String COMPAT_TERMINAL_SERVER_UPSTREAM = "terminalserver";
    public static final String COMPAT_TEXTURE_PICKER = "texturePicker";
    public static final String COMPAT_TEXTURE_PICKER_UPSTREAM = "texturepicker";
    public static final String COMPAT_TRADING_UPGRADE = "tradingUpgrade";
    public static final String COMPAT_TRACTOR_BEAM_UPGRADE = "tractorBeamUpgrade";
    public static final String COMPAT_TRADING_UPGRADE_UPSTREAM = "tradingupgrade";
    public static final String COMPAT_TRACTOR_BEAM_UPGRADE_UPSTREAM = "tractorbeamupgrade";
    public static final String COMPAT_UPGRADE_CONTAINER_TIER1 = "upgradecontainer1";
    public static final String COMPAT_UPGRADE_CONTAINER_TIER2 = "upgradecontainer2";
    public static final String COMPAT_UPGRADE_CONTAINER_TIER3 = "upgradecontainer3";
    public static final String COMPAT_WIRELESS_NETWORK_CARD_TIER1 = "wlancard1";
    public static final String COMPAT_WIRELESS_NETWORK_CARD_TIER2 = "wlancard2";
    public static final String COMPAT_WIRELESS_NETWORK_CARD = "wlancard";
    public static final String COMPAT_WRENCH = "wrench";
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
    public static final String COMPAT_RAW_CIRCUIT_BOARD = "rawcircuitboard";
    public static final String COMPAT_CIRCUIT_BOARD = "circuitboard";
    public static final String COMPAT_PRINTED_CIRCUIT_BOARD = "printedcircuitboard";
    public static final String COMPAT_MATERIAL_CONTROL_UNIT = "materialCU";
    public static final String COMPAT_MATERIAL_CUTTING_WIRE = "materialCuttingWire";
    public static final String COMPAT_MATERIAL_DISK = "materialDisk";
    public static final String COMPAT_MATERIAL_INTERWEB = "materialInterweb";
    public static final String COMPAT_CUTTING_WIRE = "cuttingwire";
    public static final String COMPAT_CONTROL_UNIT = "cu";
    public static final String COMPAT_DISK_PLATTER = "disk";
    public static final String COMPAT_BUTTON_GROUP = "buttongroup";
    public static final String COMPAT_ARROW_KEYS = "arrowkeys";
    public static final String COMPAT_NUM_PAD = "numpad";
    public static final String COMPAT_INK_CARTRIDGE_EMPTY = "inkCartridgeEmpty";
    public static final String COMPAT_INK_CARTRIDGE = "inkCartridge";
    public static final String COMPAT_INK_CARTRIDGE_EMPTY_UPSTREAM = "inkcartridgeempty";
    public static final String COMPAT_INK_CARTRIDGE_UPSTREAM = "inkcartridge";
    public static final String COMPAT_MATERIAL_NUM_PAD = "materialNumPad";
    public static final String COMPAT_MATERIAL_TRANSISTOR = "materialTransistor";
    public static final String COMPAT_CAPACITOR = "capacitor";
    public static final String COMPAT_CHAMELIUM = "chamelium";
    public static final String COMPAT_CHAMELIUM_BLOCK = "chameliumBlock";
    public static final String COMPAT_CHAMELIUM_BLOCK_UPSTREAM = "chameliumblock";
    public static final String COMPAT_STONE_ENDSTONE = "stoneEndstone";
    public static final String COMPAT_STONE_ENDSTONE_UPSTREAM = "stoneendstone";

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
                ModBlocks.CHAMELIUM_BLOCK.get(),
                ModItems.CHAMELIUM_BLOCK.get(),
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
                ModItems.DISK_DRIVE_MOUNTABLE.get(),
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
                ModBlocks.POWER_DISTRIBUTOR.get(),
                ModItems.POWER_DISTRIBUTOR.get(),
                ModBlocks.POWER_CONVERTER.get(),
                ModItems.POWER_CONVERTER.get(),
                ModBlocks.RACK.get(),
                ModItems.RACK.get(),
                ModBlocks.RAID.get(),
                ModItems.RAID.get(),
                ModBlocks.REDSTONE_IO.get(),
                ModItems.REDSTONE_IO.get(),
                ModBlocks.RELAY.get(),
                ModItems.RELAY.get(),
                ModBlocks.NET_SPLITTER.get(),
                ModItems.NET_SPLITTER.get(),
                ModBlocks.TRANSPOSER.get(),
                ModItems.TRANSPOSER.get(),
                ModBlocks.HOLOGRAM_TIER1.get(),
                ModItems.HOLOGRAM_TIER1.get(),
                ModBlocks.HOLOGRAM_TIER2.get(),
                ModItems.HOLOGRAM_TIER2.get(),
                ModBlocks.WAYPOINT.get(),
                ModItems.WAYPOINT.get(),
                ModBlocks.PRINT.get(),
                ModItems.PRINT.get(),
                ModBlocks.PRINTER.get(),
                ModItems.PRINTER.get(),
                ModItems.MANUAL.get(),
                ModItems.ANALYZER.get(),
                ModItems.WRENCH.get(),
                ModItems.TEXTURE_PICKER.get(),
                ModItems.TERMINAL.get(),
                ModItems.TERMINAL_SERVER.get(),
                ModItems.NANOMACHINES.get(),
                ModItems.SERVER_TIER1.get(),
                ModItems.SERVER_TIER2.get(),
                ModItems.SERVER_TIER3.get(),
                ModItems.APU_TIER1.get(),
                ModItems.APU_TIER2.get(),
                ModItems.CPU_TIER1.get(),
                ModItems.CPU_TIER2.get(),
                ModItems.CPU_TIER3.get(),
                ModItems.CARD_CONTAINER_TIER1.get(),
                ModItems.CARD_CONTAINER_TIER2.get(),
                ModItems.CARD_CONTAINER_TIER3.get(),
                ModItems.MICROCONTROLLER_CASE_TIER1.get(),
                ModItems.MICROCONTROLLER_CASE_TIER2.get(),
                ModItems.MICROCONTROLLER_CASE_CREATIVE.get(),
                ModItems.TABLET_CASE_TIER1.get(),
                ModItems.TABLET_CASE_TIER2.get(),
                ModItems.TABLET_CASE_CREATIVE.get(),
                ModItems.TABLET.get(),
                ModItems.DATA_CARD_TIER1.get(),
                ModItems.DATA_CARD_TIER2.get(),
                ModItems.DATA_CARD_TIER3.get(),
                ModItems.DEBUG_CARD.get(),
                ModItems.DATABASE_UPGRADE_TIER1.get(),
                ModItems.DATABASE_UPGRADE_TIER2.get(),
                ModItems.DATABASE_UPGRADE_TIER3.get(),
                ModItems.BATTERY_UPGRADE_TIER1.get(),
                ModItems.BATTERY_UPGRADE_TIER2.get(),
                ModItems.BATTERY_UPGRADE_TIER3.get(),
                ModItems.BARCODE_READER_UPGRADE.get(),
                ModItems.MEMORY_TIER1.get(),
                ModItems.MEMORY_TIER2.get(),
                ModItems.MEMORY_TIER3.get(),
                ModItems.MEMORY_TIER4.get(),
                ModItems.MEMORY_TIER5.get(),
                ModItems.MEMORY_TIER6.get(),
                ModItems.HDD_TIER1.get(),
                ModItems.HDD_TIER2.get(),
                ModItems.HDD_TIER3.get(),
                ModItems.INVENTORY_CONTROLLER_UPGRADE.get(),
                ModItems.INVENTORY_UPGRADE.get(),
                ModItems.CRAFTING_UPGRADE.get(),
                ModItems.INTERNET_CARD.get(),
                ModItems.LINKED_CARD.get(),
                ModItems.NAVIGATION_UPGRADE.get(),
                ModItems.EEPROM.get(),
                ModItems.EXPERIENCE_UPGRADE.get(),
                ModItems.FLOPPY.get(),
                ModItems.GRAPHICS_CARD_TIER1.get(),
                ModItems.GRAPHICS_CARD_TIER2.get(),
                ModItems.GRAPHICS_CARD_TIER3.get(),
                ModItems.HOVER_UPGRADE_TIER1.get(),
                ModItems.HOVER_UPGRADE_TIER2.get(),
                ModItems.NETWORK_CARD.get(),
                ModItems.PISTON_UPGRADE.get(),
                ModItems.STICKY_PISTON_UPGRADE.get(),
                ModItems.SIGN_UPGRADE.get(),
                ModItems.TRADING_UPGRADE.get(),
                ModItems.TRACTOR_BEAM_UPGRADE.get(),
                ModItems.LEASH_UPGRADE.get(),
                ModItems.ANGEL_UPGRADE.get(),
                ModItems.CHUNKLOADER_UPGRADE.get(),
                ModItems.MFU.get(),
                ModItems.WIRELESS_NETWORK_CARD_TIER1.get(),
                ModItems.WIRELESS_NETWORK_CARD_TIER2.get(),
                ModItems.REDSTONE_CARD.get(),
                ModItems.SOLAR_GENERATOR_UPGRADE.get(),
                ModItems.TANK_UPGRADE.get(),
                ModItems.TANK_CONTROLLER_UPGRADE.get(),
                ModItems.UPGRADE_CONTAINER_TIER1.get(),
                ModItems.UPGRADE_CONTAINER_TIER2.get(),
                ModItems.UPGRADE_CONTAINER_TIER3.get(),
                ModItems.GENERATOR_UPGRADE.get());
            registerMaterialItems(
                registry,
                ModItems.CUTTING_WIRE.get(),
                ModItems.ACID.get(),
                ModItems.RAW_CIRCUIT_BOARD.get(),
                ModItems.CIRCUIT_BOARD.get(),
                ModItems.PRINTED_CIRCUIT_BOARD.get(),
                ModItems.CARD.get(),
                ModItems.CHAMELIUM.get(),
                ModItems.TRANSISTOR.get(),
                ModItems.CAPACITOR.get(),
                ModItems.COMPONENT_BUS_TIER1.get(),
                ModItems.COMPONENT_BUS_TIER2.get(),
                ModItems.COMPONENT_BUS_TIER3.get(),
                ModItems.COMPONENT_BUS_CREATIVE.get(),
                ModItems.MICROCHIP_TIER1.get(),
                ModItems.MICROCHIP_TIER2.get(),
                ModItems.MICROCHIP_TIER3.get(),
                ModItems.ALU.get(),
                ModItems.CONTROL_UNIT.get(),
                ModItems.DISK_PLATTER.get(),
                ModItems.INTERWEB.get(),
                ModItems.INK_CARTRIDGE_EMPTY.get(),
                ModItems.INK_CARTRIDGE.get(),
                ModItems.BUTTON_GROUP.get(),
                ModItems.ARROW_KEYS.get(),
                ModItems.NUM_PAD.get());
            registerDiamondChip(registry, ModItems.DIAMOND_CHIP.get());
            registerEndstoneBlock(registry, ModBlocks.ENDSTONE.get(), ModItems.ENDSTONE.get());
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
        final Item chameliumItem,
        final Item transistorItem,
        final Item capacitorItem,
        final Item componentBusTier1Item,
        final Item componentBusTier2Item,
        final Item componentBusTier3Item,
        final Item componentBusCreativeItem,
        final Item microchipTier1Item,
        final Item microchipTier2Item,
        final Item microchipTier3Item,
        final Item aluItem,
        final Item controlUnitItem,
        final Item diskPlatterItem,
        final Item interwebItem,
        final Item inkCartridgeEmptyItem,
        final Item inkCartridgeItem,
        final Item buttonGroupItem,
        final Item arrowKeysItem,
        final Item numPadItem) {
        registry.register(ModContentIds.CUTTING_WIRE, null, cuttingWireItem);
        registry.register(ModContentIds.ACID, null, acidItem);
        registry.register(ModContentIds.RAW_CIRCUIT_BOARD, null, rawCircuitBoardItem);
        registry.register(ModContentIds.CIRCUIT_BOARD, null, circuitBoardItem);
        registry.register(ModContentIds.PRINTED_CIRCUIT_BOARD, null, printedCircuitBoardItem);
        registry.register(ModContentIds.CARD, null, cardItem);
        registry.register(ModContentIds.CHAMELIUM, null, chameliumItem);
        registry.register(ModContentIds.TRANSISTOR, null, transistorItem);
        registry.register(ModContentIds.CAPACITOR, null, capacitorItem);
        registry.register(ModContentIds.COMPONENT_BUS_TIER1, null, componentBusTier1Item);
        registry.register(ModContentIds.COMPONENT_BUS_TIER2, null, componentBusTier2Item);
        registry.register(ModContentIds.COMPONENT_BUS_TIER3, null, componentBusTier3Item);
        registry.register(ModContentIds.COMPONENT_BUS_CREATIVE, null, componentBusCreativeItem);
        registry.register(ModContentIds.MICROCHIP_TIER1, null, microchipTier1Item);
        registry.register(ModContentIds.MICROCHIP_TIER2, null, microchipTier2Item);
        registry.register(ModContentIds.MICROCHIP_TIER3, null, microchipTier3Item);
        registry.register(ModContentIds.ALU, null, aluItem);
        registry.register(ModContentIds.CONTROL_UNIT, null, controlUnitItem);
        registry.register(ModContentIds.DISK_PLATTER, null, diskPlatterItem);
        registry.register(ModContentIds.INTERWEB, null, interwebItem);
        registry.register(ModContentIds.INK_CARTRIDGE_EMPTY, null, inkCartridgeEmptyItem);
        registry.register(ModContentIds.INK_CARTRIDGE, null, inkCartridgeItem);
        registry.register(ModContentIds.BUTTON_GROUP, null, buttonGroupItem);
        registry.register(ModContentIds.ARROW_KEYS, null, arrowKeysItem);
        registry.register(ModContentIds.NUM_PAD, null, numPadItem);
        registry.register(COMPAT_MATERIAL_CUTTING_WIRE, null, cuttingWireItem);
        registry.register(COMPAT_MATERIAL_ACID, null, acidItem);
        registry.register(COMPAT_MATERIAL_CIRCUIT_BOARD_RAW, null, rawCircuitBoardItem);
        registry.register(COMPAT_MATERIAL_CIRCUIT_BOARD, null, circuitBoardItem);
        registry.register(COMPAT_MATERIAL_CIRCUIT_BOARD_PRINTED, null, printedCircuitBoardItem);
        registry.register(COMPAT_RAW_CIRCUIT_BOARD, null, rawCircuitBoardItem);
        registry.register(COMPAT_CIRCUIT_BOARD, null, circuitBoardItem);
        registry.register(COMPAT_PRINTED_CIRCUIT_BOARD, null, printedCircuitBoardItem);
        registry.register(COMPAT_MATERIAL_CARD, null, cardItem);
        registry.register(COMPAT_CHAMELIUM, null, chameliumItem);
        registry.register(COMPAT_MATERIAL_TRANSISTOR, null, transistorItem);
        registry.register(COMPAT_CAPACITOR, null, capacitorItem);
        registry.register(COMPAT_COMPONENT_BUS_TIER1, null, componentBusTier1Item);
        registry.register(COMPAT_COMPONENT_BUS_TIER2, null, componentBusTier2Item);
        registry.register(COMPAT_COMPONENT_BUS_TIER3, null, componentBusTier3Item);
        registry.register(COMPAT_COMPONENT_BUS_TIER1_UPSTREAM, null, componentBusTier1Item);
        registry.register(COMPAT_COMPONENT_BUS_TIER2_UPSTREAM, null, componentBusTier2Item);
        registry.register(COMPAT_COMPONENT_BUS_TIER3_UPSTREAM, null, componentBusTier3Item);
        registry.register(COMPAT_COMPONENT_BUS_CREATIVE, null, componentBusCreativeItem);
        registry.register(COMPAT_CIRCUIT_CHIP_TIER1, null, microchipTier1Item);
        registry.register(COMPAT_CIRCUIT_CHIP_TIER2, null, microchipTier2Item);
        registry.register(COMPAT_CIRCUIT_CHIP_TIER3, null, microchipTier3Item);
        registry.register(COMPAT_CIRCUIT_CHIP_TIER1_UPSTREAM, null, microchipTier1Item);
        registry.register(COMPAT_CIRCUIT_CHIP_TIER2_UPSTREAM, null, microchipTier2Item);
        registry.register(COMPAT_CIRCUIT_CHIP_TIER3_UPSTREAM, null, microchipTier3Item);
        registry.register(COMPAT_MATERIAL_ALU, null, aluItem);
        registry.register(COMPAT_MATERIAL_CONTROL_UNIT, null, controlUnitItem);
        registry.register(COMPAT_MATERIAL_DISK, null, diskPlatterItem);
        registry.register(COMPAT_MATERIAL_INTERWEB, null, interwebItem);
        registry.register(COMPAT_INK_CARTRIDGE_EMPTY, null, inkCartridgeEmptyItem);
        registry.register(COMPAT_INK_CARTRIDGE, null, inkCartridgeItem);
        registry.register(COMPAT_INK_CARTRIDGE_EMPTY_UPSTREAM, null, inkCartridgeEmptyItem);
        registry.register(COMPAT_INK_CARTRIDGE_UPSTREAM, null, inkCartridgeItem);
        registry.register(COMPAT_MATERIAL_BUTTON_GROUP, null, buttonGroupItem);
        registry.register(COMPAT_MATERIAL_ARROW_KEY, null, arrowKeysItem);
        registry.register(COMPAT_MATERIAL_NUM_PAD, null, numPadItem);
        registry.register(COMPAT_CUTTING_WIRE, null, cuttingWireItem);
        registry.register(COMPAT_CONTROL_UNIT, null, controlUnitItem);
        registry.register(COMPAT_DISK_PLATTER, null, diskPlatterItem);
        registry.register(COMPAT_BUTTON_GROUP, null, buttonGroupItem);
        registry.register(COMPAT_ARROW_KEYS, null, arrowKeysItem);
        registry.register(COMPAT_NUM_PAD, null, numPadItem);
    }

    static void registerChameliumBlock(final ItemRegistry registry, final Block chameliumBlock, final Item chameliumBlockItem) {
        registry.register(ModContentIds.CHAMELIUM_BLOCK, chameliumBlock, chameliumBlockItem);
        registry.register(COMPAT_CHAMELIUM_BLOCK, chameliumBlock, chameliumBlockItem);
        registry.register(COMPAT_CHAMELIUM_BLOCK_UPSTREAM, chameliumBlock, chameliumBlockItem);
    }

    static void registerEndstoneBlock(final ItemRegistry registry, final Block endstoneBlock, final Item endstoneItem) {
        registry.register(ModContentIds.ENDSTONE, endstoneBlock, endstoneItem);
        registry.register(COMPAT_STONE_ENDSTONE, endstoneBlock, endstoneItem);
        registry.register(COMPAT_STONE_ENDSTONE_UPSTREAM, endstoneBlock, endstoneItem);
    }

    static void registerDiamondChip(final ItemRegistry registry, final Item diamondChipItem) {
        registry.register(ModContentIds.DIAMOND_CHIP, null, diamondChipItem);
        registry.register(COMPAT_DIAMOND_CHIP, null, diamondChipItem);
        registry.register(COMPAT_DIAMOND_CHIP_UPSTREAM, null, diamondChipItem);
    }

    static void register(
        final ItemRegistry registry,
        final Block adapter,
        final Item adapterItem,
        final Block assembler,
        final Item assemblerItem,
        final Block cable,
        final Item cableItem,
        final Block chameliumBlock,
        final Item chameliumBlockItem,
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
        final Item diskDriveMountableItem,
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
        final Block powerDistributor,
        final Item powerDistributorItem,
        final Block powerConverter,
        final Item powerConverterItem,
        final Block rack,
        final Item rackItem,
        final Block raid,
        final Item raidItem,
        final Block redstoneIo,
        final Item redstoneIoItem,
        final Block relay,
        final Item relayItem,
        final Block netSplitter,
        final Item netSplitterItem,
        final Block transposer,
        final Item transposerItem,
        final Block hologramTier1,
        final Item hologramTier1Item,
        final Block hologramTier2,
        final Item hologramTier2Item,
        final Block waypoint,
        final Item waypointItem,
        final Block print,
        final Item printItem,
        final Block printer,
        final Item printerItem,
        final Item manualItem,
        final Item analyzerItem,
        final Item wrenchItem,
        final Item texturePickerItem,
        final Item terminalItem,
        final Item terminalServerItem,
        final Item nanomachinesItem,
        final Item serverTier1Item,
        final Item serverTier2Item,
        final Item serverTier3Item,
        final Item apuTier1Item,
        final Item apuTier2Item,
        final Item cpuTier1Item,
        final Item cpuTier2Item,
        final Item cpuTier3Item,
        final Item cardContainerTier1Item,
        final Item cardContainerTier2Item,
        final Item cardContainerTier3Item,
        final Item microcontrollerCaseTier1Item,
        final Item microcontrollerCaseTier2Item,
        final Item microcontrollerCaseCreativeItem,
        final Item tabletCaseTier1Item,
        final Item tabletCaseTier2Item,
        final Item tabletCaseCreativeItem,
        final Item tabletItem,
        final Item dataCardTier1Item,
        final Item dataCardTier2Item,
        final Item dataCardTier3Item,
        final Item debugCardItem,
        final Item databaseUpgradeTier1Item,
        final Item databaseUpgradeTier2Item,
        final Item databaseUpgradeTier3Item,
        final Item batteryUpgradeTier1Item,
        final Item batteryUpgradeTier2Item,
        final Item batteryUpgradeTier3Item,
        final Item barcodeReaderUpgradeItem,
        final Item memoryTier1Item,
        final Item memoryTier2Item,
        final Item memoryTier3Item,
        final Item memoryTier4Item,
        final Item memoryTier5Item,
        final Item memoryTier6Item,
        final Item hddTier1Item,
        final Item hddTier2Item,
        final Item hddTier3Item,
        final Item inventoryControllerUpgradeItem,
        final Item inventoryUpgradeItem,
        final Item craftingUpgradeItem,
        final Item internetCardItem,
        final Item linkedCardItem,
        final Item navigationUpgradeItem,
        final Item eepromItem,
        final Item experienceUpgradeItem,
        final Item floppyItem,
        final Item graphicsCardTier1Item,
        final Item graphicsCardTier2Item,
        final Item graphicsCardTier3Item,
        final Item hoverUpgradeTier1Item,
        final Item hoverUpgradeTier2Item,
        final Item networkCardItem,
        final Item pistonUpgradeItem,
        final Item stickyPistonUpgradeItem,
        final Item signUpgradeItem,
        final Item tradingUpgradeItem,
        final Item tractorBeamUpgradeItem,
        final Item leashUpgradeItem,
        final Item angelUpgradeItem,
        final Item chunkloaderUpgradeItem,
        final Item mfuItem,
        final Item wirelessNetworkCardTier1Item,
        final Item wirelessNetworkCardTier2Item,
        final Item redstoneCardItem,
        final Item solarGeneratorUpgradeItem,
        final Item tankUpgradeItem,
        final Item tankControllerUpgradeItem,
        final Item upgradeContainerTier1Item,
        final Item upgradeContainerTier2Item,
        final Item upgradeContainerTier3Item,
        final Item generatorUpgradeItem) {
        registry.register(ModContentIds.MANUAL, null, manualItem);
        registry.register(ModContentIds.ANALYZER, null, analyzerItem);
        registry.register(ModContentIds.WRENCH, null, wrenchItem);
        registry.register(COMPAT_WRENCH, null, wrenchItem);
        registry.register(ModContentIds.TEXTURE_PICKER, null, texturePickerItem);
        registry.register(COMPAT_TEXTURE_PICKER, null, texturePickerItem);
        registry.register(COMPAT_TEXTURE_PICKER_UPSTREAM, null, texturePickerItem);
        registry.register(ModContentIds.TERMINAL, null, terminalItem);
        registry.register(COMPAT_TERMINAL, null, terminalItem);
        registry.register(ModContentIds.TERMINAL_SERVER, null, terminalServerItem);
        registry.register(COMPAT_TERMINAL_SERVER, null, terminalServerItem);
        registry.register(COMPAT_TERMINAL_SERVER_UPSTREAM, null, terminalServerItem);
        registry.register(ModContentIds.NANOMACHINES, null, nanomachinesItem);
        registry.register(COMPAT_NANOMACHINES, null, nanomachinesItem);
        registry.register(ModContentIds.SERVER_TIER1, null, serverTier1Item);
        registry.register(ModContentIds.SERVER_TIER2, null, serverTier2Item);
        registry.register(ModContentIds.SERVER_TIER3, null, serverTier3Item);
        registry.register(COMPAT_SERVER_TIER1, null, serverTier1Item);
        registry.register(COMPAT_SERVER_TIER2, null, serverTier2Item);
        registry.register(COMPAT_SERVER_TIER3, null, serverTier3Item);
        registry.register(ModContentIds.APU_TIER1, null, apuTier1Item);
        registry.register(ModContentIds.APU_TIER2, null, apuTier2Item);
        registry.register(COMPAT_APU_TIER1, null, apuTier1Item);
        registry.register(COMPAT_APU_TIER2, null, apuTier2Item);
        registry.register(ModContentIds.ADAPTER, adapter, adapterItem);
        registry.register(ModContentIds.ASSEMBLER, assembler, assemblerItem);
        registry.register(ModContentIds.CABLE, cable, cableItem);
        registerChameliumBlock(registry, chameliumBlock, chameliumBlockItem);
        registry.register(ModContentIds.COMPUTER_CASE_TIER1, computerCaseTier1, computerCaseTier1Item);
        registry.register(ModContentIds.COMPUTER_CASE_TIER2, computerCaseTier2, computerCaseTier2Item);
        registry.register(ModContentIds.COMPUTER_CASE_TIER3, computerCaseTier3, computerCaseTier3Item);
        registry.register(COMPAT_COMPUTER_CASE_TIER1, computerCaseTier1, computerCaseTier1Item);
        registry.register(COMPAT_COMPUTER_CASE_TIER2, computerCaseTier2, computerCaseTier2Item);
        registry.register(COMPAT_COMPUTER_CASE_TIER3, computerCaseTier3, computerCaseTier3Item);
        registry.register(ModContentIds.DISASSEMBLER, disassembler, disassemblerItem);
        registry.register(ModContentIds.DISK_DRIVE, diskDrive, diskDriveItem);
        registry.register(COMPAT_DISK_DRIVE_UPSTREAM, diskDrive, diskDriveItem);
        registry.register(ModContentIds.DISK_DRIVE_MOUNTABLE, null, diskDriveMountableItem);
        registry.register(COMPAT_DISK_DRIVE_MOUNTABLE, null, diskDriveMountableItem);
        registry.register(COMPAT_DISK_DRIVE_MOUNTABLE_UPSTREAM, null, diskDriveMountableItem);
        registry.register(ModContentIds.GEOLYZER, geolyzer, geolyzerItem);
        registry.register(ModContentIds.SCREEN_TIER1, screenTier1, screenTier1Item);
        registry.register(ModContentIds.SCREEN_TIER2, screenTier2, screenTier2Item);
        registry.register(ModContentIds.SCREEN_TIER3, screenTier3, screenTier3Item);
        registry.register(COMPAT_SCREEN_TIER1, screenTier1, screenTier1Item);
        registry.register(COMPAT_SCREEN_TIER2, screenTier2, screenTier2Item);
        registry.register(COMPAT_SCREEN_TIER3, screenTier3, screenTier3Item);
        registry.register(ModContentIds.KEYBOARD, keyboard, keyboardItem);
        registry.register(ModContentIds.MOTION_SENSOR, motionSensor, motionSensorItem);
        registry.register(COMPAT_MOTION_SENSOR_UPSTREAM, motionSensor, motionSensorItem);
        registry.register(ModContentIds.POWER_DISTRIBUTOR, powerDistributor, powerDistributorItem);
        registry.register(COMPAT_POWER_DISTRIBUTOR, powerDistributor, powerDistributorItem);
        registry.register(COMPAT_POWER_DISTRIBUTOR_UPSTREAM, powerDistributor, powerDistributorItem);
        registry.register(ModContentIds.POWER_CONVERTER, powerConverter, powerConverterItem);
        registry.register(COMPAT_POWER_CONVERTER, powerConverter, powerConverterItem);
        registry.register(COMPAT_POWER_CONVERTER_UPSTREAM, powerConverter, powerConverterItem);
        registry.register(ModContentIds.RACK, rack, rackItem);
        registry.register(COMPAT_RACK, rack, rackItem);
        registry.register(ModContentIds.RAID, raid, raidItem);
        registry.register(COMPAT_RAID, raid, raidItem);
        registry.register(ModContentIds.REDSTONE_IO, redstoneIo, redstoneIoItem);
        registry.register(ModContentIds.RELAY, relay, relayItem);
        registry.register(COMPAT_RELAY, relay, relayItem);
        registry.register(ModContentIds.NET_SPLITTER, netSplitter, netSplitterItem);
        registry.register(COMPAT_NET_SPLITTER, netSplitter, netSplitterItem);
        registry.register(COMPAT_NET_SPLITTER_UPSTREAM, netSplitter, netSplitterItem);
        registry.register(ModContentIds.TRANSPOSER, transposer, transposerItem);
        registry.register(ModContentIds.HOLOGRAM_TIER1, hologramTier1, hologramTier1Item);
        registry.register(ModContentIds.HOLOGRAM_TIER2, hologramTier2, hologramTier2Item);
        registry.register(ModContentIds.WAYPOINT, waypoint, waypointItem);
        registry.register(ModContentIds.PRINT, print, printItem);
        registry.register(ModContentIds.PRINTER, printer, printerItem);
        registry.register(ModContentIds.CPU_TIER1, null, cpuTier1Item);
        registry.register(ModContentIds.CPU_TIER2, null, cpuTier2Item);
        registry.register(ModContentIds.CPU_TIER3, null, cpuTier3Item);
        registry.register(ModContentIds.CARD_CONTAINER_TIER1, null, cardContainerTier1Item);
        registry.register(ModContentIds.CARD_CONTAINER_TIER2, null, cardContainerTier2Item);
        registry.register(ModContentIds.CARD_CONTAINER_TIER3, null, cardContainerTier3Item);
        registry.register(ModContentIds.MICROCONTROLLER_CASE_TIER1, null, microcontrollerCaseTier1Item);
        registry.register(ModContentIds.MICROCONTROLLER_CASE_TIER2, null, microcontrollerCaseTier2Item);
        registry.register(ModContentIds.MICROCONTROLLER_CASE_CREATIVE, null, microcontrollerCaseCreativeItem);
        registry.register(ModContentIds.TABLET_CASE_TIER1, null, tabletCaseTier1Item);
        registry.register(ModContentIds.TABLET_CASE_TIER2, null, tabletCaseTier2Item);
        registry.register(ModContentIds.TABLET_CASE_CREATIVE, null, tabletCaseCreativeItem);
        registry.register(ModContentIds.TABLET, null, tabletItem);
        registry.register(ModContentIds.DATA_CARD_TIER1, null, dataCardTier1Item);
        registry.register(ModContentIds.DATA_CARD_TIER2, null, dataCardTier2Item);
        registry.register(ModContentIds.DATA_CARD_TIER3, null, dataCardTier3Item);
        registry.register(ModContentIds.DEBUG_CARD, null, debugCardItem);
        registry.register(ModContentIds.DATABASE_UPGRADE_TIER1, null, databaseUpgradeTier1Item);
        registry.register(ModContentIds.DATABASE_UPGRADE_TIER2, null, databaseUpgradeTier2Item);
        registry.register(ModContentIds.DATABASE_UPGRADE_TIER3, null, databaseUpgradeTier3Item);
        registry.register(ModContentIds.BATTERY_UPGRADE_TIER1, null, batteryUpgradeTier1Item);
        registry.register(ModContentIds.BATTERY_UPGRADE_TIER2, null, batteryUpgradeTier2Item);
        registry.register(ModContentIds.BATTERY_UPGRADE_TIER3, null, batteryUpgradeTier3Item);
        registry.register(ModContentIds.BARCODE_READER_UPGRADE, null, barcodeReaderUpgradeItem);
        registry.register(ModContentIds.MEMORY_TIER1, null, memoryTier1Item);
        registry.register(ModContentIds.MEMORY_TIER2, null, memoryTier2Item);
        registry.register(ModContentIds.MEMORY_TIER3, null, memoryTier3Item);
        registry.register(ModContentIds.MEMORY_TIER4, null, memoryTier4Item);
        registry.register(ModContentIds.MEMORY_TIER5, null, memoryTier5Item);
        registry.register(ModContentIds.MEMORY_TIER6, null, memoryTier6Item);
        registry.register(ModContentIds.HDD_TIER1, null, hddTier1Item);
        registry.register(ModContentIds.HDD_TIER2, null, hddTier2Item);
        registry.register(ModContentIds.HDD_TIER3, null, hddTier3Item);
        registry.register(ModContentIds.INVENTORY_CONTROLLER_UPGRADE, null, inventoryControllerUpgradeItem);
        registry.register(ModContentIds.INVENTORY_UPGRADE, null, inventoryUpgradeItem);
        registry.register(ModContentIds.CRAFTING_UPGRADE, null, craftingUpgradeItem);
        registry.register(ModContentIds.INTERNET_CARD, null, internetCardItem);
        registry.register(ModContentIds.LINKED_CARD, null, linkedCardItem);
        registry.register(ModContentIds.NAVIGATION_UPGRADE, null, navigationUpgradeItem);
        registry.register(ModContentIds.EEPROM, null, eepromItem);
        registry.register(ModContentIds.EXPERIENCE_UPGRADE, null, experienceUpgradeItem);
        registry.register(ModContentIds.FLOPPY, null, floppyItem);
        registry.register(ModContentIds.GRAPHICS_CARD_TIER1, null, graphicsCardTier1Item);
        registry.register(ModContentIds.GRAPHICS_CARD_TIER2, null, graphicsCardTier2Item);
        registry.register(ModContentIds.GRAPHICS_CARD_TIER3, null, graphicsCardTier3Item);
        registry.register(ModContentIds.HOVER_UPGRADE_TIER1, null, hoverUpgradeTier1Item);
        registry.register(ModContentIds.HOVER_UPGRADE_TIER2, null, hoverUpgradeTier2Item);
        registry.register(ModContentIds.NETWORK_CARD, null, networkCardItem);
        registry.register(ModContentIds.PISTON_UPGRADE, null, pistonUpgradeItem);
        registry.register(ModContentIds.STICKY_PISTON_UPGRADE, null, stickyPistonUpgradeItem);
        registry.register(ModContentIds.SIGN_UPGRADE, null, signUpgradeItem);
        registry.register(ModContentIds.TRADING_UPGRADE, null, tradingUpgradeItem);
        registry.register(ModContentIds.TRACTOR_BEAM_UPGRADE, null, tractorBeamUpgradeItem);
        registry.register(ModContentIds.LEASH_UPGRADE, null, leashUpgradeItem);
        registry.register(ModContentIds.ANGEL_UPGRADE, null, angelUpgradeItem);
        registry.register(ModContentIds.CHUNKLOADER_UPGRADE, null, chunkloaderUpgradeItem);
        registry.register(ModContentIds.MFU, null, mfuItem);
        registry.register(ModContentIds.WIRELESS_NETWORK_CARD_TIER1, null, wirelessNetworkCardTier1Item);
        registry.register(ModContentIds.WIRELESS_NETWORK_CARD_TIER2, null, wirelessNetworkCardTier2Item);
        registry.register(ModContentIds.REDSTONE_CARD, null, redstoneCardItem);
        registry.register(ModContentIds.GENERATOR_UPGRADE, null, generatorUpgradeItem);
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
        registry.register(COMPAT_MICROCONTROLLER_CASE_TIER1, null, microcontrollerCaseTier1Item);
        registry.register(COMPAT_MICROCONTROLLER_CASE_TIER2, null, microcontrollerCaseTier2Item);
        registry.register(COMPAT_MICROCONTROLLER_CASE_TIER1_UPSTREAM, null, microcontrollerCaseTier1Item);
        registry.register(COMPAT_MICROCONTROLLER_CASE_TIER2_UPSTREAM, null, microcontrollerCaseTier2Item);
        registry.register(COMPAT_MICROCONTROLLER_CASE_CREATIVE_UPSTREAM, null, microcontrollerCaseCreativeItem);
        registry.register(COMPAT_TABLET_CASE_TIER1, null, tabletCaseTier1Item);
        registry.register(COMPAT_TABLET_CASE_TIER2, null, tabletCaseTier2Item);
        registry.register(COMPAT_TABLET_CASE_CREATIVE, null, tabletCaseCreativeItem);
        registry.register(COMPAT_DATA_CARD_TIER1, null, dataCardTier1Item);
        registry.register(COMPAT_DATA_CARD_TIER2, null, dataCardTier2Item);
        registry.register(COMPAT_DATA_CARD_TIER3, null, dataCardTier3Item);
        registry.register(COMPAT_DATA_CARD, null, dataCardTier1Item);
        registry.register(COMPAT_DEBUG_CARD, null, debugCardItem);
        registry.register(COMPAT_DEBUG_CARD_UPSTREAM, null, debugCardItem);
        registry.register(COMPAT_DATABASE_UPGRADE_TIER1, null, databaseUpgradeTier1Item);
        registry.register(COMPAT_DATABASE_UPGRADE_TIER2, null, databaseUpgradeTier2Item);
        registry.register(COMPAT_DATABASE_UPGRADE_TIER3, null, databaseUpgradeTier3Item);
        registry.register(COMPAT_DATABASE_UPGRADE_TIER1_UPSTREAM, null, databaseUpgradeTier1Item);
        registry.register(COMPAT_DATABASE_UPGRADE_TIER2_UPSTREAM, null, databaseUpgradeTier2Item);
        registry.register(COMPAT_DATABASE_UPGRADE_TIER3_UPSTREAM, null, databaseUpgradeTier3Item);
        registry.register(COMPAT_BATTERY_UPGRADE_TIER1, null, batteryUpgradeTier1Item);
        registry.register(COMPAT_BATTERY_UPGRADE_TIER2, null, batteryUpgradeTier2Item);
        registry.register(COMPAT_BATTERY_UPGRADE_TIER3, null, batteryUpgradeTier3Item);
        registry.register(COMPAT_BATTERY_UPGRADE_TIER1_UPSTREAM, null, batteryUpgradeTier1Item);
        registry.register(COMPAT_BATTERY_UPGRADE_TIER2_UPSTREAM, null, batteryUpgradeTier2Item);
        registry.register(COMPAT_BATTERY_UPGRADE_TIER3_UPSTREAM, null, batteryUpgradeTier3Item);
        registry.register(COMPAT_BARCODE_READER_UPGRADE, null, barcodeReaderUpgradeItem);
        registry.register(COMPAT_MEMORY_TIER1, null, memoryTier1Item);
        registry.register(COMPAT_MEMORY_TIER2, null, memoryTier2Item);
        registry.register(COMPAT_MEMORY_TIER3, null, memoryTier3Item);
        registry.register(COMPAT_MEMORY_TIER4, null, memoryTier4Item);
        registry.register(COMPAT_MEMORY_TIER5, null, memoryTier5Item);
        registry.register(COMPAT_MEMORY_TIER6, null, memoryTier6Item);
        registry.register(COMPAT_HDD_TIER1, null, hddTier1Item);
        registry.register(COMPAT_HDD_TIER2, null, hddTier2Item);
        registry.register(COMPAT_HDD_TIER3, null, hddTier3Item);
        registry.register(COMPAT_INVENTORY_CONTROLLER_UPGRADE_UPSTREAM, null, inventoryControllerUpgradeItem);
        registry.register(COMPAT_INVENTORY_UPGRADE, null, inventoryUpgradeItem);
        registry.register(COMPAT_CRAFTING_UPGRADE, null, craftingUpgradeItem);
        registry.register(COMPAT_INVENTORY_UPGRADE_UPSTREAM, null, inventoryUpgradeItem);
        registry.register(COMPAT_CRAFTING_UPGRADE_UPSTREAM, null, craftingUpgradeItem);
        registry.register(COMPAT_INTERNET_CARD, null, internetCardItem);
        registry.register(COMPAT_LINKED_CARD, null, linkedCardItem);
        registry.register(COMPAT_NAVIGATION_UPGRADE, null, navigationUpgradeItem);
        registry.register(COMPAT_NAVIGATION_UPGRADE_UPSTREAM, null, navigationUpgradeItem);
        registry.register(COMPAT_EXPERIENCE_UPGRADE, null, experienceUpgradeItem);
        registry.register(COMPAT_EXPERIENCE_UPGRADE_UPSTREAM, null, experienceUpgradeItem);
        registry.register(COMPAT_GRAPHICS_CARD_TIER1, null, graphicsCardTier1Item);
        registry.register(COMPAT_GRAPHICS_CARD_TIER2, null, graphicsCardTier2Item);
        registry.register(COMPAT_GRAPHICS_CARD_TIER3, null, graphicsCardTier3Item);
        registry.register(COMPAT_HOVER_UPGRADE_TIER1, null, hoverUpgradeTier1Item);
        registry.register(COMPAT_HOVER_UPGRADE_TIER2, null, hoverUpgradeTier2Item);
        registry.register(COMPAT_HOVER_UPGRADE_TIER1_UPSTREAM, null, hoverUpgradeTier1Item);
        registry.register(COMPAT_HOVER_UPGRADE_TIER2_UPSTREAM, null, hoverUpgradeTier2Item);
        registry.register(COMPAT_HOLOGRAM_TIER1, hologramTier1, hologramTier1Item);
        registry.register(COMPAT_HOLOGRAM_TIER2, hologramTier2, hologramTier2Item);
        registry.register(COMPAT_NETWORK_CARD, null, networkCardItem);
        registry.register(COMPAT_PISTON_UPGRADE, null, pistonUpgradeItem);
        registry.register(COMPAT_STICKY_PISTON_UPGRADE, null, stickyPistonUpgradeItem);
        registry.register(COMPAT_SIGN_UPGRADE, null, signUpgradeItem);
        registry.register(COMPAT_TRADING_UPGRADE, null, tradingUpgradeItem);
        registry.register(COMPAT_TRACTOR_BEAM_UPGRADE, null, tractorBeamUpgradeItem);
        registry.register(COMPAT_LEASH_UPGRADE, null, leashUpgradeItem);
        registry.register(COMPAT_ANGEL_UPGRADE, null, angelUpgradeItem);
        registry.register(COMPAT_CHUNKLOADER_UPGRADE, null, chunkloaderUpgradeItem);
        registry.register(COMPAT_PISTON_UPGRADE_UPSTREAM, null, pistonUpgradeItem);
        registry.register(COMPAT_STICKY_PISTON_UPGRADE_UPSTREAM, null, stickyPistonUpgradeItem);
        registry.register(COMPAT_SIGN_UPGRADE_UPSTREAM, null, signUpgradeItem);
        registry.register(COMPAT_TRADING_UPGRADE_UPSTREAM, null, tradingUpgradeItem);
        registry.register(COMPAT_TRACTOR_BEAM_UPGRADE_UPSTREAM, null, tractorBeamUpgradeItem);
        registry.register(COMPAT_LEASH_UPGRADE_UPSTREAM, null, leashUpgradeItem);
        registry.register(COMPAT_ANGEL_UPGRADE_UPSTREAM, null, angelUpgradeItem);
        registry.register(COMPAT_CHUNKLOADER_UPGRADE_UPSTREAM, null, chunkloaderUpgradeItem);
        registry.register(COMPAT_MFU, null, mfuItem);
        registry.register(COMPAT_WIRELESS_NETWORK_CARD_TIER1, null, wirelessNetworkCardTier1Item);
        registry.register(COMPAT_WIRELESS_NETWORK_CARD_TIER2, null, wirelessNetworkCardTier2Item);
        registry.register(COMPAT_WIRELESS_NETWORK_CARD, null, wirelessNetworkCardTier2Item);
        registry.register(COMPAT_REDSTONE_CARD, null, redstoneCardItem);
        registry.register(COMPAT_REDSTONE_CARD_TIER1_UPSTREAM, null, redstoneCardItem);
        registry.register(COMPAT_GENERATOR_UPGRADE, null, generatorUpgradeItem);
        registry.register(COMPAT_SOLAR_GENERATOR_UPGRADE, null, solarGeneratorUpgradeItem);
        registry.register(COMPAT_TANK_UPGRADE, null, tankUpgradeItem);
        registry.register(COMPAT_TANK_CONTROLLER_UPGRADE, null, tankControllerUpgradeItem);
        registry.register(COMPAT_GENERATOR_UPGRADE_UPSTREAM, null, generatorUpgradeItem);
        registry.register(COMPAT_SOLAR_GENERATOR_UPGRADE_UPSTREAM, null, solarGeneratorUpgradeItem);
        registry.register(COMPAT_TANK_UPGRADE_UPSTREAM, null, tankUpgradeItem);
        registry.register(COMPAT_TANK_CONTROLLER_UPGRADE_UPSTREAM, null, tankControllerUpgradeItem);
        registry.register(COMPAT_UPGRADE_CONTAINER_TIER1, null, upgradeContainerTier1Item);
        registry.register(COMPAT_UPGRADE_CONTAINER_TIER2, null, upgradeContainerTier2Item);
        registry.register(COMPAT_UPGRADE_CONTAINER_TIER3, null, upgradeContainerTier3Item);
    }

    private ModContentCatalog() {
    }
}
