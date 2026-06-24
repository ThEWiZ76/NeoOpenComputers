package li.cil.oc.common.blockentity;

import li.cil.oc.api.driver.DeviceInfo;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class MotionSensorBlockEntityTest {
    @Test
    void exposesUpstreamDeviceInfoMetadata() throws Exception {
        MotionSensorBlockEntity sensor = allocateSensor();

        Map<String, String> metadata = sensor.getDeviceInfo();

        assertEquals(DeviceInfo.DeviceClass.Generic, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Motion sensor", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MightyPirates GmbH & Co. KG", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
        assertEquals("Blinker M1K0", metadata.get(DeviceInfo.DeviceAttribute.Product));
        assertEquals("8", metadata.get(DeviceInfo.DeviceAttribute.Capacity));
    }

    @Test
    void loadedSensitivityPreservesSavedValueLikeUpstream() throws Exception {
        MotionSensorBlockEntity sensor = allocateSensor();
        CompoundTag tag = new CompoundTag();
        tag.putDouble("oc:sensitivity", 0.1D);

        sensor.loadAdditional(tag, null);

        assertArrayEquals(new Object[]{0.1D}, sensor.getSensitivity(null, null));
    }

    @Test
    void missingSavedSensitivityLoadsZeroLikeUpstream() throws Exception {
        MotionSensorBlockEntity sensor = allocateSensor();
        setSensitivity(sensor, 0.4D);

        sensor.loadAdditional(new CompoundTag(), null);

        assertArrayEquals(new Object[]{0.0D}, sensor.getSensitivity(null, null));
    }

    private static MotionSensorBlockEntity allocateSensor() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (MotionSensorBlockEntity) ((Unsafe) unsafeField.get(null)).allocateInstance(MotionSensorBlockEntity.class);
    }

    private static void setSensitivity(final MotionSensorBlockEntity sensor, final double value) throws Exception {
        Field sensitivity = MotionSensorBlockEntity.class.getDeclaredField("sensitivity");
        sensitivity.setAccessible(true);
        sensitivity.setDouble(sensor, value);
    }
}
