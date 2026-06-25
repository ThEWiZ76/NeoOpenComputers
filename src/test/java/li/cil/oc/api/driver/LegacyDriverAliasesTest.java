package li.cil.oc.api.driver;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LegacyDriverAliasesTest {
    @Test
    void deprecatedDriverNamesBridgeToModernDriverContracts() throws ClassNotFoundException {
        Class<?> block = Class.forName("li.cil.oc.api.driver.Block");
        Class<?> sidedBlock = Class.forName("li.cil.oc.api.driver.SidedBlock");
        Class<?> item = Class.forName("li.cil.oc.api.driver.Item");
        Class<?> environmentHost = Class.forName("li.cil.oc.api.driver.EnvironmentHost");

        assertTrue(DriverBlock.class.isAssignableFrom(block));
        assertTrue(DriverBlock.class.isAssignableFrom(sidedBlock));
        assertTrue(DriverItem.class.isAssignableFrom(item));
        assertTrue(li.cil.oc.api.network.EnvironmentHost.class.isAssignableFrom(environmentHost));
    }

    @Test
    void deprecatedBlockDriverKeepsSideAgnosticModernConvenienceMethods() throws Exception {
        Class<?> block = Class.forName("li.cil.oc.api.driver.Block");

        assertArrayEquals(new Class<?>[]{Level.class, BlockPos.class}, block.getMethod("worksWith", Level.class, BlockPos.class).getParameterTypes());
        assertArrayEquals(new Class<?>[]{Level.class, BlockPos.class}, block.getMethod("createEnvironment", Level.class, BlockPos.class).getParameterTypes());
        assertArrayEquals(new Class<?>[]{Level.class, BlockPos.class, Direction.class}, block.getMethod("worksWith", Level.class, BlockPos.class, Direction.class).getParameterTypes());
        assertArrayEquals(new Class<?>[]{Level.class, BlockPos.class, Direction.class}, block.getMethod("createEnvironment", Level.class, BlockPos.class, Direction.class).getParameterTypes());
    }

    @Test
    void deprecatedEnvironmentAwareUsesModernItemStack() throws Exception {
        Class<?> environmentAware = Class.forName("li.cil.oc.api.driver.EnvironmentAware");

        assertArrayEquals(new Class<?>[]{ItemStack.class}, environmentAware.getMethod("providedEnvironment", ItemStack.class).getParameterTypes());
        assertEquals(Class.class, environmentAware.getMethod("providedEnvironment", ItemStack.class).getReturnType());
    }
}
