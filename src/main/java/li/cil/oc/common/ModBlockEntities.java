package li.cil.oc.common;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.blockentity.AdapterBlockEntity;
import li.cil.oc.common.blockentity.CableBlockEntity;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.blockentity.DiskDriveBlockEntity;
import li.cil.oc.common.blockentity.GeolyzerBlockEntity;
import li.cil.oc.common.blockentity.KeyboardBlockEntity;
import li.cil.oc.common.blockentity.MotionSensorBlockEntity;
import li.cil.oc.common.blockentity.RedstoneIoBlockEntity;
import li.cil.oc.common.blockentity.ScreenBlockEntity;
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

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CableBlockEntity>> CABLE =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.CABLE_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(CableBlockEntity::new, ModBlocks.CABLE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ComputerCaseBlockEntity>> COMPUTER_CASE =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.COMPUTER_CASE_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(
                ComputerCaseBlockEntity::new,
                ModBlocks.COMPUTER_CASE_TIER1.get(),
                ModBlocks.COMPUTER_CASE_TIER2.get(),
                ModBlocks.COMPUTER_CASE_TIER3.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DiskDriveBlockEntity>> DISK_DRIVE =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.DISK_DRIVE_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(DiskDriveBlockEntity::new, ModBlocks.DISK_DRIVE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeolyzerBlockEntity>> GEOLYZER =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.GEOLYZER_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(GeolyzerBlockEntity::new, ModBlocks.GEOLYZER.get()).build(null));

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

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MotionSensorBlockEntity>> MOTION_SENSOR =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.MOTION_SENSOR_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(MotionSensorBlockEntity::new, ModBlocks.MOTION_SENSOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RedstoneIoBlockEntity>> REDSTONE_IO =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.REDSTONE_IO_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(RedstoneIoBlockEntity::new, ModBlocks.REDSTONE_IO.get()).build(null));

    public static void register(final IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }

    private ModBlockEntities() {
    }
}
