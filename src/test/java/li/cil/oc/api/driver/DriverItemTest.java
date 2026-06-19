package li.cil.oc.api.driver;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DriverItemTest {
    @Test
    void exposesModernItemStackEnvironmentHostAndCompoundTagSignatures() throws NoSuchMethodException {
        Method worksWith = DriverItem.class.getMethod("worksWith", ItemStack.class);
        Method createEnvironment = DriverItem.class.getMethod("createEnvironment", ItemStack.class, EnvironmentHost.class);
        Method dataTag = DriverItem.class.getMethod("dataTag", ItemStack.class);

        assertArrayEquals(new Class<?>[]{ItemStack.class}, worksWith.getParameterTypes());
        assertArrayEquals(new Class<?>[]{ItemStack.class, EnvironmentHost.class}, createEnvironment.getParameterTypes());
        assertEquals(ManagedEnvironment.class, createEnvironment.getReturnType());
        assertEquals(CompoundTag.class, dataTag.getReturnType());
    }

    @Test
    void driverItemCanDescribeSlotTierAndDataTag() {
        DriverItem driver = new TestDriverItem();
        ItemStack stack = null;

        assertTrue(driver.worksWith(stack));
        assertNull(driver.createEnvironment(stack, null));
        assertEquals("memory", driver.slot(stack));
        assertEquals(2, driver.tier(stack));
        assertEquals("value", driver.dataTag(stack).getString("key"));
    }

    private static final class TestDriverItem implements DriverItem {
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
            return "memory";
        }

        @Override
        public int tier(final ItemStack stack) {
            return 2;
        }

        @Override
        public CompoundTag dataTag(final ItemStack stack) {
            CompoundTag tag = new CompoundTag();
            tag.putString("key", "value");
            return tag;
        }
    }
}
