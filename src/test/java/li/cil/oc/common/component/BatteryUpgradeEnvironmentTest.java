package li.cil.oc.common.component;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import org.junit.jupiter.api.Test;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

final class BatteryUpgradeEnvironmentTest {
    @Test
    void createsPowerConnectorWithTierCapacity() {
        OpenComputersApi.initialize();
        BatteryUpgradeEnvironment environment = new BatteryUpgradeEnvironment(0);

        Connector connector = assertInstanceOf(Connector.class, environment.node());
        assertEquals(10000D, connector.localBufferSize(), 0.000_001D);
        Map<String, String> metadata = environment.getDeviceInfo();
        assertEquals(DeviceInfo.DeviceClass.Power, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Battery", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("Unlimited Power (Almost Ed.)", metadata.get(DeviceInfo.DeviceAttribute.Product));
        assertEquals("10000.0", metadata.get(DeviceInfo.DeviceAttribute.Capacity));
    }

    @Test
    void usesConfiguredBatteryUpgradeBuffersLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        withCachedConfig(ModSettings.BATTERY_UPGRADE_BUFFERS, List.of(11D, 22D, 33D), () -> {
            final BatteryUpgradeEnvironment environment = new BatteryUpgradeEnvironment(1);
            final Connector connector = assertInstanceOf(Connector.class, environment.node());

            assertEquals(22D, BatteryUpgradeEnvironment.capacity(1), 0.000_001D);
            assertEquals(22D, connector.localBufferSize(), 0.000_001D);
            assertEquals("22.0", environment.getDeviceInfo().get(DeviceInfo.DeviceAttribute.Capacity));
        });
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
