package li.cil.oc.common.component;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.OpenComputersApi;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

final class BatteryUpgradeEnvironmentTest {
    @Test
    void createsPowerConnectorWithTierCapacity() {
        OpenComputersApi.initialize();
        BatteryUpgradeEnvironment environment = new BatteryUpgradeEnvironment(0);

        Connector connector = assertInstanceOf(Connector.class, environment.node());
        assertEquals(10000D, connector.localBufferSize(), 0.000_001D);
        Map<String, String> metadata = environment.getDeviceInfo();
        assertEquals(DeviceInfo.DeviceClass.Power, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Battery", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("Unlimited Power (Almost Ed.)", metadata.get(DeviceInfo.DeviceAttribute.Product));
        assertEquals("10000.0", metadata.get(DeviceInfo.DeviceAttribute.Capacity));
    }
}
