package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;

import java.util.Map;

public final class AngelUpgradeEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
        DeviceInfo.DeviceAttribute.Description, "Angel upgrade",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
        DeviceInfo.DeviceAttribute.Product, "FreePlacer (TM)",
        DeviceInfo.DeviceAttribute.Capacity, "8192"
    );

    public AngelUpgradeEnvironment() {
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return DEVICE_INFO;
    }
}
