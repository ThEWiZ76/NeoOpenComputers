package li.cil.oc.common.component;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AngelUpgradeEnvironmentTest {
    @Test
    void reportsConfiguredNetworkPacketCapacity() throws Exception {
        OpenComputersApi.initialize();

        withCachedConfig(ModSettings.MAX_NETWORK_PACKET_SIZE, 32, () -> {
            AngelUpgradeEnvironment environment = new AngelUpgradeEnvironment();

            assertEquals("32", environment.getDeviceInfo().get(DeviceInfo.DeviceAttribute.Capacity));
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
