package li.cil.oc.api.network;

import li.cil.oc.api.Persistable;

/**
 * Single node in an OpenComputers network graph.
 */
public interface Node extends Persistable {
    Environment host();

    Visibility reachability();

    String address();

    Network network();

    boolean isNeighborOf(Node other);

    boolean canBeReachedFrom(Node other);

    Iterable<Node> neighbors();

    Iterable<Node> reachableNodes();

    void connect(Node node);

    void disconnect(Node node);

    void remove();

    void sendToAddress(String target, String name, Object... data);

    void sendToNeighbors(String name, Object... data);

    void sendToReachable(String name, Object... data);

    void sendToVisible(String name, Object... data);
}
