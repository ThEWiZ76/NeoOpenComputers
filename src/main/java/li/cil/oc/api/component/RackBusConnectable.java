package li.cil.oc.api.component;

import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Packet;

public interface RackBusConnectable extends Environment {
    void receivePacket(Packet packet);
}
