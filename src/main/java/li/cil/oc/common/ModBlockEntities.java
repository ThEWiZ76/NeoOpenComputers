package li.cil.oc.common;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.blockentity.KeyboardBlockEntity;
import li.cil.oc.common.blockentity.ScreenBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NeoOpenComputers.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ComputerCaseBlockEntity>> COMPUTER_CASE =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.COMPUTER_CASE_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(ComputerCaseBlockEntity::new, ModBlocks.COMPUTER_CASE_TIER1.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ScreenBlockEntity>> SCREEN =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.SCREEN_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(ScreenBlockEntity::new, ModBlocks.SCREEN_TIER1.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KeyboardBlockEntity>> KEYBOARD =
        BLOCK_ENTITY_TYPES.register(
            ModContentIds.KEYBOARD_BLOCK_ENTITY,
            () -> BlockEntityType.Builder.of(KeyboardBlockEntity::new, ModBlocks.KEYBOARD.get()).build(null));

    public static void register(final IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }

    private ModBlockEntities() {
    }
}
