package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;

import java.util.Map;

public final class PassiveDeviceInfoEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private final Map<String, String> deviceInfo;

    public PassiveDeviceInfoEnvironment(final Map<String, String> deviceInfo) {
        this.deviceInfo = Map.copyOf(deviceInfo);
        final var builder = Network.newNode(this, Visibility.Neighbors);
        if (builder != null) {
            setNode(builder.create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }
}
