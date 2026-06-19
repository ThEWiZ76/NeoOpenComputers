package li.cil.oc.api;

import li.cil.oc.api.detail.Builder;
import li.cil.oc.api.detail.NetworkAPI;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NetworkFacadeApiTest {
    @AfterEach
    void resetApiReference() {
        API.network = null;
    }

    @Test
    void networkApiUsesModernMinecraftTypes() throws NoSuchMethodException {
        Method blockEntityJoin = NetworkAPI.class.getMethod("joinOrCreateNetwork", BlockEntity.class);
        Method blockGetterJoin = NetworkAPI.class.getMethod("joinOrCreateNetwork", BlockGetter.class, BlockPos.class);
        Method leaveDimension = NetworkAPI.class.getMethod("leaveWirelessNetwork", WirelessEndpoint.class, ResourceKey.class);
        Method loadPacket = NetworkAPI.class.getMethod("newPacket", CompoundTag.class);

        assertArrayEquals(new Class<?>[]{BlockEntity.class}, blockEntityJoin.getParameterTypes());
        assertArrayEquals(new Class<?>[]{BlockGetter.class, BlockPos.class}, blockGetterJoin.getParameterTypes());
        assertArrayEquals(new Class<?>[]{WirelessEndpoint.class, ResourceKey.class}, leaveDimension.getParameterTypes());
        assertEquals(Packet.class, loadPacket.getReturnType());
    }

    @Test
    void networkFacadeDelegatesToNetworkApi() {
        TestNetworkAPI api = new TestNetworkAPI();
        API.network = api;
        Node node = new TestNode();
        WirelessEndpoint endpoint = new TestWirelessEndpoint();
        Packet packet = new TestPacket();

        Network.joinOrCreateNetwork((BlockEntity) null);
        Network.joinOrCreateNetwork(null, BlockPos.ZERO);
        Network.joinNewNetwork(node);
        Network.joinWirelessNetwork(endpoint);
        Network.updateWirelessNetwork(endpoint);
        Network.leaveWirelessNetwork(endpoint);
        Network.leaveWirelessNetwork(endpoint, Level.OVERWORLD);
        Network.sendWirelessPacket(endpoint, 2.5, packet);

        assertTrue(api.blockEntityJoined);
        assertEquals(BlockPos.ZERO, api.position);
        assertSame(node, api.node);
        assertSame(endpoint, api.endpoint);
        assertEquals(Level.OVERWORLD, api.dimension);
        assertEquals(2.5, api.strength);
        assertSame(packet, api.packet);
        assertSame(api.builder, Network.newNode(new TestEnvironment(), Visibility.Network));
        Packet createdPacket = Network.newPacket("source", "destination", 10, new Object[]{"data"});
        assertSame(api.packet, createdPacket);
        Packet loadedPacket = Network.newPacket(new CompoundTag());
        assertSame(api.packet, loadedPacket);
        assertEquals("source", api.source);
        assertEquals("destination", api.destination);
        assertEquals(10, api.port);
    }

    @Test
    void networkFacadeReturnsNullWhenBackingApiIsMissing() {
        assertNull(Network.newNode(null, Visibility.Network));
        assertNull(Network.newPacket("source", "destination", 1, new Object[0]));
        assertNull(Network.newPacket(new CompoundTag()));
    }

    @Test
    void buildersCanDescribeNodeComponentConnectorCombinations() {
        TestNodeBuilder builder = new TestNodeBuilder();

        assertSame(builder.componentBuilder, builder.withComponent("gpu"));
        assertEquals("gpu", builder.componentName);
        assertEquals(Visibility.Network, builder.componentVisibility);
        assertSame(builder.componentBuilder, builder.withComponent("screen", Visibility.Neighbors));
        assertEquals("screen", builder.componentName);
        assertEquals(Visibility.Neighbors, builder.componentVisibility);
        assertSame(builder.connectorBuilder, builder.withConnector());
        assertEquals(0, builder.bufferSize);
        assertSame(builder.connectorBuilder, builder.withConnector(4.5));
        assertEquals(4.5, builder.bufferSize);
        assertSame(builder.componentConnectorBuilder, builder.componentBuilder.withConnector());
        assertSame(builder.componentConnectorBuilder, builder.connectorBuilder.withComponent("modem"));
        assertNull(builder.create());
    }

    private static final class TestNetworkAPI implements NetworkAPI {
        private final TestNodeBuilder builder = new TestNodeBuilder();
        private boolean blockEntityJoined;
        private BlockPos position;
        private Node node;
        private WirelessEndpoint endpoint;
        private ResourceKey<Level> dimension;
        private double strength;
        private Packet packet;
        private String source;
        private String destination;
        private int port;

        @Override
        public void joinOrCreateNetwork(final BlockEntity blockEntity) {
            blockEntityJoined = true;
        }

        @Override
        public void joinOrCreateNetwork(final BlockGetter world, final BlockPos pos) {
            position = pos;
        }

        @Override
        public void joinNewNetwork(final Node node) {
            this.node = node;
        }

        @Override
        public void joinWirelessNetwork(final WirelessEndpoint endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public void updateWirelessNetwork(final WirelessEndpoint endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public void leaveWirelessNetwork(final WirelessEndpoint endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public void leaveWirelessNetwork(final WirelessEndpoint endpoint, final ResourceKey<Level> dimension) {
            this.endpoint = endpoint;
            this.dimension = dimension;
        }

        @Override
        public void sendWirelessPacket(final WirelessEndpoint source, final double strength, final Packet packet) {
            endpoint = source;
            this.strength = strength;
            this.packet = packet;
        }

        @Override
        public Builder.NodeBuilder newNode(final Environment host, final Visibility reachability) {
            return builder;
        }

        @Override
        public Packet newPacket(final String source, final String destination, final int port, final Object[] data) {
            this.source = source;
            this.destination = destination;
            this.port = port;
            packet = new TestPacket();
            return packet;
        }

        @Override
        public Packet newPacket(final CompoundTag nbt) {
            packet = new TestPacket();
            return packet;
        }
    }

    private static final class TestNodeBuilder implements Builder.NodeBuilder {
        private final TestComponentBuilder componentBuilder = new TestComponentBuilder(this);
        private final TestConnectorBuilder connectorBuilder = new TestConnectorBuilder(this);
        private final TestComponentConnectorBuilder componentConnectorBuilder = new TestComponentConnectorBuilder();
        private String componentName;
        private Visibility componentVisibility;
        private double bufferSize;

        @Override
        public Builder.ComponentBuilder withComponent(final String name, final Visibility visibility) {
            componentName = name;
            componentVisibility = visibility;
            return componentBuilder;
        }

        @Override
        public Builder.ComponentBuilder withComponent(final String name) {
            return withComponent(name, Visibility.Network);
        }

        @Override
        public Builder.ConnectorBuilder withConnector(final double bufferSize) {
            this.bufferSize = bufferSize;
            return connectorBuilder;
        }

        @Override
        public Builder.ConnectorBuilder withConnector() {
            return withConnector(0);
        }

        @Override
        public Node create() {
            return null;
        }
    }

    private static final class TestComponentBuilder implements Builder.ComponentBuilder {
        private final TestNodeBuilder owner;

        private TestComponentBuilder(final TestNodeBuilder owner) {
            this.owner = owner;
        }

        @Override
        public Builder.ComponentConnectorBuilder withConnector(final double bufferSize) {
            owner.bufferSize = bufferSize;
            return owner.componentConnectorBuilder;
        }

        @Override
        public Builder.ComponentConnectorBuilder withConnector() {
            return withConnector(0);
        }

        @Override
        public Component create() {
            return null;
        }
    }

    private static final class TestConnectorBuilder implements Builder.ConnectorBuilder {
        private final TestNodeBuilder owner;

        private TestConnectorBuilder(final TestNodeBuilder owner) {
            this.owner = owner;
        }

        @Override
        public Builder.ComponentConnectorBuilder withComponent(final String name, final Visibility visibility) {
            owner.componentName = name;
            owner.componentVisibility = visibility;
            return owner.componentConnectorBuilder;
        }

        @Override
        public Builder.ComponentConnectorBuilder withComponent(final String name) {
            return withComponent(name, Visibility.Network);
        }

        @Override
        public Connector create() {
            return null;
        }
    }

    private static final class TestComponentConnectorBuilder implements Builder.ComponentConnectorBuilder {
        @Override
        public ComponentConnector create() {
            return null;
        }
    }

    private static final class TestEnvironment implements Environment {
        @Override
        public Node node() {
            return null;
        }

        @Override
        public void onConnect(final Node node) {
        }

        @Override
        public void onDisconnect(final Node node) {
        }

        @Override
        public void onMessage(final Message message) {
        }
    }

    private static class TestNode implements Node {
        @Override
        public Environment host() {
            return null;
        }

        @Override
        public Visibility reachability() {
            return Visibility.Network;
        }

        @Override
        public String address() {
            return "node";
        }

        @Override
        public li.cil.oc.api.network.Network network() {
            return null;
        }

        @Override
        public boolean isNeighborOf(final Node other) {
            return false;
        }

        @Override
        public boolean canBeReachedFrom(final Node other) {
            return false;
        }

        @Override
        public Iterable<Node> neighbors() {
            return List.of();
        }

        @Override
        public Iterable<Node> reachableNodes() {
            return List.of();
        }

        @Override
        public void connect(final Node node) {
        }

        @Override
        public void disconnect(final Node node) {
        }

        @Override
        public void remove() {
        }

        @Override
        public void sendToAddress(final String target, final String name, final Object... data) {
        }

        @Override
        public void sendToNeighbors(final String name, final Object... data) {
        }

        @Override
        public void sendToReachable(final String name, final Object... data) {
        }

        @Override
        public void sendToVisible(final String name, final Object... data) {
        }

        @Override
        public void load(final CompoundTag nbt) {
        }

        @Override
        public void save(final CompoundTag nbt) {
        }
    }

    private static final class TestPacket implements Packet {
        @Override
        public String source() {
            return "source";
        }

        @Override
        public String destination() {
            return "destination";
        }

        @Override
        public int port() {
            return 1;
        }

        @Override
        public Object[] data() {
            return new Object[0];
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public int ttl() {
            return 0;
        }

        @Override
        public Packet hop() {
            return this;
        }

        @Override
        public void save(final CompoundTag nbt) {
        }
    }

    private static final class TestWirelessEndpoint implements WirelessEndpoint {
        @Override
        public int x() {
            return 0;
        }

        @Override
        public int y() {
            return 0;
        }

        @Override
        public int z() {
            return 0;
        }

        @Override
        public Level world() {
            return null;
        }

        @Override
        public void receivePacket(final Packet packet, final WirelessEndpoint sender) {
        }
    }

    @SuppressWarnings("unused")
    private static final class TestComponentConnector extends TestNode implements ComponentConnector {
        @Override
        public String name() {
            return "component";
        }

        @Override
        public Visibility visibility() {
            return Visibility.Network;
        }

        @Override
        public void setVisibility(final Visibility value) {
        }

        @Override
        public boolean canBeSeenFrom(final Node other) {
            return false;
        }

        @Override
        public Collection<String> methods() {
            return List.of();
        }

        @Override
        public Callback annotation(final String method) {
            return null;
        }

        @Override
        public Object[] invoke(final String method, final Context context, final Object... arguments) {
            return new Object[0];
        }

        @Override
        public double localBuffer() {
            return 0;
        }

        @Override
        public double localBufferSize() {
            return 0;
        }

        @Override
        public double globalBuffer() {
            return 0;
        }

        @Override
        public double globalBufferSize() {
            return 0;
        }

        @Override
        public double changeBuffer(final double delta) {
            return delta;
        }

        @Override
        public boolean tryChangeBuffer(final double delta) {
            return true;
        }

        @Override
        public void setLocalBufferSize(final double size) {
        }
    }
}
