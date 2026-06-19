package li.cil.oc.api.prefab;

import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

final class DriverPrefabTest {
    @Test
    void sidedTileEntityDriverUsesModernLevelBlockPosDirectionTypes() throws NoSuchMethodException {
        Method worksWith = DriverSidedTileEntity.class.getMethod("worksWith", Level.class, BlockPos.class, Direction.class);

        assertArrayEquals(new Class<?>[]{Level.class, BlockPos.class, Direction.class}, worksWith.getParameterTypes());
        assertFalse(new TestTileEntityDriver(null).worksWith(null, BlockPos.ZERO, Direction.NORTH));
        assertFalse(new TestTileEntityDriver(BlockEntity.class).worksWith(null, BlockPos.ZERO, Direction.NORTH));
    }

    @Test
    void sidedBlockDriverUsesModernLevelBlockPosDirectionTypes() throws NoSuchMethodException {
        Method worksWith = DriverSidedBlock.class.getMethod("worksWith", Level.class, BlockPos.class, Direction.class);

        assertArrayEquals(new Class<?>[]{Level.class, BlockPos.class, Direction.class}, worksWith.getParameterTypes());
        assertFalse(new TestSidedBlockDriver().worksWith(null, BlockPos.ZERO, Direction.NORTH));
    }

    private static final class TestTileEntityDriver extends DriverSidedTileEntity {
        private final Class<?> blockEntityClass;

        private TestTileEntityDriver(final Class<?> blockEntityClass) {
            this.blockEntityClass = blockEntityClass;
        }

        @Override
        public Class<?> getTileEntityClass() {
            return blockEntityClass;
        }

        @Override
        public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
            return null;
        }
    }

    private static final class TestSidedBlockDriver extends DriverSidedBlock {
        private TestSidedBlockDriver() {
            super((ItemStack[]) null);
        }

        @Override
        public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
            return null;
        }
    }
}
