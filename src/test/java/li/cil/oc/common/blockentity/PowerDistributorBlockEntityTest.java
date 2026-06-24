package li.cil.oc.common.blockentity;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.common.ModSettings;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class PowerDistributorBlockEntityTest {
    @Test
    void doesNotExposeDeviceInfoMetadataLikeUpstream() {
        assertFalse(DeviceInfo.class.isAssignableFrom(PowerDistributorBlockEntity.class));
    }

    @Test
    void usesConfiguredConnectorBufferLikeUpstream() throws Exception {
        withCachedConfig(ModSettings.POWER_DISTRIBUTOR_BUFFER, 42D, () -> {
            assertEquals(42D, PowerDistributorBlockEntity.connectorBufferSize(), 0.000_001D);
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
