package li.cil.oc.api.driver;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DriverProviderTest {
    @Test
    void environmentProviderUsesModernItemStack() throws NoSuchMethodException {
        Method getEnvironment = EnvironmentProvider.class.getMethod("getEnvironment", ItemStack.class);

        assertArrayEquals(new Class<?>[]{ItemStack.class}, getEnvironment.getParameterTypes());
        assertSame(Class.class, getEnvironment.getReturnType());
        assertSame(String.class, ((EnvironmentProvider) stack -> String.class).getEnvironment(null));
    }

    @Test
    void inventoryProviderUsesModernItemStackPlayerAndContainer() throws NoSuchMethodException {
        Method worksWith = InventoryProvider.class.getMethod("worksWith", ItemStack.class, Player.class);
        Method getInventory = InventoryProvider.class.getMethod("getInventory", ItemStack.class, Player.class);
        InventoryProvider provider = new TestInventoryProvider();

        assertArrayEquals(new Class<?>[]{ItemStack.class, Player.class}, worksWith.getParameterTypes());
        assertEquals(Container.class, getInventory.getReturnType());
        assertTrue(provider.worksWith(null, null));
        assertNull(provider.getInventory(null, null));
    }

    @Test
    void namedBlockAndMethodWhitelistExposeNamingMetadata() {
        NamedBlock named = new TestNamedBlock();
        MethodWhitelist whitelist = () -> new String[]{"getEnergy"};

        assertEquals("energy_device", named.preferredName());
        assertEquals(10, named.priority());
        assertArrayEquals(new String[]{"getEnergy"}, whitelist.whitelistedMethods());
    }

    private static final class TestInventoryProvider implements InventoryProvider {
        @Override
        public boolean worksWith(final ItemStack stack, final Player player) {
            return true;
        }

        @Override
        public Container getInventory(final ItemStack stack, final Player player) {
            return null;
        }
    }

    private static final class TestNamedBlock implements NamedBlock {
        @Override
        public String preferredName() {
            return "energy_device";
        }

        @Override
        public int priority() {
            return 10;
        }
    }
}
