package li.cil.oc.common.component;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;

import java.util.Map;

public final class ScreenEnvironment {
    public static final String COMPONENT_NAME = "screen";

    public static Node createNode(final Environment host) {
        final var builder = Network.newNode(host, Visibility.Network);
        if (builder == null) {
            return null;
        }
        return builder.withComponent(COMPONENT_NAME, Visibility.Network).withConnector().create();
    }

    public static Map<String, String> deviceInfo(final int maxWidth, final int maxHeight, final TextBuffer.ColorDepth maxDepth) {
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Display,
            DeviceInfo.DeviceAttribute.Description, "Text buffer",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
            DeviceInfo.DeviceAttribute.Product, "Text Screen V0",
            DeviceInfo.DeviceAttribute.Capacity, Integer.toString(Math.max(0, maxWidth) * Math.max(0, maxHeight)),
            DeviceInfo.DeviceAttribute.Width, Integer.toString(bits(maxDepth))
        );
    }

    private static int bits(final TextBuffer.ColorDepth depth) {
        if (depth == null) {
            return 1;
        }
        return switch (depth) {
            case OneBit -> 1;
            case FourBit -> 4;
            case EightBit -> 8;
        };
    }

    private ScreenEnvironment() {
    }
}
