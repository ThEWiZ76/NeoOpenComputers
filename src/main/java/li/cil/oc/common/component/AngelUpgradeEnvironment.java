package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.ModSettings;

import java.util.Map;

public final class AngelUpgradeEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    public AngelUpgradeEnvironment() {
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
            DeviceInfo.DeviceAttribute.Description, "Angel upgrade",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
            DeviceInfo.DeviceAttribute.Product, "FreePlacer (TM)",
            DeviceInfo.DeviceAttribute.Capacity, Integer.toString(ModSettings.maxNetworkPacketSize())
        );
    }
}
