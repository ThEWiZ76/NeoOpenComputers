package li.cil.oc.api.network;

import net.minecraft.world.level.Level;

public interface WirelessEndpoint {
    int x();

    int y();

    int z();

    Level world();

    void receivePacket(Packet packet, WirelessEndpoint sender);
}
