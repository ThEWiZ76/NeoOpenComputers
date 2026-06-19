package li.cil.oc.common;

import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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
