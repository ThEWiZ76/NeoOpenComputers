package li.cil.oc.common.component;

import li.cil.oc.api.API;
import li.cil.oc.api.detail.Builder;
import li.cil.oc.api.detail.NetworkAPI;
import li.cil.oc.api.event.RobotMoveEvent;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Network;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.network.WirelessEndpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

final class WirelessNetworkCardHandlerTest {
    @AfterEach
    void resetApiReference() {
        API.network = null;
    }

    @Test
    void updatesReachableWirelessNetworkCardAfterRobotMoveLikeUpstream() {
        final RecordingNetworkApi api = new RecordingNetworkApi();
        API.network = api;
        final WirelessNetworkCardEnvironment card = new WirelessNetworkCardEnvironment(null, 0);
        final TestNode machineNode = new TestNode(null, List.of(new TestNode(card, List.of())));

        WirelessNetworkCardHandler.onRobotMove(new RobotMoveEvent.Post(agent(machineNode), Direction.NORTH));

        assertSame(card, api.updatedEndpoint);
    }

    @Test
    void ignoresReachableNonWirelessCardNodes() {
        final RecordingNetworkApi api = new RecordingNetworkApi();
        API.network = api;
        final TestNode machineNode = new TestNode(null, List.of(new TestNode(new TestEnvironment(), List.of())));

        WirelessNetworkCardHandler.onRobotMove(new RobotMoveEvent.Post(agent(machineNode), Direction.NORTH));

        assertNull(api.updatedEndpoint);
    }

    private static Agent agent(final Node node) {
        final Machine machine = (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "node" -> node;
                case "toString" -> "TestMachine";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> defaultValue(method.getReturnType());
            });
        return (Agent) Proxy.newProxyInstance(
            Agent.class.getClassLoader(),
            new Class<?>[]{Agent.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "machine" -> machine;
                case "toString" -> "TestAgent";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> defaultValue(method.getReturnType());
            });
    }

    private static Object defaultValue(final Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == void.class) {
            return null;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }

    private record TestNode(Environment host, Iterable<Node> reachableNodes) implements Node {
        @Override public Visibility reachability() { return Visibility.Network; }
        @Override public String address() { return "test"; }
        @Override public Network network() { return null; }
        @Override public boolean isNeighborOf(final Node other) { return false; }
        @Override public boolean canBeReachedFrom(final Node other) { return false; }
        @Override public Iterable<Node> neighbors() { return List.of(); }
        @Override public void connect(final Node node) {}
        @Override public void disconnect(final Node node) {}
        @Override public void remove() {}
        @Override public void sendToAddress(final String target, final String name, final Object... data) {}
        @Override public void sendToNeighbors(final String name, final Object... data) {}
        @Override public void sendToReachable(final String name, final Object... data) {}
        @Override public void sendToVisible(final String name, final Object... data) {}
        @Override public void load(final CompoundTag nbt) {}
        @Override public void save(final CompoundTag nbt) {}
    }

    private static final class TestEnvironment implements Environment {
        @Override public Node node() { return null; }
        @Override public void onConnect(final Node node) {}
        @Override public void onDisconnect(final Node node) {}
        @Override public void onMessage(final li.cil.oc.api.network.Message message) {}
    }

    private static final class RecordingNetworkApi implements NetworkAPI {
        private WirelessEndpoint updatedEndpoint;

        @Override public void joinOrCreateNetwork(final BlockEntity blockEntity) {}
        @Override public void joinOrCreateNetwork(final BlockGetter world, final BlockPos pos) {}
        @Override public void joinNewNetwork(final Node node) {}
        @Override public void joinWirelessNetwork(final WirelessEndpoint endpoint) {}
        @Override public void updateWirelessNetwork(final WirelessEndpoint endpoint) { updatedEndpoint = endpoint; }
        @Override public void leaveWirelessNetwork(final WirelessEndpoint endpoint) {}
        @Override public void leaveWirelessNetwork(final WirelessEndpoint endpoint, final ResourceKey<Level> dimension) {}
        @Override public void sendWirelessPacket(final WirelessEndpoint source, final double strength, final Packet packet) {}
        @Override public Builder.NodeBuilder newNode(final Environment host, final Visibility reachability) { return null; }
        @Override public Packet newPacket(final String source, final String destination, final int port, final Object[] data) { return null; }
        @Override public Packet newPacket(final CompoundTag nbt) { return null; }
    }
}
