package li.cil.oc.common.blockentity;

import li.cil.oc.api.driver.DeviceInfo;
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

    private static PowerDistributorBlockEntity allocateDistributor() throws Exception {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (PowerDistributorBlockEntity) ((Unsafe) unsafeField.get(null)).allocateInstance(PowerDistributorBlockEntity.class);
    }
}
