package li.cil.oc.common.component;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.common.OpenComputersApi;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class PowerUpgradeDeviceInfoTest {
    @Test
    void batteryUpgradeExposesUpstreamVendorMetadata() {
        OpenComputersApi.initialize();

        assertUpstreamDefaultVendor(new BatteryUpgradeEnvironment(0).getDeviceInfo());
    }

    @Test
    void generatorUpgradeExposesUpstreamVendorMetadata() throws Exception {
        OpenComputersApi.initialize();

        assertUpstreamDefaultVendor(allocateGenerator().getDeviceInfo());
    }

    @Test
    void solarGeneratorUpgradeExposesUpstreamVendorMetadata() {
        OpenComputersApi.initialize();

        assertUpstreamDefaultVendor(new SolarGeneratorUpgradeEnvironment(null).getDeviceInfo());
    }

    private static void assertUpstreamDefaultVendor(final Map<String, String> metadata) {
        assertEquals("MightyPirates GmbH & Co. KG", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
    }

    private static GeneratorUpgradeEnvironment allocateGenerator() throws Exception {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (GeneratorUpgradeEnvironment) ((Unsafe) unsafeField.get(null)).allocateInstance(GeneratorUpgradeEnvironment.class);
    }
}
