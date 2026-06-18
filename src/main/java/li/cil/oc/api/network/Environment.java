package li.cil.oc.api.network;

/**
 * Host object attached to a network node.
 */
public interface Environment {
    Node node();

    void onConnect(Node node);

    void onDisconnect(Node node);

    void onMessage(Message message);
}
