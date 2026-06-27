package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Tablet;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

public final class TabletEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String COMPONENT_NAME = "tablet";
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.System,
        DeviceInfo.DeviceAttribute.Description, "Tablet",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
        DeviceInfo.DeviceAttribute.Product, "Jogger"
    );

    private final Tablet tablet;

    public TabletEnvironment(final Tablet tablet) {
        this.tablet = tablet;
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.withComponent(COMPONENT_NAME).withConnector().create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return DEVICE_INFO;
    }

    @Callback(doc = "function():number -- Gets the pitch of the player holding the tablet.")
    public Object[] getPitch(final Context context, final Arguments args) {
        final Player player = tablet == null ? null : tablet.player();
        return new Object[]{player == null ? 0F : player.getXRot()};
    }

    @Callback(doc = "function():number -- Gets the yaw of the player holding the tablet.")
    public Object[] getYaw(final Context context, final Arguments args) {
        final Player player = tablet == null ? null : tablet.player();
        return new Object[]{player == null ? 0F : player.getYRot()};
    }
}
