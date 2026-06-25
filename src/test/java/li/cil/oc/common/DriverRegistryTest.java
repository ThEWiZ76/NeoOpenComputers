package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.InventoryProvider;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.driver.MinecraftConverters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DriverRegistryTest {
    @AfterEach
    void resetApi() {
        API.driver = null;
        API.network = null;
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
    void matchingBlockDriversAreCombinedLikeUpstream() throws Exception {
        API.network = new NetworkRegistry();
        DriverRegistry registry = new DriverRegistry();
        TestCallbackBlockDriver first = new TestCallbackBlockDriver("alpha", "first", 1);
        TestCallbackBlockDriver second = new TestCallbackBlockDriver("beta", "second", 2);

        registry.add(first);
        registry.add(second);

        DriverBlock driver = registry.driverFor(null, BlockPos.ZERO, Direction.NORTH);
        assertTrue(driver.worksWith(null, BlockPos.ZERO, Direction.NORTH));

        ManagedEnvironment environment = driver.createEnvironment(null, BlockPos.ZERO, Direction.NORTH);
        assertTrue(environment.node() instanceof Component);
        Component component = (Component) environment.node();
        assertEquals("beta", component.name());
        assertTrue(component.methods().contains("first"));
        assertTrue(component.methods().contains("second"));
        assertEquals("first", component.invoke("first", null)[0]);
        assertEquals("second", component.invoke("second", null)[0]);
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

    @Test
    void converterRecursivelyNormalizesUnknownValuesLikeUpstream() {
        DriverRegistry registry = new DriverRegistry();
        TestConvertedValue value = new TestConvertedValue("root");
        TestConvertedValue nested = new TestConvertedValue("nested");
        registry.add((candidate, output) -> {
            if (candidate instanceof TestConvertedValue converted) {
                output.put("name", converted.name);
                output.put("self", candidate);
                output.put("nested", nested);
            }
        });

        Object[] converted = registry.convert(new Object[]{value});

        Map<?, ?> convertedMap = assertInstanceOf(Map.class, converted[0]);
        assertEquals("root", convertedMap.get("name"));
        assertSame(convertedMap, convertedMap.get("self"));
        Map<?, ?> nestedMap = assertInstanceOf(Map.class, convertedMap.get("nested"));
        assertEquals("nested", nestedMap.get("name"));
        assertSame(nestedMap, nestedMap.get("self"));
    }

    @Test
    void converterFlattenKeyReplacesConvertedValueLikeUpstream() {
        DriverRegistry registry = new DriverRegistry();
        TestConvertedValue value = new TestConvertedValue("flat");
        registry.add((candidate, output) -> {
            if (candidate == value) {
                output.put("oc:flatten", "flattened");
            }
        });

        assertArrayEquals(new Object[]{"flattened"}, registry.convert(new Object[]{value}));
    }

    @Test
    void unknownValuesFallBackToStringLikeUpstream() {
        DriverRegistry registry = new DriverRegistry();

        assertArrayEquals(new Object[]{"converted-value:plain"}, registry.convert(new Object[]{new TestConvertedValue("plain")}));
    }

    @Test
    void minecraftNbtConverterFlattensCompoundTagsLikeUpstream() {
        DriverRegistry registry = new DriverRegistry();
        registry.add(MinecraftConverters.NBT);
        CompoundTag root = new CompoundTag();
        root.putByte("byte", (byte) 3);
        root.putShort("short", (short) 4);
        root.putInt("int", 5);
        root.putLong("long", 6L);
        root.putFloat("float", 7.5F);
        root.putDouble("double", 8.5D);
        root.putString("string", "hello");
        root.putByteArray("bytes", new byte[]{1, 2});
        root.putIntArray("ints", new int[]{3, 4});
        root.putLongArray("longs", new long[]{5L, 6L});

        ListTag list = new ListTag();
        list.add(StringTag.valueOf("first"));
        list.add(StringTag.valueOf("second"));
        root.put("list", list);

        CompoundTag nested = new CompoundTag();
        nested.put("value", IntTag.valueOf(9));
        root.put("nested", nested);

        Object[] converted = registry.convert(new Object[]{root});

        Map<?, ?> map = assertInstanceOf(Map.class, converted[0]);
        assertEquals((byte) 3, map.get("byte"));
        assertEquals((short) 4, map.get("short"));
        assertEquals(5, map.get("int"));
        assertEquals(6L, map.get("long"));
        assertEquals(7.5F, map.get("float"));
        assertEquals(8.5D, map.get("double"));
        assertEquals("hello", map.get("string"));
        assertArrayEquals(new byte[]{1, 2}, (byte[]) map.get("bytes"));
        assertArrayEquals(new int[]{3, 4}, (int[]) map.get("ints"));
        assertArrayEquals(new long[]{5L, 6L}, (long[]) map.get("longs"));
        assertArrayEquals(new Object[]{"first", "second"}, (Object[]) map.get("list"));
        Map<?, ?> nestedMap = assertInstanceOf(Map.class, map.get("nested"));
        assertEquals(9, nestedMap.get("value"));
    }

    @Test
    void duplicateItemDriversAndConvertersAreIgnoredLikeUpstream() {
        DriverRegistry registry = new DriverRegistry();
        TestDriverItem item = new TestDriverItem(true);
        Converter converter = (value, output) -> output.put("key", "value");

        registry.add(item);
        registry.add(item);
        registry.add(converter);
        registry.add(converter);

        assertEquals(1, registry.itemDrivers().size());
        assertEquals(1, registry.converterCount());
    }

    @Test
    void lockedRegistryRejectsLateRegistrationsLikeUpstream() {
        DriverRegistry registry = new DriverRegistry();
        registry.add(new TestDriverItem(true));
        Method lock = assertDoesNotThrow(() -> DriverRegistry.class.getDeclaredMethod("lockRegistrations"));
        assertDoesNotThrow(() -> lock.invoke(registry));

        assertThrows(IllegalStateException.class, () -> registry.add(new TestDriverItem(true)));
        assertThrows(IllegalStateException.class, () -> registry.add((Converter) (value, output) -> output.put("key", "value")));
        assertEquals(1, registry.itemDrivers().size());
        assertEquals(0, registry.converterCount());
    }

    @Test
    void apiLockHelperLocksDriverRegistryAfterSetup() {
        DriverRegistry registry = new DriverRegistry();
        API.driver = registry;
        Method lock = assertDoesNotThrow(() -> OpenComputersApi.class.getDeclaredMethod("lockDriverRegistry"));

        assertDoesNotThrow(() -> lock.invoke(null));

        assertThrows(IllegalStateException.class, () -> registry.add(new TestDriverItem(true)));
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

    private record TestConvertedValue(String name) {
        @Override
        public String toString() {
            return "converted-value:" + name;
        }
    }

    private record TestCallbackBlockDriver(String name, String method, int priority) implements DriverBlock {
        @Override
        public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
            return true;
        }

        @Override
        public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
            if ("first".equals(method)) {
                return new FirstCallbackEnvironment(name, priority);
            }
            return new SecondCallbackEnvironment(name, priority);
        }
    }

    private abstract static class TestCallbackEnvironment implements ManagedEnvironment, NamedBlock {
        private final String name;
        private final int priority;
        private final Node node;

        private TestCallbackEnvironment(final String name, final int priority) {
            this.name = name;
            this.priority = priority;
            node = li.cil.oc.api.Network.newNode(this, Visibility.Network)
                .withComponent(name, Visibility.Network)
                .create();
        }

        @Override
        public Node node() {
            return node;
        }

        @Override
        public boolean canUpdate() {
            return false;
        }

        @Override
        public void update() {
        }

        @Override
        public void onConnect(final Node node) {
        }

        @Override
        public void onDisconnect(final Node node) {
        }

        @Override
        public void onMessage(final Message message) {
        }

        @Override
        public void load(final CompoundTag nbt) {
        }

        @Override
        public void save(final CompoundTag nbt) {
        }

        @Override
        public String preferredName() {
            return name;
        }

        @Override
        public int priority() {
            return priority;
        }
    }

    private static final class FirstCallbackEnvironment extends TestCallbackEnvironment {
        private FirstCallbackEnvironment(final String name, final int priority) {
            super(name, priority);
        }

        @Callback
        public Object[] first(final Context context, final Arguments arguments) {
            return new Object[]{"first"};
        }
    }

    private static final class SecondCallbackEnvironment extends TestCallbackEnvironment {
        private SecondCallbackEnvironment(final String name, final int priority) {
            super(name, priority);
        }

        @Callback
        public Object[] second(final Context context, final Arguments arguments) {
            return new Object[]{"second"};
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
