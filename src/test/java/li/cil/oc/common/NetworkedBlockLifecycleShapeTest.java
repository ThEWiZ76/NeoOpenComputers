package li.cil.oc.common;

import li.cil.oc.common.block.ComputerCaseBlock;
import li.cil.oc.common.block.DiskDriveBlock;
import li.cil.oc.common.block.HologramBlock;
import li.cil.oc.common.block.KeyboardBlock;
import li.cil.oc.common.block.MotionSensorBlock;
import li.cil.oc.common.block.RedstoneIoBlock;
import li.cil.oc.common.block.ScreenBlock;
import li.cil.oc.common.block.TransposerBlock;
import li.cil.oc.common.block.WaypointBlock;
import li.cil.oc.common.block.AssemblerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class NetworkedBlockLifecycleShapeTest {
    @Test
    void networkedBlocksHandlePlacementAndNeighborChanges() throws NoSuchMethodException {
        assertNetworkLifecycleHooks(ComputerCaseBlock.class);
        assertNetworkLifecycleHooks(AssemblerBlock.class);
        assertNetworkLifecycleHooks(DiskDriveBlock.class);
        assertNetworkLifecycleHooks(ScreenBlock.class);
        assertNetworkLifecycleHooks(KeyboardBlock.class);
        assertNetworkLifecycleHooks(HologramBlock.class);
        assertNetworkLifecycleHooks(MotionSensorBlock.class);
        assertNetworkLifecycleHooks(RedstoneIoBlock.class);
        assertNetworkLifecycleHooks(TransposerBlock.class);
        assertNetworkLifecycleHooks(WaypointBlock.class);
    }

    @Test
    void diskDriveHandlesItemOnBlockInteraction() throws NoSuchMethodException {
        Method useItemOn = DiskDriveBlock.class.getDeclaredMethod(
            "useItemOn",
            ItemStack.class,
            BlockState.class,
            Level.class,
            BlockPos.class,
            Player.class,
            InteractionHand.class,
            BlockHitResult.class);

        assertEquals(ItemInteractionResult.class, useItemOn.getReturnType());
    }

    private static void assertNetworkLifecycleHooks(final Class<?> blockClass) throws NoSuchMethodException {
        final Method onPlace = blockClass.getDeclaredMethod(
            "onPlace",
            BlockState.class,
            Level.class,
            BlockPos.class,
            BlockState.class,
            boolean.class);
        final Method neighborChanged = blockClass.getDeclaredMethod(
            "neighborChanged",
            BlockState.class,
            Level.class,
            BlockPos.class,
            Block.class,
            BlockPos.class,
            boolean.class);

        assertEquals(void.class, onPlace.getReturnType());
        assertEquals(void.class, neighborChanged.getReturnType());
    }
}
