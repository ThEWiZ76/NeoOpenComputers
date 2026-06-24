package li.cil.oc.common.component;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.common.OpenComputersApi;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class StandardUpgradeDeviceInfoTest {
    @Test
    void standardUpgradesExposeUpstreamVendorMetadata() throws Exception {
        OpenComputersApi.initialize();

        assertUpstreamDefaultVendor(new AngelUpgradeEnvironment().getDeviceInfo());
        assertUpstreamDefaultVendor(new BarcodeReaderUpgradeEnvironment(null).getDeviceInfo());
        assertUpstreamDefaultVendor(new ChunkloaderUpgradeEnvironment(null).getDeviceInfo());
        assertUpstreamDefaultVendor(new CraftingUpgradeEnvironment(null).getDeviceInfo());
        assertUpstreamDefaultVendor(new ExperienceUpgradeEnvironment(null).getDeviceInfo());
        assertUpstreamDefaultVendor(new LeashUpgradeEnvironment(null).getDeviceInfo());
        assertUpstreamDefaultVendor(new NavigationUpgradeEnvironment(null).getDeviceInfo());
        assertUpstreamDefaultVendor(new PistonUpgradeEnvironment(null, null).getDeviceInfo());
        assertUpstreamDefaultVendor(new SignUpgradeEnvironment(null).getDeviceInfo());
        assertUpstreamDefaultVendor(allocateTank().getDeviceInfo());
        assertUpstreamDefaultVendor(new TractorBeamUpgradeEnvironment((Agent) null).getDeviceInfo());
        assertUpstreamDefaultVendor(new TradingUpgradeEnvironment(null).getDeviceInfo());
    }

    private static void assertUpstreamDefaultVendor(final Map<String, String> metadata) {
        assertEquals("MightyPirates GmbH & Co. KG", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
    }

    private static TankUpgradeEnvironment allocateTank() throws Exception {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (TankUpgradeEnvironment) ((Unsafe) unsafeField.get(null)).allocateInstance(TankUpgradeEnvironment.class);
    }
}
