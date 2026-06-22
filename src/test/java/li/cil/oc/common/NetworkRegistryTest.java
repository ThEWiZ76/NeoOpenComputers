package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NetworkRegistryTest {
    @AfterEach
    void resetApi() {
        API.network = null;
    }

    @Test
    void bootstrapInstallsNetworkApi() {
        OpenComputersApi.initialize();

        assertInstanceOf(NetworkRegistry.class, API.network);
    }

    @Test
    void packetsRoundTripThroughNbt() {
        NetworkRegistry registry = new NetworkRegistry();
        Packet packet = registry.newPacket("node-1", "node-2", 42, new Object[]{"ping", 7, true, new byte[]{1, 2}});
        CompoundTag nbt = new CompoundTag();

        packet.save(nbt);
        Packet loaded = registry.newPacket(nbt);

        assertEquals("node-1", loaded.source());
        assertEquals("node-2", loaded.destination());
        assertEquals(42, loaded.port());
        assertArrayEquals(new Object[]{"ping", 7, true, new byte[]{1, 2}}, loaded.data());
        assertEquals(packet.ttl() - 1, packet.hop().ttl());
        assertTrue(packet.size() > 0);
    }

    @Test
    void rejectsPacketsWithTooManyDataParts() {
        NetworkRegistry registry = new NetworkRegistry();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> registry.newPacket("node-1", "node-2", 42, new Object[]{1, 2, 3, 4, 5, 6, 7, 8, 9}));

        assertEquals("packet has too many parts", exception.getMessage());
    }

    @Test
    void rejectsPacketsOverMaximumPayloadSize() {
        NetworkRegistry registry = new NetworkRegistry();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> registry.newPacket("node-1", "node-2", 42, new Object[]{"x".repeat(8191)}));

        assertEquals("packet too big (max 8192)", exception.getMessage());
    }

    @Test
    void rejectsPacketsOverConfiguredPayloadSize() throws Exception {
        NetworkRegistry registry = new NetworkRegistry();

        withCachedConfig(ModSettings.MAX_NETWORK_PACKET_SIZE, 32, () -> {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> registry.newPacket("node-1", "node-2", 42, new Object[]{"x".repeat(31)}));

            assertEquals("packet too big (max 32)", exception.getMessage());
        });
    }

    @Test
    void nodesJoinNetworksAndDeliverMessages() {
        NetworkRegistry registry = new NetworkRegistry();
        TestEnvironment hostA = new TestEnvironment();
        TestEnvironment hostB = new TestEnvironment();
        Node nodeA = registry.newNode(hostA, Visibility.Network).create();
        Component nodeB = registry.newNode(hostB, Visibility.Network).withComponent("screen", Visibility.Network).create();
        hostA.node = nodeA;
        hostB.node = nodeB;

        registry.joinNewNetwork(nodeA);
        nodeA.connect(nodeB);
        nodeA.sendToAddress(nodeB.address(), "beep", "payload");

        assertEquals("node-1", nodeA.address());
        assertEquals("node-2", nodeB.address());
        assertSame(nodeA.network(), nodeB.network());
        assertTrue(nodeA.isNeighborOf(nodeB));
        assertTrue(nodeB.canBeReachedFrom(nodeA));
        assertTrue(nodeB.canBeSeenFrom(nodeA));
        assertEquals("screen", nodeB.name());
        assertEquals(List.of("beep"), hostB.messageNames());
        assertArrayEquals(new Object[]{"payload"}, hostB.messages.getFirst().data());
    }

    @Test
    void nodeJoinsAndConnectsAdjacentNeighborNodes() {
        NetworkRegistry registry = new NetworkRegistry();
        TestEnvironment hostA = new TestEnvironment();
        TestEnvironment hostB = new TestEnvironment();
        TestEnvironment hostC = new TestEnvironment();
        Node nodeA = registry.newNode(hostA, Visibility.Network).create();
        Node nodeB = registry.newNode(hostB, Visibility.Network).create();
        Node nodeC = registry.newNode(hostC, Visibility.Network).create();

        registry.joinOrCreateNetwork(nodeA, List.of(nodeB, nodeC));

        assertSame(nodeA.network(), nodeB.network());
        assertSame(nodeA.network(), nodeC.network());
        assertTrue(nodeA.isNeighborOf(nodeB));
        assertTrue(nodeA.isNeighborOf(nodeC));
    }

    @Test
    void componentsExposeCallbackMethods() throws Exception {
        NetworkRegistry registry = new NetworkRegistry();
        TestEnvironment host = new TestEnvironment();
        Component component = registry.newNode(host, Visibility.Network).withComponent("test", Visibility.Network).create();

        assertTrue(component.methods().contains("ping"));
        assertEquals("function():string -- Test callback.", component.annotation("ping").doc());
        assertArrayEquals(new Object[]{"pong", "data"}, component.invoke("ping", null, "data"));
    }

    @Test
    void connectorBuffersClampToLocalSize() {
        NetworkRegistry registry = new NetworkRegistry();
        Connector connector = registry.newNode(new TestEnvironment(), Visibility.Network).withConnector(10).create();

        assertEquals(0, connector.changeBuffer(6));
        assertEquals(6, connector.localBuffer());
        assertFalse(connector.tryChangeBuffer(5));
        assertTrue(connector.tryChangeBuffer(-4));
        assertEquals(2, connector.localBuffer());
        connector.setLocalBufferSize(1);
        assertEquals(1, connector.localBufferSize());
        assertEquals(1, connector.localBuffer());
    }

    private static final class TestEnvironment implements Environment {
        private final List<Message> messages = new ArrayList<>();
        private Node node;

        @Override
        public Node node() {
            return node;
        }

        @Override
        public void onConnect(final Node node) {
        }

        @Override
        public void onDisconnect(final Node node) {
        }

        @Override
        public void onMessage(final Message message) {
            messages.add(message);
        }

        private List<String> messageNames() {
            return messages.stream().map(Message::name).toList();
        }

        @Callback(doc = "function():string -- Test callback.")
        public Object[] ping(final Context context, final Arguments arguments) {
            return new Object[]{"pong", arguments.checkString(0)};
        }
    }

    private static <T> void withCachedConfig(final ModConfigSpec.ConfigValue<T> value, final T override, final ThrowingRunnable action) throws Exception {
        final Field cachedValue = ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
        cachedValue.setAccessible(true);
        final Object previous = cachedValue.get(value);
        cachedValue.set(value, override);
        try {
            action.run();
        } finally {
            cachedValue.set(value, previous);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
