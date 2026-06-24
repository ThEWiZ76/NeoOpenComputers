package li.cil.oc.common.component;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class MotionSensorEnvironmentTest {
    @Test
    void exposesUpstreamDeviceInfoMetadata() {
        OpenComputersApi.initialize();
        MotionSensorEnvironment sensor = new MotionSensorEnvironment(null);

        Map<String, String> metadata = sensor.getDeviceInfo();

        assertEquals(DeviceInfo.DeviceClass.Generic, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Motion sensor", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MightyPirates GmbH & Co. KG", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
        assertEquals("Blinker M1K0", metadata.get(DeviceInfo.DeviceAttribute.Product));
        assertEquals("8", metadata.get(DeviceInfo.DeviceAttribute.Capacity));
    }

    @Test
    void loadedSensitivityPreservesSavedValueLikeUpstream() {
        OpenComputersApi.initialize();
        MotionSensorEnvironment sensor = new MotionSensorEnvironment(null);
        CompoundTag tag = new CompoundTag();
        tag.putDouble("oc:sensitivity", 0.1D);

        sensor.load(tag);

        assertArrayEquals(new Object[]{0.1D}, sensor.getSensitivity(null, null));
    }

    @Test
    void missingSavedSensitivityLoadsZeroLikeUpstream() {
        OpenComputersApi.initialize();
        MotionSensorEnvironment sensor = new MotionSensorEnvironment(null);

        sensor.load(new CompoundTag());

        assertArrayEquals(new Object[]{0.0D}, sensor.getSensitivity(null, null));
    }
}
