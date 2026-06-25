package li.cil.oc.common.blockentity;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.common.ModSettings;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PowerConverterBlockEntityTest {
    @Test
    void exposesUpstreamPowerDeviceInfo() throws Exception {
        withCachedConfig(ModSettings.POWER_CONVERTER_RATE, 321D, () -> {
            Map<String, String> metadata = PowerConverterBlockEntity.deviceInfo();

            assertEquals(DeviceInfo.DeviceClass.Power, metadata.get(DeviceInfo.DeviceAttribute.Class));
            assertEquals("Power converter", metadata.get(DeviceInfo.DeviceAttribute.Description));
            assertEquals("MightyPirates GmbH & Co. KG", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
            assertEquals("Transgizer-PX5", metadata.get(DeviceInfo.DeviceAttribute.Product));
            assertEquals("321.0", metadata.get(DeviceInfo.DeviceAttribute.Capacity));
        });
    }

    @Test
    void usesConfiguredConverterBufferLikeUpstream() throws Exception {
        withCachedConfig(ModSettings.CONVERTER_BUFFER, 42D, () -> {
            assertEquals(42D, PowerConverterBlockEntity.connectorBufferSize(), 0.000_001D);
        });
    }

    @Test
    void implementsDeviceInfoUnlikePowerDistributor() {
        assertTrue(DeviceInfo.class.isAssignableFrom(PowerConverterBlockEntity.class));
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
}
