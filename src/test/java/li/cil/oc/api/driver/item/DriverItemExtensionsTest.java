package li.cil.oc.api.driver.item;

import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DriverItemExtensionsTest {
    @Test
    void chargeableAndCallBudgetUseModernItemStacks() {
        TestItemDriver driver = new TestItemDriver();
        ItemStack stack = null;

        assertTrue(driver.canCharge(stack));
        assertEquals(1.5, driver.charge(stack, 2.0, true));
        assertEquals(0.75, driver.getCallBudget(stack));
    }

    @Test
    void driverItemExtensionsDescribeSlotsTiersMemoryAndInventory() {
        TestItemDriver driver = new TestItemDriver();
        ItemStack stack = null;

        assertInstanceOf(DriverItem.class, driver);
        assertTrue(driver.worksWith(stack));
        assertNull(driver.createEnvironment(stack, null));
        assertEquals("container", driver.slot(stack));
        assertEquals(1, driver.tier(stack));
        assertEquals("upgrade", driver.providedSlot(stack));
        assertEquals(2, driver.providedTier(stack));
        assertEquals(512.0, driver.amount(stack));
        assertEquals(16, driver.inventoryCapacity(stack));
        assertTrue(driver.worksWith(stack, EnvironmentHost.class));
    }

    private static final class TestItemDriver implements Chargeable, CallBudget, Container, HostAware, Memory, Inventory {
        @Override
        public boolean canCharge(final ItemStack stack) {
            return true;
        }

        @Override
        public double charge(final ItemStack stack, final double amount, final boolean simulate) {
            return amount - 0.5;
        }

        @Override
        public double getCallBudget(final ItemStack stack) {
            return 0.75;
        }

        @Override
        public boolean worksWith(final ItemStack stack) {
            return true;
        }

        @Override
        public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
            return null;
        }

        @Override
        public String slot(final ItemStack stack) {
            return "container";
        }

        @Override
        public int tier(final ItemStack stack) {
            return 1;
        }

        @Override
        public CompoundTag dataTag(final ItemStack stack) {
            return null;
        }

        @Override
        public String providedSlot(final ItemStack stack) {
            return "upgrade";
        }

        @Override
        public int providedTier(final ItemStack stack) {
            return 2;
        }

        @Override
        public boolean worksWith(final ItemStack stack, final Class<? extends EnvironmentHost> host) {
            return host == EnvironmentHost.class;
        }

        @Override
        public double amount(final ItemStack stack) {
            return 512;
        }

        @Override
        public int inventoryCapacity(final ItemStack stack) {
            return 16;
        }
    }
}
