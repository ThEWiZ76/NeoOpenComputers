package li.cil.oc.common.component;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.common.OpenComputersApi;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DatabaseEnvironmentTest {
    @Test
    void reportsUpstreamDeviceInfo() {
        OpenComputersApi.initialize();

        final DatabaseEnvironment database = new DatabaseEnvironment(3);
        final Map<String, String> metadata = database.getDeviceInfo();

        assertEquals(DeviceInfo.DeviceClass.Generic, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Object catalogue", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MightyPirates GmbH & Co. KG", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
        assertEquals("iCatalogue (patent pending)", metadata.get(DeviceInfo.DeviceAttribute.Product));
        assertEquals("3", metadata.get(DeviceInfo.DeviceAttribute.Capacity));
    }
}
