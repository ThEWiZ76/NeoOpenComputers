package li.cil.oc.common;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.item.AngelUpgradeItem;
import li.cil.oc.common.item.AnalyzerItem;
import li.cil.oc.common.item.ApuItem;
import li.cil.oc.common.item.BarcodeReaderUpgradeItem;
import li.cil.oc.common.item.BatteryUpgradeItem;
import li.cil.oc.common.item.CardContainerItem;
import li.cil.oc.common.item.ChunkloaderUpgradeItem;
import li.cil.oc.common.item.ComponentBusItem;
import li.cil.oc.common.item.CraftingUpgradeItem;
import li.cil.oc.common.item.CpuItem;
import li.cil.oc.common.item.DataCardItem;
import li.cil.oc.common.item.DatabaseUpgradeItem;
import li.cil.oc.common.item.DebugCardItem;
import li.cil.oc.common.item.DiskDriveMountableItem;
import li.cil.oc.common.item.EepromItem;
import li.cil.oc.common.item.ExperienceUpgradeItem;
import li.cil.oc.common.item.FloppyItem;
import li.cil.oc.common.item.GeneratorUpgradeItem;
import li.cil.oc.common.item.GraphicsCardItem;
import li.cil.oc.common.item.HardDiskDriveItem;
import li.cil.oc.common.item.HoverUpgradeItem;
import li.cil.oc.common.item.InkCartridgeItem;
import li.cil.oc.common.item.InventoryControllerUpgradeItem;
import li.cil.oc.common.item.InventoryUpgradeItem;
import li.cil.oc.common.item.InternetCardItem;
import li.cil.oc.common.item.LeashUpgradeItem;
import li.cil.oc.common.item.LinkedCardItem;
import li.cil.oc.common.item.ManualItem;
import li.cil.oc.common.item.MemoryItem;
import li.cil.oc.common.item.MicrocontrollerCaseItem;
import li.cil.oc.common.item.MfuItem;
import li.cil.oc.common.item.NanomachinesItem;
import li.cil.oc.common.item.NavigationUpgradeItem;
import li.cil.oc.common.item.NetworkCardItem;
import li.cil.oc.common.item.PistonUpgradeItem;
import li.cil.oc.common.item.PrintItem;
import li.cil.oc.common.item.RedstoneCardItem;
import li.cil.oc.common.item.ServerItem;
import li.cil.oc.common.item.SignUpgradeItem;
import li.cil.oc.common.item.SolarGeneratorUpgradeItem;
import li.cil.oc.common.item.StickyPistonUpgradeItem;
import li.cil.oc.common.item.TankControllerUpgradeItem;
import li.cil.oc.common.item.TankUpgradeItem;
import li.cil.oc.common.item.TabletCaseItem;
import li.cil.oc.common.item.TabletItem;
import li.cil.oc.common.item.TerminalItem;
import li.cil.oc.common.item.TerminalServerItem;
import li.cil.oc.common.item.TexturePickerItem;
import li.cil.oc.common.item.TradingUpgradeItem;
import li.cil.oc.common.item.TractorBeamUpgradeItem;
import li.cil.oc.common.item.UpgradeContainerItem;
import li.cil.oc.common.item.WirelessNetworkCardItem;
import li.cil.oc.common.item.WrenchItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NeoOpenComputers.MODID);

    public static final DeferredItem<BlockItem> ADAPTER = ITEMS.registerSimpleBlockItem(
        ModContentIds.ADAPTER,
        ModBlocks.ADAPTER);

    public static final DeferredItem<AnalyzerItem> ANALYZER = ITEMS.register(
        ModContentIds.ANALYZER,
        () -> new AnalyzerItem(new Item.Properties()));

    public static final DeferredItem<WrenchItem> WRENCH = ITEMS.register(
        ModContentIds.WRENCH,
        () -> new WrenchItem(new Item.Properties()));

    public static final DeferredItem<TexturePickerItem> TEXTURE_PICKER = ITEMS.register(
        ModContentIds.TEXTURE_PICKER,
        () -> new TexturePickerItem(new Item.Properties()));

    public static final DeferredItem<TerminalItem> TERMINAL = ITEMS.register(
        ModContentIds.TERMINAL,
        () -> new TerminalItem(new Item.Properties()));

    public static final DeferredItem<TerminalServerItem> TERMINAL_SERVER = ITEMS.register(
        ModContentIds.TERMINAL_SERVER,
        () -> new TerminalServerItem(new Item.Properties()));

    public static final DeferredItem<NanomachinesItem> NANOMACHINES = ITEMS.register(
        ModContentIds.NANOMACHINES,
        () -> new NanomachinesItem(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<ServerItem> SERVER_TIER1 = ITEMS.register(
        ModContentIds.SERVER_TIER1,
        () -> new ServerItem(new Item.Properties(), 0));

    public static final DeferredItem<ServerItem> SERVER_TIER2 = ITEMS.register(
        ModContentIds.SERVER_TIER2,
        () -> new ServerItem(new Item.Properties(), 1));

    public static final DeferredItem<ServerItem> SERVER_TIER3 = ITEMS.register(
        ModContentIds.SERVER_TIER3,
        () -> new ServerItem(new Item.Properties(), 2));

    public static final DeferredItem<ApuItem> APU_TIER1 = ITEMS.register(
        ModContentIds.APU_TIER1,
        () -> new ApuItem(new Item.Properties(), 0));

    public static final DeferredItem<ApuItem> APU_TIER2 = ITEMS.register(
        ModContentIds.APU_TIER2,
        () -> new ApuItem(new Item.Properties(), 1));

    public static final DeferredItem<BlockItem> ASSEMBLER = ITEMS.registerSimpleBlockItem(
        ModContentIds.ASSEMBLER,
        ModBlocks.ASSEMBLER);

    public static final DeferredItem<BlockItem> CABLE = ITEMS.registerSimpleBlockItem(
        ModContentIds.CABLE,
        ModBlocks.CABLE);

    public static final DeferredItem<BlockItem> CHARGER = ITEMS.registerSimpleBlockItem(
        ModContentIds.CHARGER,
        ModBlocks.CHARGER);

    public static final DeferredItem<BlockItem> CHAMELIUM_BLOCK = ITEMS.registerSimpleBlockItem(
        ModContentIds.CHAMELIUM_BLOCK,
        ModBlocks.CHAMELIUM_BLOCK);

    public static final DeferredItem<BlockItem> COMPUTER_CASE_TIER1 = ITEMS.registerSimpleBlockItem(
        ModContentIds.COMPUTER_CASE_TIER1,
        ModBlocks.COMPUTER_CASE_TIER1);

    public static final DeferredItem<BlockItem> COMPUTER_CASE_TIER2 = ITEMS.registerSimpleBlockItem(
        ModContentIds.COMPUTER_CASE_TIER2,
        ModBlocks.COMPUTER_CASE_TIER2);

    public static final DeferredItem<BlockItem> COMPUTER_CASE_TIER3 = ITEMS.registerSimpleBlockItem(
        ModContentIds.COMPUTER_CASE_TIER3,
        ModBlocks.COMPUTER_CASE_TIER3);

    public static final DeferredItem<BlockItem> DISASSEMBLER = ITEMS.registerSimpleBlockItem(
        ModContentIds.DISASSEMBLER,
        ModBlocks.DISASSEMBLER);

    public static final DeferredItem<BlockItem> DISK_DRIVE = ITEMS.registerSimpleBlockItem(
        ModContentIds.DISK_DRIVE,
        ModBlocks.DISK_DRIVE);

    public static final DeferredItem<DiskDriveMountableItem> DISK_DRIVE_MOUNTABLE = ITEMS.register(
        ModContentIds.DISK_DRIVE_MOUNTABLE,
        () -> new DiskDriveMountableItem(new Item.Properties()));

    public static final DeferredItem<BlockItem> ENDSTONE = ITEMS.registerSimpleBlockItem(
        ModContentIds.ENDSTONE,
        ModBlocks.ENDSTONE);

    public static final DeferredItem<BlockItem> GEOLYZER = ITEMS.registerSimpleBlockItem(
        ModContentIds.GEOLYZER,
        ModBlocks.GEOLYZER);

    public static final DeferredItem<BlockItem> HOLOGRAM_TIER1 = ITEMS.registerSimpleBlockItem(
        ModContentIds.HOLOGRAM_TIER1,
        ModBlocks.HOLOGRAM_TIER1);

    public static final DeferredItem<BlockItem> HOLOGRAM_TIER2 = ITEMS.registerSimpleBlockItem(
        ModContentIds.HOLOGRAM_TIER2,
        ModBlocks.HOLOGRAM_TIER2);

    public static final DeferredItem<BlockItem> POWER_DISTRIBUTOR = ITEMS.registerSimpleBlockItem(
        ModContentIds.POWER_DISTRIBUTOR,
        ModBlocks.POWER_DISTRIBUTOR);

    public static final DeferredItem<BlockItem> POWER_CONVERTER = ITEMS.registerSimpleBlockItem(
        ModContentIds.POWER_CONVERTER,
        ModBlocks.POWER_CONVERTER);

    public static final DeferredItem<PrintItem> PRINT = ITEMS.register(
        ModContentIds.PRINT,
        () -> new PrintItem(ModBlocks.PRINT.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> PRINTER = ITEMS.registerSimpleBlockItem(
        ModContentIds.PRINTER,
        ModBlocks.PRINTER);

    public static final DeferredItem<BlockItem> RACK = ITEMS.registerSimpleBlockItem(
        ModContentIds.RACK,
        ModBlocks.RACK);

    public static final DeferredItem<BlockItem> RAID = ITEMS.registerSimpleBlockItem(
        ModContentIds.RAID,
        ModBlocks.RAID);

    public static final DeferredItem<BlockItem> RELAY = ITEMS.registerSimpleBlockItem(
        ModContentIds.RELAY,
        ModBlocks.RELAY);

    public static final DeferredItem<BlockItem> ROBOT = ITEMS.registerSimpleBlockItem(
        ModContentIds.ROBOT,
        ModBlocks.ROBOT);

    public static final DeferredItem<BlockItem> NET_SPLITTER = ITEMS.registerSimpleBlockItem(
        ModContentIds.NET_SPLITTER,
        ModBlocks.NET_SPLITTER);

    public static final DeferredItem<Item> CUTTING_WIRE = ITEMS.registerSimpleItem(ModContentIds.CUTTING_WIRE, new Item.Properties());
    public static final DeferredItem<Item> ACID = ITEMS.registerSimpleItem(ModContentIds.ACID, new Item.Properties());
    public static final DeferredItem<Item> RAW_CIRCUIT_BOARD = ITEMS.registerSimpleItem(ModContentIds.RAW_CIRCUIT_BOARD, new Item.Properties());
    public static final DeferredItem<Item> CIRCUIT_BOARD = ITEMS.registerSimpleItem(ModContentIds.CIRCUIT_BOARD, new Item.Properties());
    public static final DeferredItem<Item> PRINTED_CIRCUIT_BOARD = ITEMS.registerSimpleItem(ModContentIds.PRINTED_CIRCUIT_BOARD, new Item.Properties());
    public static final DeferredItem<Item> CARD = ITEMS.registerSimpleItem(ModContentIds.CARD, new Item.Properties());
    public static final DeferredItem<Item> CHAMELIUM = ITEMS.registerSimpleItem(ModContentIds.CHAMELIUM, new Item.Properties());
    public static final DeferredItem<Item> TRANSISTOR = ITEMS.registerSimpleItem(ModContentIds.TRANSISTOR, new Item.Properties());
    public static final DeferredItem<Item> CAPACITOR = ITEMS.registerSimpleItem(ModContentIds.CAPACITOR, new Item.Properties());
    public static final DeferredItem<ComponentBusItem> COMPONENT_BUS_TIER1 = ITEMS.register(
        ModContentIds.COMPONENT_BUS_TIER1,
        () -> new ComponentBusItem(new Item.Properties(), 0));
    public static final DeferredItem<ComponentBusItem> COMPONENT_BUS_TIER2 = ITEMS.register(
        ModContentIds.COMPONENT_BUS_TIER2,
        () -> new ComponentBusItem(new Item.Properties(), 1));
    public static final DeferredItem<ComponentBusItem> COMPONENT_BUS_TIER3 = ITEMS.register(
        ModContentIds.COMPONENT_BUS_TIER3,
        () -> new ComponentBusItem(new Item.Properties(), 2));
    public static final DeferredItem<ComponentBusItem> COMPONENT_BUS_CREATIVE = ITEMS.register(
        ModContentIds.COMPONENT_BUS_CREATIVE,
        () -> new ComponentBusItem(new Item.Properties(), 3));
    public static final DeferredItem<Item> MICROCHIP_TIER1 = ITEMS.registerSimpleItem(ModContentIds.MICROCHIP_TIER1, new Item.Properties());
    public static final DeferredItem<Item> MICROCHIP_TIER2 = ITEMS.registerSimpleItem(ModContentIds.MICROCHIP_TIER2, new Item.Properties());
    public static final DeferredItem<Item> MICROCHIP_TIER3 = ITEMS.registerSimpleItem(ModContentIds.MICROCHIP_TIER3, new Item.Properties());
    public static final DeferredItem<Item> DIAMOND_CHIP = ITEMS.registerSimpleItem(ModContentIds.DIAMOND_CHIP, new Item.Properties());
    public static final DeferredItem<Item> ALU = ITEMS.registerSimpleItem(ModContentIds.ALU, new Item.Properties());
    public static final DeferredItem<Item> CONTROL_UNIT = ITEMS.registerSimpleItem(ModContentIds.CONTROL_UNIT, new Item.Properties());
    public static final DeferredItem<Item> DISK_PLATTER = ITEMS.registerSimpleItem(ModContentIds.DISK_PLATTER, new Item.Properties());
    public static final DeferredItem<Item> INTERWEB = ITEMS.registerSimpleItem(ModContentIds.INTERWEB, new Item.Properties());
    public static final DeferredItem<Item> INK_CARTRIDGE_EMPTY = ITEMS.registerSimpleItem(ModContentIds.INK_CARTRIDGE_EMPTY, new Item.Properties().stacksTo(1));
    public static final DeferredItem<InkCartridgeItem> INK_CARTRIDGE = ITEMS.register(
        ModContentIds.INK_CARTRIDGE,
        () -> new InkCartridgeItem(new Item.Properties(), INK_CARTRIDGE_EMPTY.get()));
    public static final DeferredItem<Item> BUTTON_GROUP = ITEMS.registerSimpleItem(ModContentIds.BUTTON_GROUP, new Item.Properties());
    public static final DeferredItem<Item> ARROW_KEYS = ITEMS.registerSimpleItem(ModContentIds.ARROW_KEYS, new Item.Properties());
    public static final DeferredItem<Item> NUM_PAD = ITEMS.registerSimpleItem(ModContentIds.NUM_PAD, new Item.Properties());

    public static final DeferredItem<BatteryUpgradeItem> BATTERY_UPGRADE_TIER1 = ITEMS.register(
        ModContentIds.BATTERY_UPGRADE_TIER1,
        () -> new BatteryUpgradeItem(new Item.Properties(), 0));

    public static final DeferredItem<BatteryUpgradeItem> BATTERY_UPGRADE_TIER2 = ITEMS.register(
        ModContentIds.BATTERY_UPGRADE_TIER2,
        () -> new BatteryUpgradeItem(new Item.Properties(), 1));

    public static final DeferredItem<BatteryUpgradeItem> BATTERY_UPGRADE_TIER3 = ITEMS.register(
        ModContentIds.BATTERY_UPGRADE_TIER3,
        () -> new BatteryUpgradeItem(new Item.Properties(), 2));

    public static final DeferredItem<BarcodeReaderUpgradeItem> BARCODE_READER_UPGRADE = ITEMS.register(
        ModContentIds.BARCODE_READER_UPGRADE,
        () -> new BarcodeReaderUpgradeItem(new Item.Properties()));

    public static final DeferredItem<CardContainerItem> CARD_CONTAINER_TIER1 = ITEMS.register(
        ModContentIds.CARD_CONTAINER_TIER1,
        () -> new CardContainerItem(new Item.Properties(), 0));

    public static final DeferredItem<CardContainerItem> CARD_CONTAINER_TIER2 = ITEMS.register(
        ModContentIds.CARD_CONTAINER_TIER2,
        () -> new CardContainerItem(new Item.Properties(), 1));

    public static final DeferredItem<CardContainerItem> CARD_CONTAINER_TIER3 = ITEMS.register(
        ModContentIds.CARD_CONTAINER_TIER3,
        () -> new CardContainerItem(new Item.Properties(), 2));

    public static final DeferredItem<MicrocontrollerCaseItem> MICROCONTROLLER_CASE_TIER1 = ITEMS.register(
        ModContentIds.MICROCONTROLLER_CASE_TIER1,
        () -> new MicrocontrollerCaseItem(new Item.Properties(), 0));

    public static final DeferredItem<MicrocontrollerCaseItem> MICROCONTROLLER_CASE_TIER2 = ITEMS.register(
        ModContentIds.MICROCONTROLLER_CASE_TIER2,
        () -> new MicrocontrollerCaseItem(new Item.Properties(), 1));

    public static final DeferredItem<MicrocontrollerCaseItem> MICROCONTROLLER_CASE_CREATIVE = ITEMS.register(
        ModContentIds.MICROCONTROLLER_CASE_CREATIVE,
        () -> new MicrocontrollerCaseItem(new Item.Properties(), 3));

    public static final DeferredItem<BlockItem> MICROCONTROLLER_TIER1 = ITEMS.registerSimpleBlockItem(
        ModContentIds.MICROCONTROLLER_TIER1,
        ModBlocks.MICROCONTROLLER_TIER1);

    public static final DeferredItem<BlockItem> MICROCONTROLLER_TIER2 = ITEMS.registerSimpleBlockItem(
        ModContentIds.MICROCONTROLLER_TIER2,
        ModBlocks.MICROCONTROLLER_TIER2);

    public static final DeferredItem<BlockItem> MICROCONTROLLER_CREATIVE = ITEMS.registerSimpleBlockItem(
        ModContentIds.MICROCONTROLLER_CREATIVE,
        ModBlocks.MICROCONTROLLER_CREATIVE);

    public static final DeferredItem<TabletCaseItem> TABLET_CASE_TIER1 = ITEMS.register(
        ModContentIds.TABLET_CASE_TIER1,
        () -> new TabletCaseItem(new Item.Properties(), 0));

    public static final DeferredItem<TabletCaseItem> TABLET_CASE_TIER2 = ITEMS.register(
        ModContentIds.TABLET_CASE_TIER2,
        () -> new TabletCaseItem(new Item.Properties(), 1));

    public static final DeferredItem<TabletCaseItem> TABLET_CASE_CREATIVE = ITEMS.register(
        ModContentIds.TABLET_CASE_CREATIVE,
        () -> new TabletCaseItem(new Item.Properties(), 3));

    public static final DeferredItem<TabletItem> TABLET = ITEMS.register(
        ModContentIds.TABLET,
        () -> new TabletItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<CpuItem> CPU_TIER1 = ITEMS.register(
        ModContentIds.CPU_TIER1,
        () -> new CpuItem(new Item.Properties(), 0));

    public static final DeferredItem<CpuItem> CPU_TIER2 = ITEMS.register(
        ModContentIds.CPU_TIER2,
        () -> new CpuItem(new Item.Properties(), 1));

    public static final DeferredItem<CpuItem> CPU_TIER3 = ITEMS.register(
        ModContentIds.CPU_TIER3,
        () -> new CpuItem(new Item.Properties(), 2));

    public static final DeferredItem<DataCardItem> DATA_CARD_TIER1 = ITEMS.register(
        ModContentIds.DATA_CARD_TIER1,
        () -> new DataCardItem(new Item.Properties(), 0));

    public static final DeferredItem<DataCardItem> DATA_CARD_TIER2 = ITEMS.register(
        ModContentIds.DATA_CARD_TIER2,
        () -> new DataCardItem(new Item.Properties(), 1));

    public static final DeferredItem<DataCardItem> DATA_CARD_TIER3 = ITEMS.register(
        ModContentIds.DATA_CARD_TIER3,
        () -> new DataCardItem(new Item.Properties(), 2));

    public static final DeferredItem<DebugCardItem> DEBUG_CARD = ITEMS.register(
        ModContentIds.DEBUG_CARD,
        () -> new DebugCardItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredItem<DatabaseUpgradeItem> DATABASE_UPGRADE_TIER1 = ITEMS.register(
        ModContentIds.DATABASE_UPGRADE_TIER1,
        () -> new DatabaseUpgradeItem(new Item.Properties(), 0));

    public static final DeferredItem<DatabaseUpgradeItem> DATABASE_UPGRADE_TIER2 = ITEMS.register(
        ModContentIds.DATABASE_UPGRADE_TIER2,
        () -> new DatabaseUpgradeItem(new Item.Properties(), 1));

    public static final DeferredItem<DatabaseUpgradeItem> DATABASE_UPGRADE_TIER3 = ITEMS.register(
        ModContentIds.DATABASE_UPGRADE_TIER3,
        () -> new DatabaseUpgradeItem(new Item.Properties(), 2));

    public static final DeferredItem<EepromItem> EEPROM = ITEMS.register(
        ModContentIds.EEPROM,
        () -> new EepromItem(new Item.Properties()));

    public static final DeferredItem<ExperienceUpgradeItem> EXPERIENCE_UPGRADE = ITEMS.register(
        ModContentIds.EXPERIENCE_UPGRADE,
        () -> new ExperienceUpgradeItem(new Item.Properties()));

    public static final DeferredItem<FloppyItem> FLOPPY = ITEMS.register(
        ModContentIds.FLOPPY,
        () -> new FloppyItem(new Item.Properties()));

    public static final DeferredItem<GeneratorUpgradeItem> GENERATOR_UPGRADE = ITEMS.register(
        ModContentIds.GENERATOR_UPGRADE,
        () -> new GeneratorUpgradeItem(new Item.Properties()));

    public static final DeferredItem<GraphicsCardItem> GRAPHICS_CARD_TIER1 = ITEMS.register(
        ModContentIds.GRAPHICS_CARD_TIER1,
        () -> new GraphicsCardItem(new Item.Properties(), 0));

    public static final DeferredItem<GraphicsCardItem> GRAPHICS_CARD_TIER2 = ITEMS.register(
        ModContentIds.GRAPHICS_CARD_TIER2,
        () -> new GraphicsCardItem(new Item.Properties(), 1));

    public static final DeferredItem<GraphicsCardItem> GRAPHICS_CARD_TIER3 = ITEMS.register(
        ModContentIds.GRAPHICS_CARD_TIER3,
        () -> new GraphicsCardItem(new Item.Properties(), 2));

    public static final DeferredItem<HardDiskDriveItem> HDD_TIER1 = ITEMS.register(
        ModContentIds.HDD_TIER1,
        () -> new HardDiskDriveItem(new Item.Properties(), 0));

    public static final DeferredItem<HardDiskDriveItem> HDD_TIER2 = ITEMS.register(
        ModContentIds.HDD_TIER2,
        () -> new HardDiskDriveItem(new Item.Properties(), 1));

    public static final DeferredItem<HardDiskDriveItem> HDD_TIER3 = ITEMS.register(
        ModContentIds.HDD_TIER3,
        () -> new HardDiskDriveItem(new Item.Properties(), 2));

    public static final DeferredItem<HoverUpgradeItem> HOVER_UPGRADE_TIER1 = ITEMS.register(
        ModContentIds.HOVER_UPGRADE_TIER1,
        () -> new HoverUpgradeItem(new Item.Properties(), 0));

    public static final DeferredItem<HoverUpgradeItem> HOVER_UPGRADE_TIER2 = ITEMS.register(
        ModContentIds.HOVER_UPGRADE_TIER2,
        () -> new HoverUpgradeItem(new Item.Properties(), 1));

    public static final DeferredItem<InventoryControllerUpgradeItem> INVENTORY_CONTROLLER_UPGRADE = ITEMS.register(
        ModContentIds.INVENTORY_CONTROLLER_UPGRADE,
        () -> new InventoryControllerUpgradeItem(new Item.Properties()));

    public static final DeferredItem<InventoryUpgradeItem> INVENTORY_UPGRADE = ITEMS.register(
        ModContentIds.INVENTORY_UPGRADE,
        () -> new InventoryUpgradeItem(new Item.Properties()));

    public static final DeferredItem<CraftingUpgradeItem> CRAFTING_UPGRADE = ITEMS.register(
        ModContentIds.CRAFTING_UPGRADE,
        () -> new CraftingUpgradeItem(new Item.Properties()));

    public static final DeferredItem<InternetCardItem> INTERNET_CARD = ITEMS.register(
        ModContentIds.INTERNET_CARD,
        () -> new InternetCardItem(new Item.Properties()));

    public static final DeferredItem<LinkedCardItem> LINKED_CARD = ITEMS.register(
        ModContentIds.LINKED_CARD,
        () -> new LinkedCardItem(new Item.Properties()));

    public static final DeferredItem<ManualItem> MANUAL = ITEMS.register(
        ModContentIds.MANUAL,
        () -> new ManualItem(new Item.Properties()));

    public static final DeferredItem<MemoryItem> MEMORY_TIER1 = ITEMS.register(
        ModContentIds.MEMORY_TIER1,
        () -> new MemoryItem(new Item.Properties(), 0));

    public static final DeferredItem<MemoryItem> MEMORY_TIER2 = ITEMS.register(
        ModContentIds.MEMORY_TIER2,
        () -> new MemoryItem(new Item.Properties(), 1));

    public static final DeferredItem<MemoryItem> MEMORY_TIER3 = ITEMS.register(
        ModContentIds.MEMORY_TIER3,
        () -> new MemoryItem(new Item.Properties(), 2));

    public static final DeferredItem<MemoryItem> MEMORY_TIER4 = ITEMS.register(
        ModContentIds.MEMORY_TIER4,
        () -> new MemoryItem(new Item.Properties(), 3));

    public static final DeferredItem<MemoryItem> MEMORY_TIER5 = ITEMS.register(
        ModContentIds.MEMORY_TIER5,
        () -> new MemoryItem(new Item.Properties(), 4));

    public static final DeferredItem<MemoryItem> MEMORY_TIER6 = ITEMS.register(
        ModContentIds.MEMORY_TIER6,
        () -> new MemoryItem(new Item.Properties(), 5));

    public static final DeferredItem<NavigationUpgradeItem> NAVIGATION_UPGRADE = ITEMS.register(
        ModContentIds.NAVIGATION_UPGRADE,
        () -> new NavigationUpgradeItem(new Item.Properties()));

    public static final DeferredItem<NetworkCardItem> NETWORK_CARD = ITEMS.register(
        ModContentIds.NETWORK_CARD,
        () -> new NetworkCardItem(new Item.Properties()));

    public static final DeferredItem<PistonUpgradeItem> PISTON_UPGRADE = ITEMS.register(
        ModContentIds.PISTON_UPGRADE,
        () -> new PistonUpgradeItem(new Item.Properties()));

    public static final DeferredItem<StickyPistonUpgradeItem> STICKY_PISTON_UPGRADE = ITEMS.register(
        ModContentIds.STICKY_PISTON_UPGRADE,
        () -> new StickyPistonUpgradeItem(new Item.Properties()));

    public static final DeferredItem<SignUpgradeItem> SIGN_UPGRADE = ITEMS.register(
        ModContentIds.SIGN_UPGRADE,
        () -> new SignUpgradeItem(new Item.Properties()));

    public static final DeferredItem<TradingUpgradeItem> TRADING_UPGRADE = ITEMS.register(
        ModContentIds.TRADING_UPGRADE,
        () -> new TradingUpgradeItem(new Item.Properties()));

    public static final DeferredItem<TractorBeamUpgradeItem> TRACTOR_BEAM_UPGRADE = ITEMS.register(
        ModContentIds.TRACTOR_BEAM_UPGRADE,
        () -> new TractorBeamUpgradeItem(new Item.Properties()));

    public static final DeferredItem<LeashUpgradeItem> LEASH_UPGRADE = ITEMS.register(
        ModContentIds.LEASH_UPGRADE,
        () -> new LeashUpgradeItem(new Item.Properties()));

    public static final DeferredItem<AngelUpgradeItem> ANGEL_UPGRADE = ITEMS.register(
        ModContentIds.ANGEL_UPGRADE,
        () -> new AngelUpgradeItem(new Item.Properties()));

    public static final DeferredItem<ChunkloaderUpgradeItem> CHUNKLOADER_UPGRADE = ITEMS.register(
        ModContentIds.CHUNKLOADER_UPGRADE,
        () -> new ChunkloaderUpgradeItem(new Item.Properties()));

    public static final DeferredItem<MfuItem> MFU = ITEMS.register(
        ModContentIds.MFU,
        () -> new MfuItem(new Item.Properties()));

    public static final DeferredItem<WirelessNetworkCardItem> WIRELESS_NETWORK_CARD_TIER1 = ITEMS.register(
        ModContentIds.WIRELESS_NETWORK_CARD_TIER1,
        () -> new WirelessNetworkCardItem(new Item.Properties(), 0));

    public static final DeferredItem<WirelessNetworkCardItem> WIRELESS_NETWORK_CARD_TIER2 = ITEMS.register(
        ModContentIds.WIRELESS_NETWORK_CARD_TIER2,
        () -> new WirelessNetworkCardItem(new Item.Properties(), 1));

    public static final DeferredItem<RedstoneCardItem> REDSTONE_CARD = ITEMS.register(
        ModContentIds.REDSTONE_CARD,
        () -> new RedstoneCardItem(new Item.Properties()));

    public static final DeferredItem<SolarGeneratorUpgradeItem> SOLAR_GENERATOR_UPGRADE = ITEMS.register(
        ModContentIds.SOLAR_GENERATOR_UPGRADE,
        () -> new SolarGeneratorUpgradeItem(new Item.Properties()));

    public static final DeferredItem<TankUpgradeItem> TANK_UPGRADE = ITEMS.register(
        ModContentIds.TANK_UPGRADE,
        () -> new TankUpgradeItem(new Item.Properties()));

    public static final DeferredItem<TankControllerUpgradeItem> TANK_CONTROLLER_UPGRADE = ITEMS.register(
        ModContentIds.TANK_CONTROLLER_UPGRADE,
        () -> new TankControllerUpgradeItem(new Item.Properties()));

    public static final DeferredItem<UpgradeContainerItem> UPGRADE_CONTAINER_TIER1 = ITEMS.register(
        ModContentIds.UPGRADE_CONTAINER_TIER1,
        () -> new UpgradeContainerItem(new Item.Properties(), 0));

    public static final DeferredItem<UpgradeContainerItem> UPGRADE_CONTAINER_TIER2 = ITEMS.register(
        ModContentIds.UPGRADE_CONTAINER_TIER2,
        () -> new UpgradeContainerItem(new Item.Properties(), 1));

    public static final DeferredItem<UpgradeContainerItem> UPGRADE_CONTAINER_TIER3 = ITEMS.register(
        ModContentIds.UPGRADE_CONTAINER_TIER3,
        () -> new UpgradeContainerItem(new Item.Properties(), 2));

    public static final DeferredItem<BlockItem> SCREEN_TIER1 = ITEMS.registerSimpleBlockItem(
        ModContentIds.SCREEN_TIER1,
        ModBlocks.SCREEN_TIER1);

    public static final DeferredItem<BlockItem> SCREEN_TIER2 = ITEMS.registerSimpleBlockItem(
        ModContentIds.SCREEN_TIER2,
        ModBlocks.SCREEN_TIER2);

    public static final DeferredItem<BlockItem> SCREEN_TIER3 = ITEMS.registerSimpleBlockItem(
        ModContentIds.SCREEN_TIER3,
        ModBlocks.SCREEN_TIER3);

    public static final DeferredItem<BlockItem> KEYBOARD = ITEMS.registerSimpleBlockItem(
        ModContentIds.KEYBOARD,
        ModBlocks.KEYBOARD);

    public static final DeferredItem<BlockItem> MOTION_SENSOR = ITEMS.registerSimpleBlockItem(
        ModContentIds.MOTION_SENSOR,
        ModBlocks.MOTION_SENSOR);

    public static final DeferredItem<BlockItem> REDSTONE_IO = ITEMS.registerSimpleBlockItem(
        ModContentIds.REDSTONE_IO,
        ModBlocks.REDSTONE_IO);

    public static final DeferredItem<BlockItem> TRANSPOSER = ITEMS.registerSimpleBlockItem(
        ModContentIds.TRANSPOSER,
        ModBlocks.TRANSPOSER);

    public static final DeferredItem<BlockItem> WAYPOINT = ITEMS.registerSimpleBlockItem(
        ModContentIds.WAYPOINT,
        ModBlocks.WAYPOINT);

    public static void register(final IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    private ModItems() {
    }
}
