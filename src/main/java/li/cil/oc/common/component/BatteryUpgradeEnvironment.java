package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.ModSettings;

import java.util.Map;

public class BatteryUpgradeEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    public static final double[] CAPACITIES = {10000D, 15000D, 20000D};

    private final int tier;

    public BatteryUpgradeEnvironment(final int tier) {
        this.tier = clampTier(tier);
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.withConnector(capacity(this.tier)).create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        final double capacity = capacity(tier);
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Power,
            DeviceInfo.DeviceAttribute.Description, "Battery",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
            DeviceInfo.DeviceAttribute.Product, "Unlimited Power (Almost Ed.)",
            DeviceInfo.DeviceAttribute.Capacity, Double.toString(capacity)
        );
    }

    public static double capacity(final int tier) {
        return ModSettings.batteryUpgradeBuffer(tier);
    }

    private static int clampTier(final int tier) {
        return Math.max(0, Math.min(CAPACITIES.length - 1, tier));
    }
}
