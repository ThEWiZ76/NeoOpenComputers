package li.cil.oc.common.component;

import li.cil.oc.common.OpenComputersApi;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

final class MotionSensorEnvironmentTest {
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
