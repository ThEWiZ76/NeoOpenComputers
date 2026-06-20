package li.cil.oc.common;

import li.cil.oc.common.block.ComputerCaseBlock;
import li.cil.oc.common.block.DiskDriveBlock;
import li.cil.oc.common.block.KeyboardBlock;
import li.cil.oc.common.block.ScreenBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class NetworkedBlockLifecycleShapeTest {
    @Test
    void networkedBlocksHandlePlacementAndNeighborChanges() throws NoSuchMethodException {
        assertNetworkLifecycleHooks(ComputerCaseBlock.class);
        assertNetworkLifecycleHooks(DiskDriveBlock.class);
        assertNetworkLifecycleHooks(ScreenBlock.class);
        assertNetworkLifecycleHooks(KeyboardBlock.class);
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
