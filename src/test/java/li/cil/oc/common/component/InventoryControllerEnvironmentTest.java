package li.cil.oc.common.component;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InventoryControllerEnvironmentTest {
    @Test
    void reportsUpstreamDeviceInfo() {
        OpenComputersApi.initialize();

        final InventoryControllerEnvironment controller = new InventoryControllerEnvironment(null);
        final Map<String, String> metadata = controller.getDeviceInfo();

        assertEquals(DeviceInfo.DeviceClass.Generic, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Inventory controller", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MightyPirates GmbH & Co. KG", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
        assertEquals("Item Cataloguer R1", metadata.get(DeviceInfo.DeviceAttribute.Product));
    }

    @Test
    void nonAdapterControllerIsNeighborVisibleLikeUpstreamRobotAndDroneHosts() {
        OpenComputersApi.initialize();

        final InventoryControllerEnvironment controller = new InventoryControllerEnvironment(new TestEnvironmentHost());
        final Component component = assertInstanceOf(Component.class, controller.node());

        assertEquals(Visibility.Neighbors, component.visibility());
    }

    @Test
    void equipCallbackIsRobotSpecificLikeUpstream() {
        assertFalse(hasDeclaredEquipCallback(InventoryControllerEnvironment.class),
            "Adapter/drone inventory controller must not expose robot-only equip callback");
        assertTrue(hasDeclaredEquipCallback(InventoryControllerEnvironment.RobotInventoryControllerEnvironment.class),
            "Robot inventory controller must expose upstream equip callback");
    }

    @Test
    void rawStackCallbacksHonorInspectionConfigLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        withCachedConfig(ModSettings.ALLOW_ITEM_STACK_INSPECTION, false, () -> {
            final InventoryControllerEnvironment controller = new InventoryControllerEnvironment(null);

            assertArrayEquals(new Object[]{null, "not enabled in config"}, controller.getInventoryName(null, new TestArguments(0)));
            assertArrayEquals(new Object[]{null, "not enabled in config"}, controller.getStackInSlot(null, new TestArguments(0, 1)));
            assertArrayEquals(new Object[]{null, "not enabled in config"}, controller.getAllStacks(null, new TestArguments(0)));
        });
    }

    private static boolean hasDeclaredEquipCallback(final Class<?> type) {
        try {
            return type.getDeclaredMethod("equip", Context.class, Arguments.class).isAnnotationPresent(Callback.class);
        } catch (final NoSuchMethodException ignored) {
            return false;
        }
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

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class TestEnvironmentHost implements EnvironmentHost {
        @Override
        public Level world() {
            return null;
        }

        @Override
        public double xPosition() {
            return 0;
        }

        @Override
        public double yPosition() {
            return 0;
        }

        @Override
        public double zPosition() {
            return 0;
        }

        @Override
        public void markChanged() {
        }
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
        @Override public boolean isBoolean(final int index) { return index < values.length && values[index] instanceof Boolean; }
        @Override public boolean isInteger(final int index) { return index < values.length && values[index] instanceof Integer; }
        @Override public boolean isLong(final int index) { return index < values.length && values[index] instanceof Long; }
        @Override public boolean isDouble(final int index) { return index < values.length && values[index] instanceof Double; }
        @Override public boolean isString(final int index) { return index < values.length && values[index] instanceof String; }
        @Override public boolean isByteArray(final int index) { return index < values.length && values[index] instanceof byte[]; }
        @Override public boolean isTable(final int index) { return index < values.length && values[index] instanceof Map; }
        @Override public boolean isItemStack(final int index) { return index < values.length && values[index] instanceof ItemStack; }
        @Override public Object[] toArray() { return Arrays.copyOf(values, values.length); }
        @Override public Iterator<Object> iterator() { return Arrays.asList(values).iterator(); }
    }
}
