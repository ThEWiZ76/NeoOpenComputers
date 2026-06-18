package li.cil.oc.api.network;

/**
 * Graph of connected OpenComputers nodes.
 */
public interface Network {
    boolean connect(Node nodeA, Node nodeB);

    boolean disconnect(Node nodeA, Node nodeB);

    boolean remove(Node node);

    Node node(String address);

    Iterable<Node> nodes();

    Iterable<Node> nodes(Node reference);

    Iterable<Node> neighbors(Node node);

    void sendToAddress(Node source, String target, String name, Object... data);

    void sendToNeighbors(Node source, String name, Object... data);

    void sendToReachable(Node source, String name, Object... data);

    void sendToVisible(Node source, String name, Object... data);
}
