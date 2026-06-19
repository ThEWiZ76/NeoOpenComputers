package li.cil.oc.api.detail;

import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;

public interface Builder<T extends Node> {
    T create();

    interface NodeBuilder extends Builder<Node> {
        ComponentBuilder withComponent(String name, Visibility visibility);

        ComponentBuilder withComponent(String name);

        ConnectorBuilder withConnector(double bufferSize);

        ConnectorBuilder withConnector();
    }

    interface ComponentBuilder extends Builder<Component> {
        ComponentConnectorBuilder withConnector(double bufferSize);

        ComponentConnectorBuilder withConnector();
    }

    interface ConnectorBuilder extends Builder<Connector> {
        ComponentConnectorBuilder withComponent(String name, Visibility visibility);

        ComponentConnectorBuilder withComponent(String name);
    }

    interface ComponentConnectorBuilder extends Builder<ComponentConnector> {
    }
}
