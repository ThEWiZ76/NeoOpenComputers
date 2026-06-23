package li.cil.oc.common.blockentity;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.common.ModSettings;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PowerDistributorBlockEntityTest {
    @Test
    void exposesPowerDeviceInfoMetadata() throws Exception {
        assertTrue(DeviceInfo.class.isAssignableFrom(PowerDistributorBlockEntity.class));

        final PowerDistributorBlockEntity distributor = allocateDistributor();
        final Map<String, String> metadata = ((DeviceInfo) distributor).getDeviceInfo();

        assertEquals(DeviceInfo.DeviceClass.Power, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Power distributor", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MightyPirates", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
        assertEquals(Double.toString(PowerDistributorBlockEntity.CONNECTOR_BUFFER_SIZE), metadata.get(DeviceInfo.DeviceAttribute.Capacity));
    }

    @Test
    void usesConfiguredBufferForDeviceInfoLikeUpstream() throws Exception {
        withCachedConfig(ModSettings.POWER_DISTRIBUTOR_BUFFER, 42D, () -> {
            final PowerDistributorBlockEntity distributor = allocateDistributor();

            assertEquals(42D, PowerDistributorBlockEntity.connectorBufferSize(), 0.000_001D);
            assertEquals("42.0", distributor.getDeviceInfo().get(DeviceInfo.DeviceAttribute.Capacity));
        });
    }

    private static PowerDistributorBlockEntity allocateDistributor() throws Exception {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (PowerDistributorBlockEntity) ((Unsafe) unsafeField.get(null)).allocateInstance(PowerDistributorBlockEntity.class);
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
