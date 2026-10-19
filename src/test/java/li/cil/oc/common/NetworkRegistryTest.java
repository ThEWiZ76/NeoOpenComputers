package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.driver.MethodWhitelist;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.FilteredEnvironment;
import li.cil.oc.api.network.ManagedPeripheral;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NetworkRegistryTest {
    @Test
    void loadingConnectedAddressKeepsRoutingAndChainTopologyConsistent() {
        final var registry = deterministicRegistry();
        final var leftHost = new TestEnvironment();
        final var middleHost = new TestEnvironment();
        final var rightHost = new TestEnvironment();
        final var left = registry.newNode(leftHost, Visibility.Network).create();
        final var middle = registry.newNode(middleHost, Visibility.Network).create();
        final var right = registry.newNode(rightHost, Visibility.Network).create();
        leftHost.node = left; middleHost.node = middle; rightHost.node = right;
        registry.joinNewNetwork(left); left.connect(middle); middle.connect(right);
        final String oldAddress = middle.address();
        final var saved = new CompoundTag(); saved.putString("address", "restored-middle");
        middle.load(saved);
        assertEquals("restored-middle", middle.address());
        assertSame(middle, left.network().node("restored-middle"));
        assertEquals(null, left.network().node(oldAddress));
        assertTrue(left.isNeighborOf(middle) && middle.isNeighborOf(right));
        assertFalse(left.isNeighborOf(right));
        assertSame(left.network(), right.network());
        left.sendToAddress("restored-middle", "after-load", 731);
        assertEquals(List.of("after-load"), middleHost.messageNames());
        middle.remove();
        assertEquals(null, left.network().node("restored-middle"));
        assertFalse(left.network() == right.network(), "Removing the readdressed bridge must split the chain");
    }

    @Test
    void loadingDuplicateConnectedAddressPreservesTheExistingOwner() {
        final var registry = deterministicRegistry();
        final var firstHost = new TestEnvironment(); final var secondHost = new TestEnvironment();
        final var first = registry.newNode(firstHost, Visibility.Network).create();
        final var second = registry.newNode(secondHost, Visibility.Network).create();
        firstHost.node = first; secondHost.node = second;
        registry.joinNewNetwork(first); first.connect(second);
        final String owner = first.address(), previous = second.address();
        final var saved = new CompoundTag(); saved.putString("address", owner);
        second.load(saved);
        assertEquals(owner, first.address());
        assertNotEquals(owner, second.address());
        assertSame(first, first.network().node(owner));
        assertSame(second, first.network().node(second.address()));
        assertEquals(null, first.network().node(previous));
        assertTrue(first.isNeighborOf(second));
    }

    @Test
    void blankOrWrongTypeSavedAddressesDoNotInvalidateConnectedNode() {
        final var registry = deterministicRegistry();
        final var host = new TestEnvironment();
        final var node = registry.newNode(host, Visibility.Network).create(); host.node = node;
        registry.joinNewNetwork(node);
        final String address = node.address();
        final var blank = new CompoundTag(); blank.putString("address", "");
        node.load(blank);
        assertEquals(address, node.address());
        final var wrongType = new CompoundTag(); wrongType.putInt("address", 731);
        node.load(wrongType);
        assertEquals(address, node.address());
        assertSame(node, node.network().node(address));
    }

    @Test
    void networkDiscoverySkipsUnloadedNeighborsButConnectsLoadedNeighbors() {
        final var registry = deterministicRegistry();
        final var origin = new net.minecraft.core.BlockPos(15, 64, 8);
        final var host = new NetworkBlockEntity(registry, origin);
        final var neighbor = new NetworkBlockEntity(registry, origin.west());
        final var world = guardedWorld(Map.of(origin, host, origin.west(), neighbor), true);
        registry.joinOrCreateNetwork(world, origin);
        assertTrue(host.node().isNeighborOf(neighbor.node()), "Skipping unloaded chunks must not break loaded neighbors");
    }

    @Test
    void networkDiscoveryDoesNotLoadItsOriginChunk() {
        final var registry = deterministicRegistry();
        final var world = guardedWorld(Map.of(), false);
        registry.joinOrCreateNetwork(world, new net.minecraft.core.BlockPos(15, 64, 8));
    }

    @Test
    void networkDiscoveryDoesNotAskSidedHostsAboutUnloadedNeighbors() {
        final var registry = deterministicRegistry();
        final var origin = new net.minecraft.core.BlockPos(15, 64, 8);
        final var host = new SidedNetworkBlockEntity(registry, origin);
        final var neighbor = new NetworkBlockEntity(registry, origin.west());
        registry.joinOrCreateNetwork(guardedWorld(Map.of(origin, host, origin.west(), neighbor), true), origin);
        assertTrue(host.node().isNeighborOf(neighbor.node()));
    }

    private static net.minecraft.world.level.LevelReader guardedWorld(
        Map<net.minecraft.core.BlockPos, NetworkBlockEntity> blocks, boolean originLoaded) {
        return (net.minecraft.world.level.LevelReader) java.lang.reflect.Proxy.newProxyInstance(
            NetworkRegistryTest.class.getClassLoader(), new Class<?>[]{net.minecraft.world.level.LevelReader.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "hasChunk" -> originLoaded && (int) args[0] == 0 && (int) args[1] == 0;
                case "hasChunkAt" -> {
                    final var pos = (net.minecraft.core.BlockPos) args[0];
                    yield originLoaded && pos.getX() >> 4 == 0 && pos.getZ() >> 4 == 0;
                }
                case "getBlockEntity" -> {
                    final var pos = (net.minecraft.core.BlockPos) args[0];
                    if (!originLoaded || pos.getX() >> 4 != 0 || pos.getZ() >> 4 != 0)
                        throw new AssertionError("Network discovery queried an unloaded chunk: " + pos);
                    yield blocks.get(pos);
                }
                default -> throw new AssertionError("Unexpected world access: " + method.getName());
            });
    }

    private static class NetworkBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity implements Environment {
        private final Node node;
        NetworkBlockEntity(NetworkRegistry registry, net.minecraft.core.BlockPos pos) {
            super(null, pos, null);
            node = registry.newNode(this, Visibility.Network).create();
        }
        @Override public boolean isValidBlockState(net.minecraft.world.level.block.state.BlockState state) { return true; }
        @Override public Node node() { return node; }
        @Override public void onConnect(Node other) { }
        @Override public void onDisconnect(Node other) { }
        @Override public void onMessage(Message message) { }
    }

    private static final class SidedNetworkBlockEntity extends NetworkBlockEntity implements li.cil.oc.api.network.SidedEnvironment {
        SidedNetworkBlockEntity(NetworkRegistry registry, net.minecraft.core.BlockPos pos) { super(registry, pos); }
        @Override public Node sidedNode(net.minecraft.core.Direction side) { return node(); }
        @Override public boolean canConnect(net.minecraft.core.Direction side) {
            if (getBlockPos().relative(side).getX() >> 4 != 0)
                throw new AssertionError("Sided host was asked to inspect an unloaded neighbor");
            return true;
        }
    }

    @Test
    void newNetworkAddressesDoNotReuseLoadedAddressesAfterRegistryRestart() {
        final var registry = deterministicRegistry();
        final var restoredHost = new TestEnvironment();
        final var restored = registry.newNode(restoredHost, Visibility.Network).create();
        restoredHost.node = restored;
        final var saved = new CompoundTag();
        saved.putString("address", "node-1");
        restored.load(saved);
        registry.joinNewNetwork(restored);
        final var freshHost = new TestEnvironment();
        final var fresh = registry.newNode(freshHost, Visibility.Network).create();
        freshHost.node = fresh;
        registry.joinNewNetwork(fresh);
        assertNotEquals(restored.address(), fresh.address(), "Separate loaded and new networks must not share a global device address");
        assertEquals("node-1", restored.address(), "Existing persisted addresses must be preserved");
    }

    @AfterEach
    void resetApi() {
        API.network = null;
        API.driver = null;
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
    void newPacketsUseConfiguredInitialTtl() throws Exception {
        NetworkRegistry registry = new NetworkRegistry();

        withCachedConfig(ModSettings.INITIAL_NETWORK_PACKET_TTL, 9, () -> {
            Packet packet = registry.newPacket("node-1", "node-2", 42, new Object[]{"ping"});

            assertEquals(9, packet.ttl());
            assertEquals(8, packet.hop().ttl());
        });
    }

    @Test
    void rejectsPacketsWithTooManyDataParts() {
        NetworkRegistry registry = new NetworkRegistry();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> registry.newPacket("node-1", "node-2", 42, new Object[]{1, 2, 3, 4, 5, 6, 7, 8, 9}));

        assertEquals("packet has too many parts", exception.getMessage());
    }

    @Test
    void rejectsPacketsOverConfiguredDataPartCount() throws Exception {
        NetworkRegistry registry = new NetworkRegistry();

        withCachedConfig(ModSettings.MAX_NETWORK_PACKET_PARTS, 4, () -> {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> registry.newPacket("node-1", "node-2", 42, new Object[]{1, 2, 3, 4, 5}));

            assertEquals("packet has too many parts", exception.getMessage());
        });
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
        NetworkRegistry registry = deterministicRegistry();
        TestEnvironment hostA = new TestEnvironment();
        TestEnvironment hostB = new TestEnvironment();
        Node nodeA = registry.newNode(hostA, Visibility.Network).create();
        Component nodeB = registry.newNode(hostB, Visibility.Network).withComponent("screen", Visibility.Network).create();
        hostA.node = nodeA;
        hostB.node = nodeB;

        registry.joinNewNetwork(nodeA);
        nodeA.connect(nodeB);
        nodeA.sendToAddress(nodeB.address(), "beep", "payload");

        assertEquals("00000000-0000-0000-0000-000000000001", nodeA.address());
        assertEquals("00000000-0000-0000-0000-000000000002", nodeB.address());
        assertSame(nodeA.network(), nodeB.network());
        assertTrue(nodeA.isNeighborOf(nodeB));
        assertTrue(nodeB.canBeReachedFrom(nodeA));
        assertTrue(nodeB.canBeSeenFrom(nodeA));
        assertEquals("screen", nodeB.name());
        assertEquals(List.of("beep"), hostB.messageNames());
        assertArrayEquals(new Object[]{"payload"}, hostB.messages.getFirst().data());
    }

    @Test
    void componentVisibilityChangesNotifyReachableNodesLikeUpstream() {
        NetworkRegistry registry = new NetworkRegistry();
        RecordingEnvironment machineHost = new RecordingEnvironment();
        TestEnvironment screenHost = new TestEnvironment();
        Node machineNode = registry.newNode(machineHost, Visibility.Network).create();
        Component screenNode = registry.newNode(screenHost, Visibility.Network).withComponent("screen", Visibility.Network).create();
        machineHost.setNode(machineNode);
        screenHost.node = screenNode;
        registry.joinNewNetwork(machineNode);
        machineNode.connect(screenNode);
        machineHost.connected.clear();
        machineHost.disconnected.clear();

        screenNode.setVisibility(Visibility.None);

        assertEquals(List.of(screenNode), machineHost.disconnected);
        assertEquals(List.of(), machineHost.connected);
        machineHost.connected.clear();
        machineHost.disconnected.clear();

        screenNode.setVisibility(Visibility.Network);

        assertEquals(List.of(screenNode), machineHost.connected);
        assertEquals(List.of(), machineHost.disconnected);
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
    void componentCallbackReturnsUseRegisteredConvertersLikeUpstream() throws Exception {
        DriverRegistry driverRegistry = new DriverRegistry();
        TestConvertedValue value = new TestConvertedValue("component");
        driverRegistry.add((candidate, output) -> {
            if (candidate == value) {
                output.put("name", value.name);
            }
        });
        API.driver = driverRegistry;
        NetworkRegistry registry = new NetworkRegistry();
        ConvertedCallbackEnvironment host = new ConvertedCallbackEnvironment(value);
        Component component = registry.newNode(host, Visibility.Network).withComponent("test", Visibility.Network).create();

        Object[] result = component.invoke("converted", null);

        assertEquals(Map.of("name", "component"), result[0]);
    }

    @Test
    void componentsHonorCallbackFiltersLikeUpstream() {
        NetworkRegistry registry = new NetworkRegistry();
        FilteredTestEnvironment host = new FilteredTestEnvironment();
        Component component = registry.newNode(host, Visibility.Network).withComponent("test", Visibility.Network).create();

        assertTrue(component.methods().contains("ping"));
        assertFalse(component.methods().contains("hidden"));
        assertFalse(component.methods().contains("extra"));
        assertThrows(NoSuchMethodException.class, () -> component.invoke("hidden", null));
        assertThrows(NoSuchMethodException.class, () -> component.invoke("extra", null));
    }

    @Test
    void componentsIgnoreInvalidCallbackShapesLikeUpstream() {
        NetworkRegistry registry = new NetworkRegistry();
        InvalidCallbackEnvironment host = new InvalidCallbackEnvironment();
        Component component = registry.newNode(host, Visibility.Network).withComponent("test", Visibility.Network).create();

        assertTrue(component.methods().contains("valid"));
        assertFalse(component.methods().contains("badReturn"));
        assertFalse(component.methods().contains("badArgs"));
        assertFalse(component.methods().contains("privateCallback"));
        assertThrows(NoSuchMethodException.class, () -> component.invoke("badReturn", null));
        assertThrows(NoSuchMethodException.class, () -> component.invoke("badArgs", null));
        assertThrows(NoSuchMethodException.class, () -> component.invoke("privateCallback", null));
    }

    @Test
    void componentsUseMethodNameForBlankCallbackNamesLikeUpstream() throws Exception {
        NetworkRegistry registry = new NetworkRegistry();
        BlankCallbackNameEnvironment host = new BlankCallbackNameEnvironment();
        Component component = registry.newNode(host, Visibility.Network).withComponent("test", Visibility.Network).create();

        assertTrue(component.methods().contains("fallback"));
        assertFalse(component.methods().contains("   "));
        assertArrayEquals(new Object[]{"fallback"}, component.invoke("fallback", null));
    }

    @Test
    void componentsExposeManagedPeripheralMethodsLikeUpstream() throws Exception {
        NetworkRegistry registry = new NetworkRegistry();
        PeripheralEnvironment host = new PeripheralEnvironment();
        Component component = registry.newNode(host, Visibility.Network).withComponent("test", Visibility.Network).create();

        Callback annotation = component.annotation("dynamic");

        assertTrue(component.methods().contains("dynamic"));
        assertEquals("dynamic", annotation.value());
        assertTrue(annotation.direct());
        assertEquals(100, annotation.limit());
        assertArrayEquals(new Object[]{"dynamic", "payload"}, component.invoke("dynamic", null, "payload"));
    }

    @Test
    void componentArgumentsTreatNonNaNNumbersAsIntegersLikeUpstream() throws Exception {
        NetworkRegistry registry = new NetworkRegistry();
        NumericArgumentsEnvironment host = new NumericArgumentsEnvironment();
        Component component = registry.newNode(host, Visibility.Network).withComponent("test", Visibility.Network).create();

        Object[] result = component.invoke("integerFlags", null, 1.5D, Float.POSITIVE_INFINITY);

        assertArrayEquals(new Object[]{true, true, true, true}, result);
    }

    @Test
    void componentArgumentsClampLongIntegerOverflowLikeUpstream() throws Exception {
        NetworkRegistry registry = new NetworkRegistry();
        NumericArgumentsEnvironment host = new NumericArgumentsEnvironment();
        Component component = registry.newNode(host, Visibility.Network).withComponent("test", Visibility.Network).create();

        Object[] result = component.invoke("clampIntegers", null, Long.MAX_VALUE, Long.MIN_VALUE);

        assertArrayEquals(new Object[]{Integer.MAX_VALUE, Integer.MIN_VALUE}, result);
    }

    @Test
    void componentArgumentsRejectNaNIntegersLikeUpstream() {
        NetworkRegistry registry = new NetworkRegistry();
        NumericArgumentsEnvironment host = new NumericArgumentsEnvironment();
        Component component = registry.newNode(host, Visibility.Network).withComponent("test", Visibility.Network).create();

        assertThrows(IllegalArgumentException.class, () -> component.invoke("checkInteger", null, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> component.invoke("checkLong", null, Float.NaN));
    }

    @Test
    void componentArgumentsTreatStringsAsByteArraysLikeUpstream() throws Exception {
        NetworkRegistry registry = new NetworkRegistry();
        NumericArgumentsEnvironment host = new NumericArgumentsEnvironment();
        Component component = registry.newNode(host, Visibility.Network).withComponent("test", Visibility.Network).create();

        Object[] result = component.invoke("byteArrayFlags", null, "payload", "bytes".getBytes(StandardCharsets.UTF_8));

        assertArrayEquals(new Object[]{true, true}, result);
    }

    @Test
    void componentOptionalArgumentsTreatNullAsMissingLikeUpstream() throws Exception {
        NetworkRegistry registry = new NetworkRegistry();
        NumericArgumentsEnvironment host = new NumericArgumentsEnvironment();
        Component component = registry.newNode(host, Visibility.Network).withComponent("test", Visibility.Network).create();

        Object[] result = component.invoke("optionalDefaults", null, null, null, null);

        assertArrayEquals(new Object[]{"fallback", 42, "any"}, result);
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

    @Test
    void ignoredPowerAllowsConnectorDrainsWithoutChangingBuffer() throws Exception {
        NetworkRegistry registry = new NetworkRegistry();
        Connector connector = registry.newNode(new TestEnvironment(), Visibility.Network).withConnector(10).create();
        connector.changeBuffer(5);

        withCachedConfig(ModSettings.IGNORE_POWER, true, () -> {
            assertEquals(5, connector.localBuffer());
            assertEquals(0, connector.changeBuffer(-8));
            assertEquals(5, connector.localBuffer());
            assertTrue(connector.tryChangeBuffer(-8));
            assertEquals(5, connector.localBuffer());
        });
    }

    @Test
    void ignoredPowerRejectsConnectorChargesWithoutChangingBuffer() throws Exception {
        withCachedConfig(ModSettings.IGNORE_POWER, true, () -> {
            NetworkRegistry registry = new NetworkRegistry();
            Connector connector = registry.newNode(new TestEnvironment(), Visibility.Network).withConnector(10).create();

            assertEquals(3, connector.changeBuffer(3));
            assertEquals(0, connector.localBuffer());
            assertFalse(connector.tryChangeBuffer(3));
            assertEquals(0, connector.localBuffer());
        });
    }

    private static class TestEnvironment implements Environment {
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

        void setNode(final Node node) {
            this.node = node;
        }

        @Callback(doc = "function():string -- Test callback.")
        public Object[] ping(final Context context, final Arguments arguments) {
            return new Object[]{"pong", arguments.checkString(0)};
        }
    }

    private static final class RecordingEnvironment extends TestEnvironment {
        private final List<Node> connected = new ArrayList<>();
        private final List<Node> disconnected = new ArrayList<>();

        @Override
        public void onConnect(final Node node) {
            connected.add(node);
        }

        @Override
        public void onDisconnect(final Node node) {
            disconnected.add(node);
        }
    }

    private static final class ConvertedCallbackEnvironment extends TestEnvironment {
        private final Object value;

        private ConvertedCallbackEnvironment(final Object value) {
            this.value = value;
        }

        @Callback
        public Object[] converted(final Context context, final Arguments arguments) {
            return new Object[]{value};
        }
    }

    private record TestConvertedValue(String name) {
    }

    private static final class BlankCallbackNameEnvironment extends TestEnvironment {
        @Callback("   ")
        public Object[] fallback(final Context context, final Arguments arguments) {
            return new Object[]{"fallback"};
        }
    }

    private static final class PeripheralEnvironment extends TestEnvironment implements ManagedPeripheral {
        @Override
        public String[] methods() {
            return new String[]{"dynamic"};
        }

        @Override
        public Object[] invoke(final String method, final Context context, final Arguments args) throws Exception {
            if (!"dynamic".equals(method)) {
                throw new NoSuchMethodException(method);
            }
            return new Object[]{method, args.checkString(0)};
        }
    }

    private static final class InvalidCallbackEnvironment extends TestEnvironment {
        @Callback
        public Object[] valid(final Context context, final Arguments arguments) {
            return new Object[]{"valid"};
        }

        @Callback
        public String badReturn(final Context context, final Arguments arguments) {
            return "bad";
        }

        @Callback
        public Object[] badArgs() {
            return new Object[]{"bad"};
        }

        @Callback
        private Object[] privateCallback(final Context context, final Arguments arguments) {
            return new Object[]{"bad"};
        }
    }

    private static final class NumericArgumentsEnvironment extends TestEnvironment {
        @Callback
        public Object[] integerFlags(final Context context, final Arguments arguments) {
            return new Object[]{
                arguments.isInteger(0),
                arguments.isInteger(1),
                arguments.isLong(0),
                arguments.isLong(1)
            };
        }

        @Callback
        public Object[] clampIntegers(final Context context, final Arguments arguments) {
            return new Object[]{
                arguments.checkInteger(0),
                arguments.checkInteger(1)
            };
        }

        @Callback
        public Object[] checkInteger(final Context context, final Arguments arguments) {
            return new Object[]{arguments.checkInteger(0)};
        }

        @Callback
        public Object[] checkLong(final Context context, final Arguments arguments) {
            return new Object[]{arguments.checkLong(0)};
        }

        @Callback
        public Object[] byteArrayFlags(final Context context, final Arguments arguments) {
            return new Object[]{
                arguments.isByteArray(0),
                arguments.isByteArray(1)
            };
        }

        @Callback
        public Object[] optionalDefaults(final Context context, final Arguments arguments) {
            return new Object[]{
                arguments.optString(0, "fallback"),
                arguments.optInteger(1, 42),
                arguments.optAny(2, "any")
            };
        }
    }

    private static final class FilteredTestEnvironment extends TestEnvironment implements FilteredEnvironment, MethodWhitelist {
        @Override
        public boolean isCallbackEnabled(final String name) {
            return !"hidden".equals(name);
        }

        @Override
        public String[] whitelistedMethods() {
            return new String[]{"ping", "hidden"};
        }

        @Callback
        public Object[] hidden(final Context context, final Arguments arguments) {
            return new Object[]{"hidden"};
        }

        @Callback
        public Object[] extra(final Context context, final Arguments arguments) {
            return new Object[]{"extra"};
        }
    }

    private static NetworkRegistry deterministicRegistry() {
        final var sequence = new java.util.concurrent.atomic.AtomicLong();
        return new NetworkRegistry(() -> new java.util.UUID(0, sequence.incrementAndGet()));
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
