package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;

public final class ScreenEnvironment {
    public static final String COMPONENT_NAME = "screen";

    public static Node createNode(final Environment host) {
        final var builder = Network.newNode(host, Visibility.Neighbors);
        if (builder == null) {
            return null;
        }
        return builder.withComponent(COMPONENT_NAME, Visibility.Neighbors).create();
    }

    private ScreenEnvironment() {
    }
}
