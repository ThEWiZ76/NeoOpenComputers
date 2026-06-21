package li.cil.oc.common.component;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.common.OpenComputersApi;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class InventoryControllerEnvironmentTest {
    @Test
    void reportsUpstreamDeviceInfo() {
        OpenComputersApi.initialize();

        final InventoryControllerEnvironment controller = new InventoryControllerEnvironment(null);
        final Map<String, String> metadata = controller.getDeviceInfo();

        assertEquals(DeviceInfo.DeviceClass.Generic, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Inventory controller", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MightyPirates", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
        assertEquals("Item Cataloguer R1", metadata.get(DeviceInfo.DeviceAttribute.Product));
    }
}
