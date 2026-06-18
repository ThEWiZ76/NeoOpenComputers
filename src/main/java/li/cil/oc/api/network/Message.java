package li.cil.oc.api.network;

/**
 * Message delivered through an OpenComputers network.
 */
public interface Message {
    Node source();

    String name();

    Object[] data();

    void cancel();
}
