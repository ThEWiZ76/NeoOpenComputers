package li.cil.oc.api.detail;

import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.network.WirelessEndpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface NetworkAPI {
    void joinOrCreateNetwork(BlockEntity blockEntity);

    void joinOrCreateNetwork(BlockGetter world, BlockPos pos);

    void joinNewNetwork(Node node);

    void joinWirelessNetwork(WirelessEndpoint endpoint);

    void updateWirelessNetwork(WirelessEndpoint endpoint);

    void leaveWirelessNetwork(WirelessEndpoint endpoint);

    void leaveWirelessNetwork(WirelessEndpoint endpoint, ResourceKey<Level> dimension);

    void sendWirelessPacket(WirelessEndpoint source, double strength, Packet packet);

    Builder.NodeBuilder newNode(Environment host, Visibility reachability);

    Packet newPacket(String source, String destination, int port, Object[] data);

    Packet newPacket(CompoundTag nbt);
}
