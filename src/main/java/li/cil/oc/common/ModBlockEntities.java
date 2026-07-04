package li.cil.oc.common;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.blockentity.AdapterBlockEntity;
import li.cil.oc.common.blockentity.AssemblerBlockEntity;
import li.cil.oc.common.blockentity.CableBlockEntity;
import li.cil.oc.common.blockentity.ChargerBlockEntity;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.blockentity.DisassemblerBlockEntity;
import li.cil.oc.common.blockentity.DiskDriveBlockEntity;
import li.cil.oc.common.blockentity.GeolyzerBlockEntity;
import li.cil.oc.common.blockentity.HologramBlockEntity;
import li.cil.oc.common.blockentity.KeyboardBlockEntity;
import li.cil.oc.common.blockentity.MicrocontrollerBlockEntity;
import li.cil.oc.common.blockentity.MotionSensorBlockEntity;
import li.cil.oc.common.blockentity.NetSplitterBlockEntity;
import li.cil.oc.common.blockentity.PowerConverterBlockEntity;
import li.cil.oc.common.blockentity.PowerDistributorBlockEntity;
import li.cil.oc.common.blockentity.PrintBlockEntity;
import li.cil.oc.common.blockentity.PrinterBlockEntity;
import li.cil.oc.common.blockentity.RackBlockEntity;
import li.cil.oc.common.blockentity.RaidBlockEntity;
import li.cil.oc.common.blockentity.RedstoneIoBlockEntity;
import li.cil.oc.common.blockentity.RelayBlockEntity;
import li.cil.oc.common.blockentity.ScreenBlockEntity;
import li.cil.oc.common.blockentity.TransposerBlockEntity;
import li.cil.oc.common.blockentity.WaypointBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NeoOpenComputers.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AdapterBlockEntity>> ADAPTER =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.ADAPTER_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(AdapterBlockEntity::new, ModBlocks.ADAPTER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AssemblerBlockEntity>> ASSEMBLER =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.ASSEMBLER_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(AssemblerBlockEntity::new, ModBlocks.ASSEMBLER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CableBlockEntity>> CABLE =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.CABLE_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(CableBlockEntity::new, ModBlocks.CABLE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChargerBlockEntity>> CHARGER =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.CHARGER_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(ChargerBlockEntity::new, ModBlocks.CHARGER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ComputerCaseBlockEntity>> COMPUTER_CASE =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.COMPUTER_CASE_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(
                ComputerCaseBlockEntity::new,
                ModBlocks.COMPUTER_CASE_TIER1.get(),
                ModBlocks.COMPUTER_CASE_TIER2.get(),
                ModBlocks.COMPUTER_CASE_TIER3.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DisassemblerBlockEntity>> DISASSEMBLER =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.DISASSEMBLER_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(DisassemblerBlockEntity::new, ModBlocks.DISASSEMBLER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DiskDriveBlockEntity>> DISK_DRIVE =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.DISK_DRIVE_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(DiskDriveBlockEntity::new, ModBlocks.DISK_DRIVE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeolyzerBlockEntity>> GEOLYZER =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.GEOLYZER_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(GeolyzerBlockEntity::new, ModBlocks.GEOLYZER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HologramBlockEntity>> HOLOGRAM =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.HOLOGRAM_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(
                HologramBlockEntity::new,
                ModBlocks.HOLOGRAM_TIER1.get(),
                ModBlocks.HOLOGRAM_TIER2.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ScreenBlockEntity>> SCREEN =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.SCREEN_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(
                ScreenBlockEntity::new,
                ModBlocks.SCREEN_TIER1.get(),
                ModBlocks.SCREEN_TIER2.get(),
                ModBlocks.SCREEN_TIER3.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KeyboardBlockEntity>> KEYBOARD =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.KEYBOARD_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(KeyboardBlockEntity::new, ModBlocks.KEYBOARD.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MicrocontrollerBlockEntity>> MICROCONTROLLER =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.MICROCONTROLLER_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(
                MicrocontrollerBlockEntity::new,
                ModBlocks.MICROCONTROLLER_TIER1.get(),
                ModBlocks.MICROCONTROLLER_TIER2.get(),
                ModBlocks.MICROCONTROLLER_CREATIVE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MotionSensorBlockEntity>> MOTION_SENSOR =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.MOTION_SENSOR_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(MotionSensorBlockEntity::new, ModBlocks.MOTION_SENSOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerDistributorBlockEntity>> POWER_DISTRIBUTOR =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.POWER_DISTRIBUTOR_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(PowerDistributorBlockEntity::new, ModBlocks.POWER_DISTRIBUTOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerConverterBlockEntity>> POWER_CONVERTER =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.POWER_CONVERTER_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(PowerConverterBlockEntity::new, ModBlocks.POWER_CONVERTER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PrintBlockEntity>> PRINT =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.PRINT_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(PrintBlockEntity::new, ModBlocks.PRINT.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PrinterBlockEntity>> PRINTER =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.PRINTER_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(PrinterBlockEntity::new, ModBlocks.PRINTER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RackBlockEntity>> RACK =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.RACK_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(RackBlockEntity::new, ModBlocks.RACK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RaidBlockEntity>> RAID =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.RAID_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(RaidBlockEntity::new, ModBlocks.RAID.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RedstoneIoBlockEntity>> REDSTONE_IO =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.REDSTONE_IO_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(RedstoneIoBlockEntity::new, ModBlocks.REDSTONE_IO.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RelayBlockEntity>> RELAY =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.RELAY_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(RelayBlockEntity::new, ModBlocks.RELAY.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetSplitterBlockEntity>> NET_SPLITTER =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.NET_SPLITTER_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(NetSplitterBlockEntity::new, ModBlocks.NET_SPLITTER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TransposerBlockEntity>> TRANSPOSER =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.TRANSPOSER_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(TransposerBlockEntity::new, ModBlocks.TRANSPOSER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WaypointBlockEntity>> WAYPOINT =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.WAYPOINT_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(WaypointBlockEntity::new, ModBlocks.WAYPOINT.get()).build(null));

    public static void register(final IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }

    private ModBlockEntities() {
    }
}
