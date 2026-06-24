package li.cil.oc.common.component;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.common.OpenComputersApi;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CardDeviceInfoVendorTest {
    @Test
    void cardsExposeUpstreamDefaultVendorMetadata() {
        OpenComputersApi.initialize();

        assertUpstreamDefaultVendor(new GraphicsCardEnvironment(0).getDeviceInfo());
        assertUpstreamDefaultVendor(new InternetCardEnvironment().getDeviceInfo());
        assertUpstreamDefaultVendor(new LinkedCardEnvironment(null, "test").getDeviceInfo());
        assertUpstreamDefaultVendor(new RedstoneCardEnvironment(null).getDeviceInfo());
    }

    private static void assertUpstreamDefaultVendor(final Map<String, String> metadata) {
        assertEquals("MightyPirates GmbH & Co. KG", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
    }
}
