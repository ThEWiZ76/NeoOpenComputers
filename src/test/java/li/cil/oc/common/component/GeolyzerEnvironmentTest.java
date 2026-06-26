package li.cil.oc.common.component;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GeolyzerEnvironmentTest {
    @Test
    void usesConfiguredRangeAndScanCostLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        withCachedConfig(ModSettings.GEOLYZER_RANGE, 2, () ->
            withCachedConfig(ModSettings.GEOLYZER_SCAN_COST, 3.5D, () -> {
                GeolyzerEnvironment environment = new GeolyzerEnvironment(null);
                ComponentConnector connector = assertInstanceOf(ComponentConnector.class, environment.node());
                connector.setLocalBufferSize(10D);
                connector.changeBuffer(10D);

                DeviceInfo info = assertInstanceOf(DeviceInfo.class, environment);
                assertEquals("2", info.getDeviceInfo().get(DeviceInfo.DeviceAttribute.Capacity));
                assertInstanceOf(float[].class, environment.scan(null, new TestArguments(0, 0, 0, 1, 1, 1))[0]);
                assertEquals(6.5D, connector.localBuffer(), 0.000_001D);

                IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> environment.scan(null, new TestArguments(3, 0, 0, 1, 1, 1)));
                assertEquals("location out of bounds", error.getMessage());
            }));
    }

    @Test
    void analyzeHonorsItemStackInspectionConfigLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        withCachedConfig(ModSettings.ALLOW_ITEM_STACK_INSPECTION, false, () -> {
            GeolyzerEnvironment environment = new GeolyzerEnvironment(null);

            assertArrayEquals(new Object[]{null, "not enabled in config"}, environment.analyze(null, new TestArguments(0)));
        });
    }

    @Test
    void weatherBlocksSunOnlyForBiomesWithPrecipitationLikeUpstream() {
        assertTrue(GeolyzerEnvironment.weatherAllowsSun(false, true, false));
        assertTrue(GeolyzerEnvironment.weatherAllowsSun(false, false, true));
        assertTrue(GeolyzerEnvironment.weatherAllowsSun(false, true, true));
        assertTrue(GeolyzerEnvironment.weatherAllowsSun(true, false, false));

        assertFalse(GeolyzerEnvironment.weatherAllowsSun(true, true, false));
        assertFalse(GeolyzerEnvironment.weatherAllowsSun(true, false, true));
        assertFalse(GeolyzerEnvironment.weatherAllowsSun(true, true, true));
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
