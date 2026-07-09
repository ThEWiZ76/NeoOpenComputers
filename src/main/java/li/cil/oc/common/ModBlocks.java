package li.cil.oc.common;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.block.AdapterBlock;
import li.cil.oc.common.block.AssemblerBlock;
import li.cil.oc.common.block.CableBlock;
import li.cil.oc.common.block.ChargerBlock;
import li.cil.oc.common.block.ChameliumBlock;
import li.cil.oc.common.block.ComputerCaseBlock;
import li.cil.oc.common.block.DisassemblerBlock;
import li.cil.oc.common.block.DiskDriveBlock;
import li.cil.oc.common.block.GeolyzerBlock;
import li.cil.oc.common.block.HologramBlock;
import li.cil.oc.common.block.KeyboardBlock;
import li.cil.oc.common.block.MicrocontrollerBlock;
import li.cil.oc.common.block.MotionSensorBlock;
import li.cil.oc.common.block.NetSplitterBlock;
import li.cil.oc.common.block.PowerConverterBlock;
import li.cil.oc.common.block.PowerDistributorBlock;
import li.cil.oc.common.block.PrintBlock;
import li.cil.oc.common.block.PrinterBlock;
import li.cil.oc.common.block.RackBlock;
import li.cil.oc.common.block.RaidBlock;
import li.cil.oc.common.block.RedstoneIoBlock;
import li.cil.oc.common.block.RelayBlock;
import li.cil.oc.common.block.RobotBlock;
import li.cil.oc.common.block.ScreenBlock;
import li.cil.oc.common.block.TransposerBlock;
import li.cil.oc.common.block.WaypointBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NeoOpenComputers.MODID);

    public static final DeferredBlock<Block> ADAPTER = BLOCKS.register(
        ModContentIds.ADAPTER,
        () -> new AdapterBlock(adapterProperties()));

    public static final DeferredBlock<Block> ASSEMBLER = BLOCKS.register(
        ModContentIds.ASSEMBLER,
        () -> new AssemblerBlock(assemblerProperties()));

    public static final DeferredBlock<Block> CABLE = BLOCKS.register(
        ModContentIds.CABLE,
        () -> new CableBlock(cableProperties()));

    public static final DeferredBlock<Block> CHARGER = BLOCKS.register(
        ModContentIds.CHARGER,
        () -> new ChargerBlock(networkInfrastructureProperties()));

    public static final DeferredBlock<Block> CHAMELIUM_BLOCK = BLOCKS.register(
        ModContentIds.CHAMELIUM_BLOCK,
        () -> new ChameliumBlock(chameliumBlockProperties()));

    public static final DeferredBlock<Block> COMPUTER_CASE_TIER1 = BLOCKS.register(
        ModContentIds.COMPUTER_CASE_TIER1,
        () -> new ComputerCaseBlock(computerCaseProperties(), 0));

    public static final DeferredBlock<Block> COMPUTER_CASE_TIER2 = BLOCKS.register(
        ModContentIds.COMPUTER_CASE_TIER2,
        () -> new ComputerCaseBlock(computerCaseProperties(), 1));

    public static final DeferredBlock<Block> COMPUTER_CASE_TIER3 = BLOCKS.register(
        ModContentIds.COMPUTER_CASE_TIER3,
        () -> new ComputerCaseBlock(computerCaseProperties(), 2));

    public static final DeferredBlock<Block> DISASSEMBLER = BLOCKS.register(
        ModContentIds.DISASSEMBLER,
        () -> new DisassemblerBlock(disassemblerProperties()));

    public static final DeferredBlock<Block> DISK_DRIVE = BLOCKS.register(
        ModContentIds.DISK_DRIVE,
        () -> new DiskDriveBlock(diskDriveProperties()));

    public static final DeferredBlock<Block> ENDSTONE = BLOCKS.register(
        ModContentIds.ENDSTONE,
        () -> new Block(endstoneProperties()));

    public static final DeferredBlock<Block> GEOLYZER = BLOCKS.register(
        ModContentIds.GEOLYZER,
        () -> new GeolyzerBlock(geolyzerProperties()));

    public static final DeferredBlock<Block> HOLOGRAM_TIER1 = BLOCKS.register(
        ModContentIds.HOLOGRAM_TIER1,
        () -> new HologramBlock(hologramProperties(), 0));

    public static final DeferredBlock<Block> HOLOGRAM_TIER2 = BLOCKS.register(
        ModContentIds.HOLOGRAM_TIER2,
        () -> new HologramBlock(hologramProperties(), 1));

    public static final DeferredBlock<Block> SCREEN_TIER1 = BLOCKS.register(
        ModContentIds.SCREEN_TIER1,
        () -> new ScreenBlock(screenProperties(), 0));

    public static final DeferredBlock<Block> SCREEN_TIER2 = BLOCKS.register(
        ModContentIds.SCREEN_TIER2,
        () -> new ScreenBlock(screenProperties(), 1));

    public static final DeferredBlock<Block> SCREEN_TIER3 = BLOCKS.register(
        ModContentIds.SCREEN_TIER3,
        () -> new ScreenBlock(screenProperties(), 2));

    public static final DeferredBlock<Block> KEYBOARD = BLOCKS.register(
        ModContentIds.KEYBOARD,
        () -> new KeyboardBlock(keyboardProperties()));

    public static final DeferredBlock<Block> MICROCONTROLLER_TIER1 = BLOCKS.register(
        ModContentIds.MICROCONTROLLER_TIER1,
        () -> new MicrocontrollerBlock(microcontrollerProperties(), 0));

    public static final DeferredBlock<Block> MICROCONTROLLER_TIER2 = BLOCKS.register(
        ModContentIds.MICROCONTROLLER_TIER2,
        () -> new MicrocontrollerBlock(microcontrollerProperties(), 1));

    public static final DeferredBlock<Block> MICROCONTROLLER_CREATIVE = BLOCKS.register(
        ModContentIds.MICROCONTROLLER_CREATIVE,
        () -> new MicrocontrollerBlock(microcontrollerProperties(), 3));

    public static final DeferredBlock<Block> MOTION_SENSOR = BLOCKS.register(
        ModContentIds.MOTION_SENSOR,
        () -> new MotionSensorBlock(motionSensorProperties()));

    public static final DeferredBlock<Block> POWER_DISTRIBUTOR = BLOCKS.register(
        ModContentIds.POWER_DISTRIBUTOR,
        () -> new PowerDistributorBlock(networkInfrastructureProperties()));

    public static final DeferredBlock<Block> POWER_CONVERTER = BLOCKS.register(
        ModContentIds.POWER_CONVERTER,
        () -> new PowerConverterBlock(networkInfrastructureProperties()));

    public static final DeferredBlock<Block> PRINT = BLOCKS.register(
        ModContentIds.PRINT,
        () -> new PrintBlock(printProperties()));

    public static final DeferredBlock<Block> PRINTER = BLOCKS.register(
        ModContentIds.PRINTER,
        () -> new PrinterBlock(printerProperties()));

    public static final DeferredBlock<Block> RACK = BLOCKS.register(
        ModContentIds.RACK,
        () -> new RackBlock(rackProperties()));

    public static final DeferredBlock<Block> RAID = BLOCKS.register(
        ModContentIds.RAID,
        () -> new RaidBlock(raidProperties()));

    public static final DeferredBlock<Block> REDSTONE_IO = BLOCKS.register(
        ModContentIds.REDSTONE_IO,
        () -> new RedstoneIoBlock(redstoneIoProperties()));

    public static final DeferredBlock<Block> RELAY = BLOCKS.register(
        ModContentIds.RELAY,
        () -> new RelayBlock(networkInfrastructureProperties()));

    public static final DeferredBlock<Block> ROBOT = BLOCKS.register(
        ModContentIds.ROBOT,
        () -> new RobotBlock(robotProperties()));

    public static final DeferredBlock<Block> NET_SPLITTER = BLOCKS.register(
        ModContentIds.NET_SPLITTER,
        () -> new NetSplitterBlock(networkInfrastructureProperties()));

    public static final DeferredBlock<Block> TRANSPOSER = BLOCKS.register(
        ModContentIds.TRANSPOSER,
        () -> new TransposerBlock(transposerProperties()));

    public static final DeferredBlock<Block> WAYPOINT = BLOCKS.register(
        ModContentIds.WAYPOINT,
        () -> new WaypointBlock(waypointProperties()));

    public static void register(final IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }

    private ModBlocks() {
    }

    private static BlockBehaviour.Properties computerCaseProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(2.0F, 6.0F);
    }

    private static BlockBehaviour.Properties cableProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY)
            .strength(0.5F, 1.0F);
    }

    private static BlockBehaviour.Properties chameliumBlockProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .strength(1.5F, 6.0F);
    }

    private static BlockBehaviour.Properties adapterProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(1.5F, 4.0F);
    }

    private static BlockBehaviour.Properties assemblerProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(2.0F, 6.0F)
            .noOcclusion();
    }

    private static BlockBehaviour.Properties screenProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .strength(1.5F, 4.0F);
    }

    private static BlockBehaviour.Properties diskDriveProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(1.5F, 4.0F);
    }

    private static BlockBehaviour.Properties endstoneProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.SAND)
            .strength(3.0F, 9.0F);
    }

    private static BlockBehaviour.Properties disassemblerProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(2.0F, 6.0F)
            .noOcclusion();
    }

    private static BlockBehaviour.Properties geolyzerProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(2.0F, 6.0F);
    }

    private static BlockBehaviour.Properties keyboardProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY)
            .strength(1.0F, 3.0F)
            .noOcclusion();
    }

    private static BlockBehaviour.Properties microcontrollerProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(1.5F, 4.0F);
    }

    private static BlockBehaviour.Properties hologramProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(1.0F, 3.0F)
            .noOcclusion();
    }

    private static BlockBehaviour.Properties motionSensorProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(1.0F, 3.0F);
    }

    private static BlockBehaviour.Properties redstoneIoProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_RED)
            .strength(1.5F, 4.0F);
    }

    private static BlockBehaviour.Properties networkInfrastructureProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(1.5F, 4.0F);
    }

    private static BlockBehaviour.Properties robotProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(1.5F, 4.0F)
            .noOcclusion();
    }

    private static BlockBehaviour.Properties printProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_LIGHT_GRAY)
            .strength(1.0F, 3.0F)
            .dynamicShape()
            .noOcclusion();
    }

    private static BlockBehaviour.Properties printerProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(2.0F, 6.0F)
            .noOcclusion();
    }

    private static BlockBehaviour.Properties rackProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(2.0F, 6.0F);
    }

    private static BlockBehaviour.Properties raidProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(1.5F, 4.0F);
    }

    private static BlockBehaviour.Properties transposerProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(1.5F, 4.0F);
    }

    private static BlockBehaviour.Properties waypointProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PURPLE)
            .strength(1.0F, 3.0F)
            .noOcclusion();
    }
}
