package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;

import java.util.Map;

public final class KeyboardEnvironment {
    public static final String COMPONENT_NAME = "keyboard";
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Input,
        DeviceInfo.DeviceAttribute.Description, "Keyboard",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
        DeviceInfo.DeviceAttribute.Product, "Fancytyper MX-Stone"
    );

    public static Node createNode(final Environment host) {
        final var builder = Network.newNode(host, Visibility.Network);
        if (builder == null) {
            return null;
        }
        return builder.withComponent(COMPONENT_NAME, Visibility.Network).create();
    }

    public static Map<String, String> deviceInfo() {
        return DEVICE_INFO;
    }

    private KeyboardEnvironment() {
    }
}
