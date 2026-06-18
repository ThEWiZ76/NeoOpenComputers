package li.cil.oc.api.driver;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DeviceInfoTest {
    @Test
    void deviceInfoReturnsStringMap() {
        DeviceInfo info = () -> Map.of(DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Memory);

        assertEquals("memory", info.getDeviceInfo().get("class"));
    }

    @Test
    void exposesUpstreamRecommendedAttributeKeys() {
        assertEquals("description", DeviceInfo.DeviceAttribute.Description);
        assertEquals("vendor", DeviceInfo.DeviceAttribute.Vendor);
        assertEquals("product", DeviceInfo.DeviceAttribute.Product);
        assertEquals("clock", DeviceInfo.DeviceAttribute.Clock);
    }

    @Test
    void exposesUpstreamRecommendedDeviceClasses() {
        assertEquals("processor", DeviceInfo.DeviceClass.Processor);
        assertEquals("volume", DeviceInfo.DeviceClass.Volume);
        assertEquals("generic", DeviceInfo.DeviceClass.Generic);
    }
}
