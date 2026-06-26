package li.cil.oc.common;

import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.driver.CompoundBlockDriver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ModDriverCatalogTest {
    @Test
    void registersInitialItemDriversInOrder() {
        final DriverRegistry registry = new DriverRegistry();
        final TestDriver first = new TestDriver();
        final TestDriver second = new TestDriver();

        ModDriverCatalog.register(registry, first, second);

        final ArrayList<DriverItem> drivers = new ArrayList<>(registry.itemDrivers());
        assertEquals(2, drivers.size());
        assertSame(first, drivers.get(0));
        assertSame(second, drivers.get(1));
    }

    @Test
    void registersInitialBlockDriversInOrder() {
        final DriverRegistry registry = new DriverRegistry();
        final TestBlockDriver first = new TestBlockDriver();

        ModDriverCatalog.registerBlocks(registry, first);

        final DriverBlock driver = registry.driverFor(null, BlockPos.ZERO, Direction.NORTH);
        assertInstanceOf(CompoundBlockDriver.class, driver);
        assertTrue(driver.worksWith(null, BlockPos.ZERO, Direction.NORTH));
    }

    private static final class TestBlockDriver implements DriverBlock {
        @Override
        public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
            return true;
        }

        @Override
        public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
            return null;
        }
    }

    private static final class TestDriver implements DriverItem {
        @Override
        public boolean worksWith(final ItemStack stack) {
            return false;
        }

        @Override
        public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
            return null;
        }

        @Override
        public String slot(final ItemStack stack) {
            return "test";
        }

        @Override
        public int tier(final ItemStack stack) {
            return 0;
        }

        @Override
        public CompoundTag dataTag(final ItemStack stack) {
            return null;
        }
    }
}
