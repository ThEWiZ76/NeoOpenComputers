package li.cil.oc.common;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.block.ComputerCaseBlock;
import li.cil.oc.common.block.KeyboardBlock;
import li.cil.oc.common.block.ScreenBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NeoOpenComputers.MODID);

    public static final DeferredBlock<Block> COMPUTER_CASE_TIER1 = BLOCKS.register(
        ModContentIds.COMPUTER_CASE_TIER1,
        () -> new ComputerCaseBlock(computerCaseProperties()));

    public static final DeferredBlock<Block> SCREEN_TIER1 = BLOCKS.register(
        ModContentIds.SCREEN_TIER1,
        () -> new ScreenBlock(screenProperties()));

    public static final DeferredBlock<Block> KEYBOARD = BLOCKS.register(
        ModContentIds.KEYBOARD,
        () -> new KeyboardBlock(keyboardProperties()));

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

    private static BlockBehaviour.Properties screenProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .strength(1.5F, 4.0F);
    }

    private static BlockBehaviour.Properties keyboardProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY)
            .strength(1.0F, 3.0F);
    }
}
