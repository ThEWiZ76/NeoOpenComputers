package li.cil.oc.common.blockentity;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

final class MotionSensorBlockEntityTest {
    @Test
    void loadedSensitivityPreservesSavedValueLikeUpstream() throws Exception {
        MotionSensorBlockEntity sensor = allocateSensor();
        CompoundTag tag = new CompoundTag();
        tag.putDouble("oc:sensitivity", 0.1D);

        sensor.loadAdditional(tag, null);

        assertArrayEquals(new Object[]{0.1D}, sensor.getSensitivity(null, null));
    }

    private static MotionSensorBlockEntity allocateSensor() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (MotionSensorBlockEntity) ((Unsafe) unsafeField.get(null)).allocateInstance(MotionSensorBlockEntity.class);
    }
}
