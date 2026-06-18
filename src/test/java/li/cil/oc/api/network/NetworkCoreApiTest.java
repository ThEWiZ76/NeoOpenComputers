package li.cil.oc.api.network;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NetworkCoreApiTest {
    @Test
    void environmentReceivesNodeLifecycleAndMessages() {
        var node = new TestNode("node-a");
        var message = new TestMessage(node, "ping", new Object[]{"data"});
        var environment = new TestEnvironment(node);

        environment.onConnect(node);
        environment.onMessage(message);
        environment.onDisconnect(node);

        assertSame(node, environment.node());
        assertSame(node, environment.connected);
        assertSame(node, environment.disconnected);
        assertSame(message, environment.message);
    }

    @Test
    void managedEnvironmentAddsPersistenceAndTickLifecycle() {
        var environment = new TestManagedEnvironment(new TestNode("managed"));

        environment.save(new CompoundTag());
        environment.load(new CompoundTag());
        environment.update();

        assertTrue(environment.canUpdate());
        assertTrue(environment.saved);
        assertTrue(environment.loaded);
        assertTrue(environment.updated);
    }

    @Test
    void messageExposesSourceNameDataAndCancel() {
        var source = new TestNode("source");
        var message = new TestMessage(source, "event", new Object[]{1, "two"});

        message.cancel();

        assertSame(source, message.source());
        assertEquals("event", message.name());
        assertArrayEquals(new Object[]{1, "two"}, message.data());
        assertTrue(message.cancelled);
    }

    @Test
    void nodeExposesNetworkGraphAndMessageOperations() {
        var network = new TestNetwork();
        var node = new TestNode("node-a");
        var other = new TestNode("node-b");
        node.network = network;
        node.neighbors = List.of(other);

        node.connect(other);
        node.sendToAddress("node-b", "ping", 1);
        node.sendToNeighbors("near", 2);
        node.sendToReachable("reachable", 3);
        node.sendToVisible("visible", 4);

        assertSame(node, node.host().node());
        assertEquals("node-a", node.address());
        assertSame(network, node.network());
        assertSame(other, node.neighbors().iterator().next());
        assertTrue(node.isNeighborOf(other));
        assertTrue(network.connected);
        assertEquals("visible", network.lastMessageName);
    }

    @Test
    void networkExposesGraphQueriesAndSendOperations() {
        var network = new TestNetwork();
        var nodeA = new TestNode("a");
        var nodeB = new TestNode("b");

        assertTrue(network.connect(nodeA, nodeB));
        assertTrue(network.disconnect(nodeA, nodeB));
        assertTrue(network.remove(nodeA));
        network.sendToAddress(nodeA, "b", "address");
        network.sendToNeighbors(nodeA, "neighbors");
        network.sendToReachable(nodeA, "reachable");
        network.sendToVisible(nodeA, "visible");

        assertSame(nodeA, network.node("a"));
        assertEquals(List.of(nodeA, nodeB), network.nodes());
        assertEquals(List.of(nodeB), network.neighbors(nodeA));
        assertEquals("visible", network.lastMessageName);
    }

    private static class TestEnvironment implements Environment {
        private final Node node;
        private Node connected;
        private Node disconnected;
        private Message message;

        private TestEnvironment(final Node node) {
            this.node = node;
        }

        @Override
        public Node node() {
            return node;
        }

        @Override
        public void onConnect(final Node node) {
            connected = node;
        }

        @Override
        public void onDisconnect(final Node node) {
            disconnected = node;
        }

        @Override
        public void onMessage(final Message message) {
            this.message = message;
        }
    }

    private static final class TestManagedEnvironment extends TestEnvironment implements ManagedEnvironment {
        private boolean saved;
        private boolean loaded;
        private boolean updated;

        private TestManagedEnvironment(final Node node) {
            super(node);
        }

        @Override
        public boolean canUpdate() {
            return true;
        }

        @Override
        public void update() {
            updated = true;
        }

        @Override
        public void load(final CompoundTag nbt) {
            loaded = true;
        }

        @Override
        public void save(final CompoundTag nbt) {
            saved = true;
        }
    }

    private static final class TestNode implements Node {
        private final String address;
        private Network network;
        private Iterable<Node> neighbors = List.of();

        private TestNode(final String address) {
            this.address = address;
        }

        @Override
        public Environment host() {
            return new TestEnvironment(this);
        }

        @Override
        public Visibility reachability() {
            return Visibility.Network;
        }

        @Override
        public String address() {
            return address;
        }

        @Override
        public Network network() {
            return network;
        }

        @Override
        public boolean isNeighborOf(final Node other) {
            return neighbors.iterator().next() == other;
        }

        @Override
        public boolean canBeReachedFrom(final Node other) {
            return true;
        }

        @Override
        public Iterable<Node> neighbors() {
            return neighbors;
        }

        @Override
        public Iterable<Node> reachableNodes() {
            return neighbors;
        }

        @Override
        public void connect(final Node node) {
            network.connect(this, node);
        }

        @Override
        public void disconnect(final Node node) {
            network.disconnect(this, node);
        }

        @Override
        public void remove() {
            network.remove(this);
        }

        @Override
        public void sendToAddress(final String target, final String name, final Object... data) {
            network.sendToAddress(this, target, name, data);
        }

        @Override
        public void sendToNeighbors(final String name, final Object... data) {
            network.sendToNeighbors(this, name, data);
        }

        @Override
        public void sendToReachable(final String name, final Object... data) {
            network.sendToReachable(this, name, data);
        }

        @Override
        public void sendToVisible(final String name, final Object... data) {
            network.sendToVisible(this, name, data);
        }

        @Override
        public void load(final CompoundTag nbt) {
        }

        @Override
        public void save(final CompoundTag nbt) {
        }
    }

    private static final class TestMessage implements Message {
        private final Node source;
        private final String name;
        private final Object[] data;
        private boolean cancelled;

        private TestMessage(final Node source, final String name, final Object[] data) {
            this.source = source;
            this.name = name;
            this.data = data;
        }

        @Override
        public Node source() {
            return source;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Object[] data() {
            return data;
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }

    private static final class TestNetwork implements Network {
        private Node nodeA;
        private Node nodeB;
        private boolean connected;
        private String lastMessageName;

        @Override
        public boolean connect(final Node nodeA, final Node nodeB) {
            this.nodeA = nodeA;
            this.nodeB = nodeB;
            connected = true;
            return true;
        }

        @Override
        public boolean disconnect(final Node nodeA, final Node nodeB) {
            return true;
        }

        @Override
        public boolean remove(final Node node) {
            return true;
        }

        @Override
        public Node node(final String address) {
            return nodeA;
        }

        @Override
        public Iterable<Node> nodes() {
            return List.of(nodeA, nodeB);
        }

        @Override
        public Iterable<Node> nodes(final Node reference) {
            return List.of(nodeB);
        }

        @Override
        public Iterable<Node> neighbors(final Node node) {
            return List.of(nodeB);
        }

        @Override
        public void sendToAddress(final Node source, final String target, final String name, final Object... data) {
            lastMessageName = name;
        }

        @Override
        public void sendToNeighbors(final Node source, final String name, final Object... data) {
            lastMessageName = name;
        }

        @Override
        public void sendToReachable(final Node source, final String name, final Object... data) {
            lastMessageName = name;
        }

        @Override
        public void sendToVisible(final Node source, final String name, final Object... data) {
            lastMessageName = name;
        }
    }
}
