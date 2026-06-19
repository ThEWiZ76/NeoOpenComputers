package li.cil.oc.api.driver;

import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DriverBlockTest {
    @Test
    void exposesModernLevelBlockPosAndDirectionSignatures() throws NoSuchMethodException {
        Method worksWith = DriverBlock.class.getMethod("worksWith", Level.class, BlockPos.class, Direction.class);
        Method createEnvironment = DriverBlock.class.getMethod("createEnvironment", Level.class, BlockPos.class, Direction.class);

        assertArrayEquals(new Class<?>[]{Level.class, BlockPos.class, Direction.class}, worksWith.getParameterTypes());
        assertArrayEquals(new Class<?>[]{Level.class, BlockPos.class, Direction.class}, createEnvironment.getParameterTypes());
        assertEquals(ManagedEnvironment.class, createEnvironment.getReturnType());
    }

    @Test
    void driverBlockCanMatchAndCreateEnvironment() {
        DriverBlock driver = new TestDriverBlock();

        assertTrue(driver.worksWith(null, BlockPos.ZERO, Direction.NORTH));
        assertNull(driver.createEnvironment(null, BlockPos.ZERO, Direction.NORTH));
    }

    private static final class TestDriverBlock implements DriverBlock {
        @Override
        public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
            return side == Direction.NORTH;
        }

        @Override
        public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
            return null;
        }
    }
}
