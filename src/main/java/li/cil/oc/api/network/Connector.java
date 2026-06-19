package li.cil.oc.api.network;

/**
 * Node with local and network-wide energy buffers.
 */
public interface Connector extends Node {
    double localBuffer();

    double localBufferSize();

    double globalBuffer();

    double globalBufferSize();

    double changeBuffer(double delta);

    boolean tryChangeBuffer(double delta);

    void setLocalBufferSize(double size);
}
