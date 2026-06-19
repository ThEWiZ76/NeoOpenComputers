package li.cil.oc.api.prefab;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PrefabDriverItemTest {
    @Test
    void exposesModernDriverItemSignatures() throws NoSuchMethodException {
        Method worksWith = DriverItem.class.getMethod("worksWith", ItemStack.class);
        Method createEnvironment = DriverItem.class.getMethod("createEnvironment", ItemStack.class, EnvironmentHost.class);
        Method dataTag = DriverItem.class.getMethod("dataTag", ItemStack.class);

        assertArrayEquals(new Class<?>[]{ItemStack.class}, worksWith.getParameterTypes());
        assertArrayEquals(new Class<?>[]{ItemStack.class, EnvironmentHost.class}, createEnvironment.getParameterTypes());
        assertEquals(ManagedEnvironment.class, createEnvironment.getReturnType());
        assertEquals(CompoundTag.class, dataTag.getReturnType());
    }

    @Test
    void defaultsAreNullSafeWithoutMinecraftRegistryBootstrap() {
        TestDriverItem driver = new TestDriverItem((ItemStack[]) null);

        assertFalse(driver.worksWith(null));
        assertEquals(0, driver.tier(null));
        assertTrue(driver.dataTag(null).isEmpty());
        assertNull(driver.createEnvironment(null, null));
    }

    @Test
    void exposesHostTypeHelpers() {
        TestDriverItem driver = new TestDriverItem((ItemStack[]) null);

        assertTrue(driver.isAdapter(AdapterHost.class));
        assertTrue(driver.isComputer(li.cil.oc.api.internal.Case.class));
        assertTrue(driver.isRobot(li.cil.oc.api.internal.Robot.class));
        assertTrue(driver.isRotatable(RotatableHost.class));
        assertTrue(driver.isServer(li.cil.oc.api.internal.Server.class));
        assertTrue(driver.isTablet(li.cil.oc.api.internal.Tablet.class));
    }

    private static final class TestDriverItem extends DriverItem {
        private TestDriverItem(final ItemStack... items) {
            super(items);
        }

        @Override
        public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
            return null;
        }

        @Override
        public String slot(final ItemStack stack) {
            return "card";
        }
    }

    private interface AdapterHost extends EnvironmentHost, li.cil.oc.api.internal.Adapter {
    }

    private interface RotatableHost extends EnvironmentHost, li.cil.oc.api.internal.Rotatable {
    }
}
