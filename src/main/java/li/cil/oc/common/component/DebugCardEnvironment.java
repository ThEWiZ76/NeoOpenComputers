package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;

public final class DebugCardEnvironment extends AbstractManagedEnvironment {
    private static final String COMPONENT_NAME = "debug";

    private final EnvironmentHost host;

    public DebugCardEnvironment(final EnvironmentHost host) {
        this.host = host;
        final var builder = Network.newNode(this, Visibility.Neighbors);
        if (builder != null) {
            setNode(builder.withComponent(COMPONENT_NAME).withConnector().create());
        }
    }

    @Callback(doc = "function(value:number):number -- Changes the component network's energy buffer by the specified delta.")
    public Object[] changeBuffer(final Context context, final Arguments args) {
        if (node() instanceof Connector connector) {
            return new Object[]{connector.changeBuffer(args.checkDouble(0))};
        }
        return new Object[]{0D};
    }

    @Callback(doc = "function():number -- Get the container's X position in the world.")
    public Object[] getX(final Context context, final Arguments args) {
        return new Object[]{host == null ? 0D : host.xPosition()};
    }

    @Callback(doc = "function():number -- Get the container's Y position in the world.")
    public Object[] getY(final Context context, final Arguments args) {
        return new Object[]{host == null ? 0D : host.yPosition()};
    }

    @Callback(doc = "function():number -- Get the container's Z position in the world.")
    public Object[] getZ(final Context context, final Arguments args) {
        return new Object[]{host == null ? 0D : host.zPosition()};
    }
}
