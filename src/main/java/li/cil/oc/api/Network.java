package li.cil.oc.api;

import li.cil.oc.api.detail.Builder;
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

public final class Network {
    public static void joinOrCreateNetwork(final BlockEntity blockEntity) {
        if (API.network != null) {
            API.network.joinOrCreateNetwork(blockEntity);
        }
    }

    public static void joinOrCreateNetwork(final BlockGetter world, final BlockPos pos) {
        if (API.network != null) {
            API.network.joinOrCreateNetwork(world, pos);
        }
    }

    public static void joinNewNetwork(final Node node) {
        if (API.network != null) {
            API.network.joinNewNetwork(node);
        }
    }

    public static void joinWirelessNetwork(final WirelessEndpoint endpoint) {
        if (API.network != null) {
            API.network.joinWirelessNetwork(endpoint);
        }
    }

    public static void updateWirelessNetwork(final WirelessEndpoint endpoint) {
        if (API.network != null) {
            API.network.updateWirelessNetwork(endpoint);
        }
    }

    public static void leaveWirelessNetwork(final WirelessEndpoint endpoint) {
        if (API.network != null) {
            API.network.leaveWirelessNetwork(endpoint);
        }
    }

    public static void leaveWirelessNetwork(final WirelessEndpoint endpoint, final ResourceKey<Level> dimension) {
        if (API.network != null) {
            API.network.leaveWirelessNetwork(endpoint, dimension);
        }
    }

    public static void sendWirelessPacket(final WirelessEndpoint source, final double strength, final Packet packet) {
        if (API.network != null) {
            API.network.sendWirelessPacket(source, strength, packet);
        }
    }

    public static Builder.NodeBuilder newNode(final Environment host, final Visibility reachability) {
        if (API.network != null) {
            return API.network.newNode(host, reachability);
        }
        return null;
    }

    public static Packet newPacket(final String source, final String destination, final int port, final Object... data) {
        if (API.network != null) {
            return API.network.newPacket(source, destination, port, data);
        }
        return null;
    }

    public static Packet newPacket(final CompoundTag nbt) {
        if (API.network != null) {
            return API.network.newPacket(nbt);
        }
        return null;
    }

    private Network() {
    }
}
