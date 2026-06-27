package li.cil.oc.common.blockentity;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.util.StateAware;
import li.cil.oc.common.ModSettings;
import net.minecraft.world.Container;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChargerBlockEntityTest {
    @Test
    void exposesUpstreamDeviceInfoMetadata() {
        assertTrue(DeviceInfo.class.isAssignableFrom(ChargerBlockEntity.class));
        assertTrue(StateAware.class.isAssignableFrom(ChargerBlockEntity.class));

        final Map<String, String> metadata = ChargerBlockEntity.deviceInfo();

        assertEquals(DeviceInfo.DeviceClass.Generic, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Charger", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MightyPirates GmbH & Co. KG", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
        assertEquals("PowerUpper", metadata.get(DeviceInfo.DeviceAttribute.Product));
    }

    @Test
    void usesUpstreamChargerPowerSettings() throws Exception {
        withCachedConfig(ModSettings.CHARGER_RATE, 123D, () ->
            withCachedConfig(ModSettings.CONVERTER_BUFFER, 456D, () -> {
                assertEquals(123D, ChargerBlockEntity.energyThroughput(), 0.000_001D);
                assertEquals(456D, ChargerBlockEntity.connectorBufferSize(), 0.000_001D);
            }));
    }

    @Test
    void exposesOneChargeableInventorySlotLikeUpstream() {
        assertTrue(Container.class.isAssignableFrom(ChargerBlockEntity.class));
        assertEquals(1, ChargerBlockEntity.CONTAINER_SIZE);
    }

    @Test
    void redstoneSignalControlsChargeSpeedLikeUpstream() {
        assertEquals(0D, ChargerBlockEntity.chargeSpeedForSignal(0, false), 0.000_001D);
        assertEquals(7D / 15D, ChargerBlockEntity.chargeSpeedForSignal(7, false), 0.000_001D);
        assertEquals(1D, ChargerBlockEntity.chargeSpeedForSignal(15, false), 0.000_001D);
        assertEquals(1D, ChargerBlockEntity.chargeSpeedForSignal(0, true), 0.000_001D);
        assertEquals(8D / 15D, ChargerBlockEntity.chargeSpeedForSignal(7, true), 0.000_001D);
        assertEquals(0D, ChargerBlockEntity.chargeSpeedForSignal(15, true), 0.000_001D);
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
