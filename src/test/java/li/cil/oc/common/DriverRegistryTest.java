package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.InventoryProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DriverRegistryTest {
    @AfterEach
    void resetApi() {
        API.driver = null;
    }

    @Test
    void bootstrapInstallsDriverApi() {
        OpenComputersApi.initialize();

        assertTrue(API.driver instanceof DriverRegistry);
    }

    @Test
    void findsRegisteredBlockAndItemDriversInOrder() {
        DriverRegistry registry = new DriverRegistry();
        TestDriverBlock block = new TestDriverBlock(true);
        TestDriverItem item = new TestDriverItem(true);

        registry.add(new TestDriverBlock(false));
        registry.add(block);
        registry.add(new TestDriverItem(false));
        registry.add(item);

        assertSame(block, registry.driverFor(null, BlockPos.ZERO, Direction.NORTH));
        assertSame(item, registry.driverFor((ItemStack) null));
        assertEquals(2, registry.itemDrivers().size());
    }

    @Test
    void hostAwareItemDriverCanFilterByHost() {
        DriverRegistry registry = new DriverRegistry();
        TestHostAwareDriver item = new TestHostAwareDriver();
        registry.add(item);

        assertSame(item, registry.driverFor(null, TestHost.class));
        assertNull(registry.driverFor(null, EnvironmentHost.class));
    }

    @Test
    void hostAwareStackMatchPreventsGenericFallbackForWrongHostLikeUpstream() {
        DriverRegistry registry = new DriverRegistry();
        TestHostAwareDriver hostAware = new TestHostAwareDriver();
        TestDriverItem generic = new TestDriverItem(true);
        registry.add(hostAware);
        registry.add(generic);

        assertNull(registry.driverFor(null, EnvironmentHost.class));
        assertSame(hostAware, registry.driverFor(null, TestHost.class));
    }

    @Test
    void environmentProvidersReturnAllMatches() {
        DriverRegistry registry = new DriverRegistry();
        EnvironmentProvider first = stack -> String.class;
        EnvironmentProvider second = stack -> Integer.class;

        registry.add(first);
        registry.add(second);

        assertEquals(String.class, registry.environmentFor(null));
        assertEquals(Set.of(String.class, Integer.class), registry.environmentsFor(null));
    }

    @Test
    void converterAndInventoryProvidersCanBeRegistered() {
        DriverRegistry registry = new DriverRegistry();
        Converter converter = (value, output) -> output.put("key", "value");
        InventoryProvider inventoryProvider = new TestInventoryProvider();

        registry.add(converter);
        registry.add(inventoryProvider);

        assertNull(registry.itemHandlerFor(null, null));
        assertEquals(1, registry.converterCount());
    }

    private record TestDriverBlock(boolean matches) implements DriverBlock {
        @Override
        public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
            return matches;
        }

        @Override
        public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
            return null;
        }
    }

    private static class TestDriverItem implements DriverItem {
        private final boolean matches;

        private TestDriverItem(final boolean matches) {
            this.matches = matches;
        }

        @Override
        public boolean worksWith(final ItemStack stack) {
            return matches;
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

    private static final class TestHostAwareDriver extends TestDriverItem implements HostAware {
        private TestHostAwareDriver() {
            super(true);
        }

        @Override
        public boolean worksWith(final ItemStack stack, final Class<? extends EnvironmentHost> host) {
            return host == TestHost.class;
        }
    }

    private interface TestHost extends EnvironmentHost {
    }

    private static final class TestInventoryProvider implements InventoryProvider {
        @Override
        public boolean worksWith(final ItemStack stack, final Player player) {
            return false;
        }

        @Override
        public net.minecraft.world.Container getInventory(final ItemStack stack, final Player player) {
            return null;
        }
    }
}
