package li.cil.oc.common.component;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.internal.MultiTank;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExperienceUpgradeEnvironmentTest {
    @Test
    void exposesExperienceCallbacks() throws NoSuchMethodException {
        assertCallback("level");
        assertCallback("consume");
    }

    @Test
    void levelMathUpdatesConnectorAndPersistsExperience() {
        OpenComputersApi.initialize();
        TestAgent host = new TestAgent();
        ExperienceUpgradeEnvironment environment = new ExperienceUpgradeEnvironment(host);

        ComponentConnector connector = assertInstanceOf(ComponentConnector.class, environment.node());
        assertArrayEquals(new Object[]{0D}, environment.level(null, new TestArguments()));
        assertEquals(150_000D, connector.localBufferSize(), 0.000_001D);

        environment.addExperience(ExperienceUpgradeEnvironment.xpForLevel(2));

        assertArrayEquals(new Object[]{2D}, environment.level(null, new TestArguments()));
        assertEquals(10_000D, connector.localBufferSize(), 0.000_001D);
        assertTrue(host.changed);

        CompoundTag tag = new CompoundTag();
        environment.save(tag);

        ExperienceUpgradeEnvironment loaded = new ExperienceUpgradeEnvironment(new TestAgent());
        loaded.load(tag);

        assertArrayEquals(new Object[]{2D}, loaded.level(null, new TestArguments()));
        assertEquals(10_000D, assertInstanceOf(ComponentConnector.class, loaded.node()).localBufferSize(), 0.000_001D);
    }

    @Test
    void clampsLevelAtThirty() {
        OpenComputersApi.initialize();
        ExperienceUpgradeEnvironment environment = new ExperienceUpgradeEnvironment(new TestAgent());

        environment.addExperience(ExperienceUpgradeEnvironment.xpForLevel(60));

        assertArrayEquals(new Object[]{30D}, environment.level(null, new TestArguments()));
        assertEquals(150_000D, assertInstanceOf(ComponentConnector.class, environment.node()).localBufferSize(), 0.000_001D);
    }

    @Test
    void usesConfiguredBufferPerLevel() throws Exception {
        withCachedConfig(ModSettings.EXPERIENCE_BUFFER_PER_LEVEL, 123D, () -> {
            OpenComputersApi.initialize();
            ExperienceUpgradeEnvironment environment = new ExperienceUpgradeEnvironment(new TestAgent());

            environment.addExperience(ExperienceUpgradeEnvironment.xpForLevel(2));

            assertEquals(246D, assertInstanceOf(ComponentConnector.class, environment.node()).localBufferSize(), 0.000_001D);
        });
    }

    @Test
    void exposesDeviceInfoMetadata() {
        OpenComputersApi.initialize();
        ExperienceUpgradeEnvironment environment = new ExperienceUpgradeEnvironment(new TestAgent());

        Map<String, String> metadata = ((DeviceInfo) environment).getDeviceInfo();

        assertEquals(DeviceInfo.DeviceClass.Generic, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Knowledge database", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("ERSO (Event Recorder and Self-Optimizer)", metadata.get(DeviceInfo.DeviceAttribute.Product));
        assertEquals("30", metadata.get(DeviceInfo.DeviceAttribute.Capacity));
    }

    @Test
    void componentIsNetworkVisibleLikeUpstreamExperienceUpgrade() {
        OpenComputersApi.initialize();

        ExperienceUpgradeEnvironment environment = new ExperienceUpgradeEnvironment(new TestAgent());
        ComponentConnector connector = assertInstanceOf(ComponentConnector.class, environment.node());

        assertEquals(Visibility.Network, connector.visibility());
    }

    private static void assertCallback(final String methodName) throws NoSuchMethodException {
        Method method = ExperienceUpgradeEnvironment.class.getMethod(methodName, li.cil.oc.api.machine.Context.class, Arguments.class);
        assertTrue(method.isAnnotationPresent(Callback.class));
    }

    private static <T> void withCachedConfig(final ModConfigSpec.ConfigValue<T> value, final T override, final ThrowingRunnable action) throws Exception {
        final Field cachedValue = ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
        cachedValue.setAccessible(true);
        final Object previous = cachedValue.get(value);
        cachedValue.set(value, override);
        try {
            action.run();
        } finally {
            cachedValue.set(value, previous);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class TestAgent implements Agent {
        private boolean changed;
        private int selectedSlot;

        @Override public Container equipmentInventory() { return null; }
        @Override public Container mainInventory() { return null; }
        @Override public MultiTank tank() { return null; }
        @Override public int selectedSlot() { return selectedSlot; }
        @Override public void setSelectedSlot(final int index) { selectedSlot = index; }
        @Override public int selectedTank() { return 0; }
        @Override public void setSelectedTank(final int index) { }
        @Override public Player player() { return null; }
        @Override public String name() { return "test"; }
        @Override public void setName(final String name) { }
        @Override public String ownerName() { return "test"; }
        @Override public UUID ownerUUID() { return new UUID(0L, 0L); }
        @Override public Machine machine() { return null; }
        @Override public Iterable<ItemStack> internalComponents() { return List.of(); }
        @Override public int componentSlot(final String address) { return -1; }
        @Override public void onMachineConnect(final Node node) { }
        @Override public void onMachineDisconnect(final Node node) { }
        @Override public Level world() { return null; }
        @Override public double xPosition() { return 0; }
        @Override public double yPosition() { return 0; }
        @Override public double zPosition() { return 0; }
        @Override public void markChanged() { changed = true; }
        @Override public Direction facing() { return Direction.NORTH; }
        @Override public Direction toGlobal(final Direction value) { return value; }
        @Override public Direction toLocal(final Direction value) { return value; }
    }

    private record TestArguments(Object... values) implements Arguments {
        @Override public int count() { return values.length; }
        @Override public Object checkAny(final int index) { return values[index]; }
        @Override public boolean checkBoolean(final int index) { return (Boolean) values[index]; }
        @Override public int checkInteger(final int index) { return ((Number) values[index]).intValue(); }
        @Override public long checkLong(final int index) { return ((Number) values[index]).longValue(); }
        @Override public double checkDouble(final int index) { return ((Number) values[index]).doubleValue(); }
        @Override public String checkString(final int index) { return (String) values[index]; }
        @Override public byte[] checkByteArray(final int index) { return (byte[]) values[index]; }
        @Override public Map checkTable(final int index) { return (Map) values[index]; }
        @Override public ItemStack checkItemStack(final int index) { return (ItemStack) values[index]; }
        @Override public Object optAny(final int index, final Object def) { return index < values.length ? values[index] : def; }
        @Override public boolean optBoolean(final int index, final boolean def) { return index < values.length ? checkBoolean(index) : def; }
        @Override public int optInteger(final int index, final int def) { return index < values.length ? checkInteger(index) : def; }
        @Override public long optLong(final int index, final long def) { return index < values.length ? checkLong(index) : def; }
        @Override public double optDouble(final int index, final double def) { return index < values.length ? checkDouble(index) : def; }
        @Override public String optString(final int index, final String def) { return index < values.length ? checkString(index) : def; }
        @Override public byte[] optByteArray(final int index, final byte[] def) { return index < values.length ? checkByteArray(index) : def; }
        @Override public Map optTable(final int index, final Map def) { return index < values.length ? checkTable(index) : def; }
        @Override public ItemStack optItemStack(final int index, final ItemStack def) { return index < values.length ? checkItemStack(index) : def; }
        @Override public boolean isBoolean(final int index) { return values[index] instanceof Boolean; }
        @Override public boolean isInteger(final int index) { return values[index] instanceof Integer; }
        @Override public boolean isLong(final int index) { return values[index] instanceof Long; }
        @Override public boolean isDouble(final int index) { return values[index] instanceof Double; }
        @Override public boolean isString(final int index) { return values[index] instanceof String; }
        @Override public boolean isByteArray(final int index) { return values[index] instanceof byte[]; }
        @Override public boolean isTable(final int index) { return values[index] instanceof Map; }
        @Override public boolean isItemStack(final int index) { return values[index] instanceof ItemStack; }
        @Override public Object[] toArray() { return values; }
        @Override public Iterator<Object> iterator() { return java.util.Arrays.asList(values).iterator(); }
    }
}
