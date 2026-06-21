package li.cil.oc.common.blockentity;

import li.cil.oc.api.driver.DeviceInfo;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AdapterBlockEntityTest {
    @Test
    void exposesUpstreamDeviceInfoMetadata() throws Exception {
        assertTrue(DeviceInfo.class.isAssignableFrom(AdapterBlockEntity.class));

        final AdapterBlockEntity adapter = allocateAdapter();
        final Map<String, String> metadata = ((DeviceInfo) adapter).getDeviceInfo();

        assertEquals(DeviceInfo.DeviceClass.Bus, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Adapter", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MightyPirates", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
        assertEquals("Multiplug Ext.1", metadata.get(DeviceInfo.DeviceAttribute.Product));
    }

    private static AdapterBlockEntity allocateAdapter() throws Exception {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (AdapterBlockEntity) ((Unsafe) unsafeField.get(null)).allocateInstance(AdapterBlockEntity.class);
    }
}
