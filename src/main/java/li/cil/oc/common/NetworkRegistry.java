package li.cil.oc.common;

import li.cil.oc.api.detail.Builder;
import li.cil.oc.api.detail.NetworkAPI;
import li.cil.oc.api.driver.MethodWhitelist;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.FilteredEnvironment;
import li.cil.oc.api.network.ManagedPeripheral;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Network;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.network.WirelessEndpoint;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.common.driver.CompoundBlockEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

final class NetworkRegistry implements NetworkAPI {
    private int nextNodeId = 1;
    private final Set<WirelessEndpoint> wirelessEndpoints = new LinkedHashSet<>();

    @Override
    public void joinOrCreateNetwork(final BlockEntity blockEntity) {
        if (blockEntity != null) {
            joinOrCreateNetwork(blockEntity.getLevel(), blockEntity.getBlockPos());
        }
    }

    @Override
    public void joinOrCreateNetwork(final BlockGetter world, final BlockPos pos) {
        if (world == null || pos == null) {
            return;
        }
        if (world.getBlockEntity(pos) instanceof SidedEnvironment sidedEnvironment) {
            for (final Direction direction : Direction.values()) {
                if (sidedEnvironment.canConnect(direction)) {
                    final Node adjacentNode = adjacentNode(world, pos, direction);
                    joinOrCreateNetwork(sidedEnvironment.sidedNode(direction), adjacentNode == null ? List.of() : List.of(adjacentNode));
                }
            }
            return;
        }
        if (world.getBlockEntity(pos) instanceof Environment environment) {
            joinOrCreateNetwork(environment.node(), adjacentNodes(world, pos));
        }
    }

    void joinOrCreateNetwork(final Node node, final Iterable<Node> adjacentNodes) {
        if (node == null) {
            return;
        }
        if (node.network() == null) {
            joinNewNetwork(node);
        }
        if (adjacentNodes != null) {
            for (Node adjacentNode : adjacentNodes) {
                if (adjacentNode != null) {
                    node.connect(adjacentNode);
                }
            }
        }
    }

    @Override
    public void joinNewNetwork(final Node node) {
        if (node instanceof BaseNode baseNode && baseNode.network == null) {
            new WiredNetwork(baseNode);
        }
    }

    @Override
    public void joinWirelessNetwork(final WirelessEndpoint endpoint) {
        if (endpoint != null) {
            wirelessEndpoints.add(endpoint);
        }
    }

    @Override
    public void updateWirelessNetwork(final WirelessEndpoint endpoint) {
        if (endpoint != null) {
            wirelessEndpoints.add(endpoint);
        }
    }

    @Override
    public void leaveWirelessNetwork(final WirelessEndpoint endpoint) {
        wirelessEndpoints.remove(endpoint);
    }

    @Override
    public void leaveWirelessNetwork(final WirelessEndpoint endpoint, final ResourceKey<Level> dimension) {
        if (endpoint == null) {
            return;
        }
        final Level world = endpoint.world();
        if (world == null || world.dimension().equals(dimension)) {
            leaveWirelessNetwork(endpoint);
        }
    }

    @Override
    public void sendWirelessPacket(final WirelessEndpoint source, final double strength, final Packet packet) {
        if (source == null || packet == null || strength <= 0D) {
            return;
        }
        for (WirelessEndpoint endpoint : List.copyOf(wirelessEndpoints)) {
            if (endpoint != source && endpoint.world() == source.world() && wirelessDistance(source, endpoint) <= strength) {
                endpoint.receivePacket(packet, source);
            }
        }
    }

    @Override
    public Builder.NodeBuilder newNode(final Environment host, final Visibility reachability) {
        return new NodeBuilder(host, reachability);
    }

    @Override
    public Packet newPacket(final String source, final String destination, final int port, final Object[] data) {
        final Packet packet = new PacketImpl(source, destination, port, data, ModSettings.initialNetworkPacketTtl());
        final int maxPacketSize = ModSettings.maxNetworkPacketSize();
        if (packet.size() > maxPacketSize) {
            throw new IllegalArgumentException("packet too big (max " + maxPacketSize + ")");
        }
        return packet;
    }

    @Override
    public Packet newPacket(final CompoundTag nbt) {
        final Object[] data = new Object[nbt.getInt("dataLength")];
        for (int index = 0; index < data.length; index++) {
            final Tag tag = nbt.get("data" + index);
            if (tag instanceof ByteTag value) {
                data[index] = value.getAsByte() != 0;
            } else if (tag instanceof ShortTag value) {
                data[index] = value.getAsShort();
            } else if (tag instanceof IntTag value) {
                data[index] = value.getAsInt();
            } else if (tag instanceof LongTag value) {
                data[index] = value.getAsLong();
            } else if (tag instanceof FloatTag value) {
                data[index] = value.getAsFloat();
            } else if (tag instanceof DoubleTag value) {
                data[index] = value.getAsDouble();
            } else if (tag instanceof StringTag value) {
                data[index] = value.getAsString();
            } else if (tag instanceof ByteArrayTag value) {
                data[index] = value.getAsByteArray();
            }
        }
        return new PacketImpl(
            nbt.getString("source"),
            nbt.contains("dest") ? nbt.getString("dest") : null,
            nbt.getInt("port"),
            data,
            nbt.getInt("ttl"));
    }

    private String nextAddress(final WiredNetwork network) {
        String address;
        do {
            address = "node-" + nextNodeId++;
        } while (network.nodesByAddress.containsKey(address));
        return address;
    }

    private static Iterable<Node> adjacentNodes(final BlockGetter world, final BlockPos pos) {
        final List<Node> nodes = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            final Node node = adjacentNode(world, pos, direction);
            if (node != null) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    private static Node adjacentNode(final BlockGetter world, final BlockPos pos, final Direction direction) {
        if (world.getBlockEntity(pos.relative(direction)) instanceof SidedEnvironment sidedEnvironment && sidedEnvironment.canConnect(direction.getOpposite())) {
            return sidedEnvironment.sidedNode(direction.getOpposite());
        }
        if (world.getBlockEntity(pos.relative(direction)) instanceof Environment environment) {
            return environment.node();
        }
        return null;
    }

    private static double wirelessDistance(final WirelessEndpoint source, final WirelessEndpoint target) {
        final double dx = source.x() - target.x();
        final double dy = source.y() - target.y();
        final double dz = source.z() - target.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private final class NodeBuilder implements Builder.NodeBuilder {
        private final Environment host;
        private final Visibility reachability;

        private NodeBuilder(final Environment host, final Visibility reachability) {
            this.host = host;
            this.reachability = reachability;
        }

        @Override
        public Builder.ComponentBuilder withComponent(final String name, final Visibility visibility) {
            return NetworkRegistry.this.new ComponentBuilder(host, reachability, name, visibility);
        }

        @Override
        public Builder.ComponentBuilder withComponent(final String name) {
            return withComponent(name, reachability);
        }

        @Override
        public Builder.ConnectorBuilder withConnector(final double bufferSize) {
            return NetworkRegistry.this.new ConnectorBuilder(host, reachability, bufferSize);
        }

        @Override
        public Builder.ConnectorBuilder withConnector() {
            return withConnector(0);
        }

        @Override
        public Node create() {
            return new BaseNode(host, reachability);
        }
    }

    private final class ComponentBuilder implements Builder.ComponentBuilder {
        private final Environment host;
        private final Visibility reachability;
        private final String name;
        private final Visibility visibility;

        private ComponentBuilder(final Environment host, final Visibility reachability, final String name, final Visibility visibility) {
            this.host = host;
            this.reachability = reachability;
            this.name = name;
            this.visibility = visibility;
        }

        @Override
        public Builder.ComponentConnectorBuilder withConnector(final double bufferSize) {
            return NetworkRegistry.this.new ComponentConnectorBuilder(host, reachability, name, visibility, bufferSize);
        }

        @Override
        public Builder.ComponentConnectorBuilder withConnector() {
            return withConnector(0);
        }

        @Override
        public Component create() {
            return new ComponentNode(host, reachability, name, visibility);
        }
    }

    private final class ConnectorBuilder implements Builder.ConnectorBuilder {
        private final Environment host;
        private final Visibility reachability;
        private final double bufferSize;

        private ConnectorBuilder(final Environment host, final Visibility reachability, final double bufferSize) {
            this.host = host;
            this.reachability = reachability;
            this.bufferSize = bufferSize;
        }

        @Override
        public Builder.ComponentConnectorBuilder withComponent(final String name, final Visibility visibility) {
            return NetworkRegistry.this.new ComponentConnectorBuilder(host, reachability, name, visibility, bufferSize);
        }

        @Override
        public Builder.ComponentConnectorBuilder withComponent(final String name) {
            return withComponent(name, reachability);
        }

        @Override
        public Connector create() {
            return new ConnectorNode(host, reachability, bufferSize);
        }
    }

    private final class ComponentConnectorBuilder implements Builder.ComponentConnectorBuilder {
        private final Environment host;
        private final Visibility reachability;
        private final String name;
        private final Visibility visibility;
        private final double bufferSize;

        private ComponentConnectorBuilder(final Environment host, final Visibility reachability, final String name, final Visibility visibility, final double bufferSize) {
            this.host = host;
            this.reachability = reachability;
            this.name = name;
            this.visibility = visibility;
            this.bufferSize = bufferSize;
        }

        @Override
        public ComponentConnector create() {
            return new ComponentConnectorNode(host, reachability, name, visibility, bufferSize);
        }
    }

    private class BaseNode implements Node {
        private final Environment host;
        private final Visibility reachability;
        private String address;
        private WiredNetwork network;

        private BaseNode(final Environment host, final Visibility reachability) {
            this.host = host;
            this.reachability = reachability;
        }

        @Override
        public Environment host() {
            return host;
        }

        @Override
        public Visibility reachability() {
            return reachability;
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
            return network != null && network.neighborSet(this).contains(other);
        }

        @Override
        public boolean canBeReachedFrom(final Node other) {
            return switch (reachability) {
                case None -> false;
                case Neighbors -> isNeighborOf(other);
                case Network -> network != null && other != null && network == other.network();
            };
        }

        @Override
        public Iterable<Node> neighbors() {
            return network == null ? List.of() : network.neighbors(this);
        }

        @Override
        public Iterable<Node> reachableNodes() {
            return network == null ? List.of() : network.nodes(this);
        }

        @Override
        public void connect(final Node node) {
            if (network != null) {
                network.connect(this, node);
            } else if (node != null && node.network() instanceof WiredNetwork otherNetwork) {
                otherNetwork.connect(node, this);
            }
        }

        @Override
        public void disconnect(final Node node) {
            if (network != null && node != null && network == node.network()) {
                network.disconnect(this, node);
            }
        }

        @Override
        public void remove() {
            if (network != null) {
                network.remove(this);
            }
        }

        @Override
        public void sendToAddress(final String target, final String name, final Object... data) {
            if (network != null) {
                network.sendToAddress(this, target, name, data);
            }
        }

        @Override
        public void sendToNeighbors(final String name, final Object... data) {
            if (network != null) {
                network.sendToNeighbors(this, name, data);
            }
        }

        @Override
        public void sendToReachable(final String name, final Object... data) {
            if (network != null) {
                network.sendToReachable(this, name, data);
            }
        }

        @Override
        public void sendToVisible(final String name, final Object... data) {
            if (network != null) {
                network.sendToVisible(this, name, data);
            }
        }

        @Override
        public void load(final CompoundTag nbt) {
            if (nbt.contains("address")) {
                address = nbt.getString("address");
            }
        }

        @Override
        public void save(final CompoundTag nbt) {
            if (address != null) {
                nbt.putString("address", address);
            }
        }
    }

    private class ComponentNode extends BaseNode implements Component {
        private final String name;
        private final Map<String, ComponentCallbackEntry> callbacks;
        private Visibility visibility;

        private ComponentNode(final Environment host, final Visibility reachability, final String name, final Visibility visibility) {
            super(host, reachability);
            this.name = name;
            this.callbacks = discoverCallbacks(host);
            setVisibility(visibility);
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Visibility visibility() {
            return visibility;
        }

        @Override
        public void setVisibility(final Visibility value) {
            if (value.ordinal() > reachability().ordinal()) {
                throw new IllegalArgumentException("component visibility cannot exceed node reachability");
            }
            visibility = value;
        }

        @Override
        public boolean canBeSeenFrom(final Node other) {
            return switch (visibility) {
                case None -> false;
                case Neighbors -> isNeighborOf(other);
                case Network -> canBeReachedFrom(other);
            };
        }

        @Override
        public Collection<String> methods() {
            return Collections.unmodifiableSet(callbacks.keySet());
        }

        @Override
        public Callback annotation(final String method) {
            final ComponentCallbackEntry callback = callbacks.get(method);
            if (callback == null) {
                throw new NoSuchElementException(method);
            }
            return callback.annotation();
        }

        @Override
        public Object[] invoke(final String method, final Context context, final Object... arguments) throws Exception {
            final ComponentCallbackEntry callback = callbacks.get(method);
            if (callback == null) {
                throw new NoSuchMethodException(method);
            }
            return callback.invoke(host(), context, new RuntimeArguments(arguments));
        }

        private Map<String, ComponentCallbackEntry> discoverCallbacks(final Environment host) {
            if (host instanceof CompoundBlockEnvironment compound) {
                return discoverCompoundCallbacks(compound);
            }
            return discoverCallbacks(host, localWhitelist(host));
        }

        private Map<String, ComponentCallbackEntry> discoverCompoundCallbacks(final CompoundBlockEnvironment compound) {
            final Map<String, ComponentCallbackEntry> discovered = new LinkedHashMap<>();
            final Set<String> whitelist = compoundWhitelist(compound.environments());
            final List<CompoundBlockEnvironment.Entry> environments = new ArrayList<>(compound.environments());
            environments.sort((a, b) -> Integer.compare(priority(b.environment()), priority(a.environment())));
            for (CompoundBlockEnvironment.Entry entry : environments) {
                final Map<String, ComponentCallbackEntry> callbacks = discoverCallbacks(entry.environment(), whitelist);
                for (Map.Entry<String, ComponentCallbackEntry> callback : callbacks.entrySet()) {
                    discovered.putIfAbsent(callback.getKey(), new TargetedComponentCallbackEntry(entry.environment(), callback.getValue()));
                }
            }
            return discovered;
        }

        private Map<String, ComponentCallbackEntry> discoverCallbacks(final Environment host, final Set<String> whitelist) {
            final Map<String, ComponentCallbackEntry> discovered = new LinkedHashMap<>();
            final Set<String> localWhitelist = whitelist == null ? localWhitelist(host) : whitelist;
            final FilteredEnvironment filter = host instanceof FilteredEnvironment filtered ? filtered : null;
            if (host instanceof ManagedPeripheral peripheral) {
                final String[] methods = peripheral.methods();
                if (methods != null) {
                    for (String name : methods) {
                        if (name != null && (localWhitelist.isEmpty() || localWhitelist.contains(name)) && (filter == null || filter.isCallbackEnabled(name))) {
                            discovered.putIfAbsent(name, new ManagedPeripheralCallbackEntry(name));
                        }
                    }
                }
            }
            Class<?> type = host.getClass();
            while (type != null) {
                for (Method method : type.getDeclaredMethods()) {
                    final Callback callback = method.getAnnotation(Callback.class);
                    if (callback != null && isValidCallbackMethod(method)) {
                        method.setAccessible(true);
                        final String name = callback.value().trim().isEmpty() ? method.getName() : callback.value();
                        if ((localWhitelist.isEmpty() || localWhitelist.contains(name)) && (filter == null || filter.isCallbackEnabled(name))) {
                            discovered.putIfAbsent(name, new ReflectedComponentCallbackEntry(method));
                        }
                    }
                }
                type = type.getSuperclass();
            }
            return discovered;
        }

        private Set<String> localWhitelist(final Environment host) {
            final Set<String> whitelist = host instanceof MethodWhitelist methodWhitelist && methodWhitelist.whitelistedMethods() != null
                ? Set.copyOf(Arrays.asList(methodWhitelist.whitelistedMethods()))
                : Set.of();
            return whitelist;
        }

        private Set<String> compoundWhitelist(final List<CompoundBlockEnvironment.Entry> environments) {
            Set<String> whitelist = null;
            for (CompoundBlockEnvironment.Entry entry : environments) {
                final Set<String> local = localWhitelist(entry.environment());
                if (!local.isEmpty()) {
                    whitelist = whitelist == null ? new LinkedHashSet<>(local) : intersect(whitelist, local);
                }
            }
            return whitelist == null ? Set.of() : whitelist;
        }

        private Set<String> intersect(final Set<String> left, final Set<String> right) {
            final Set<String> result = new LinkedHashSet<>(left);
            result.retainAll(right);
            return result;
        }

        private int priority(final Environment environment) {
            return environment instanceof NamedBlock named ? named.priority() : 0;
        }

        private boolean isValidCallbackMethod(final Method method) {
            final Class<?>[] parameterTypes = method.getParameterTypes();
            return method.getReturnType() == Object[].class &&
                parameterTypes.length == 2 &&
                parameterTypes[0] == Context.class &&
                parameterTypes[1] == li.cil.oc.api.machine.Arguments.class &&
                Modifier.isPublic(method.getModifiers());
        }
    }

    private interface ComponentCallbackEntry {
        Callback annotation();

        Object[] invoke(Environment host, Context context, RuntimeArguments arguments) throws Exception;
    }

    private record ReflectedComponentCallbackEntry(Method method) implements ComponentCallbackEntry {
        @Override
        public Callback annotation() {
            return method.getAnnotation(Callback.class);
        }

        @Override
        public Object[] invoke(final Environment host, final Context context, final RuntimeArguments arguments) throws Exception {
            try {
                final Object result = method.invoke(host, context, arguments);
                if (result == null) {
                    return null;
                }
                if (result instanceof Object[] values) {
                    return values;
                }
                return new Object[]{result};
            } catch (InvocationTargetException e) {
                final Throwable cause = e.getCause();
                if (cause instanceof Exception exception) {
                    throw exception;
                }
                if (cause instanceof Error error) {
                    throw error;
                }
                throw new RuntimeException(cause);
            }
        }
    }

    private record TargetedComponentCallbackEntry(Environment target, ComponentCallbackEntry delegate) implements ComponentCallbackEntry {
        @Override
        public Callback annotation() {
            return delegate.annotation();
        }

        @Override
        public Object[] invoke(final Environment host, final Context context, final RuntimeArguments arguments) throws Exception {
            return delegate.invoke(target, context, arguments);
        }
    }

    private record ManagedPeripheralCallbackEntry(String name) implements ComponentCallbackEntry {
        @Override
        public Callback annotation() {
            return new PeripheralCallbackAnnotation(name);
        }

        @Override
        public Object[] invoke(final Environment host, final Context context, final RuntimeArguments arguments) throws Exception {
            if (!(host instanceof ManagedPeripheral peripheral)) {
                throw new NoSuchMethodException(name);
            }
            return peripheral.invoke(name, context, arguments);
        }
    }

    private record PeripheralCallbackAnnotation(String value) implements Callback {
        @Override
        public boolean direct() {
            return true;
        }

        @Override
        public int limit() {
            return 100;
        }

        @Override
        public String doc() {
            return "";
        }

        @Override
        public boolean getter() {
            return false;
        }

        @Override
        public boolean setter() {
            return false;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Callback.class;
        }
    }

    private interface ConnectorBufferAccess extends Connector {
        double rawLocalBuffer();

        double rawLocalBufferSize();

        double rawChangeBuffer(double delta);
    }

    private List<ConnectorBufferAccess> connectorBuffers(final BaseNode reference, final ConnectorBufferAccess fallback) {
        if (reference.network == null) {
            return List.of(fallback);
        }
        final List<ConnectorBufferAccess> connectors = new ArrayList<>();
        for (final Node node : reference.network.nodes()) {
            if (node instanceof ConnectorBufferAccess connector) {
                connectors.add(connector);
            }
        }
        return connectors.isEmpty() ? List.of(fallback) : connectors;
    }

    private double changeConnectorBuffers(final ConnectorBufferAccess preferred, final List<ConnectorBufferAccess> connectors, final double delta) {
        double remaining = preferred.rawChangeBuffer(delta);
        if (Math.abs(remaining) <= ConnectorNode.BUFFER_EPSILON) {
            return 0;
        }
        for (final ConnectorBufferAccess connector : connectors) {
            if (connector != preferred) {
                remaining = connector.rawChangeBuffer(remaining);
                if (Math.abs(remaining) <= ConnectorNode.BUFFER_EPSILON) {
                    return 0;
                }
            }
        }
        return remaining;
    }

    private class ConnectorNode extends BaseNode implements ConnectorBufferAccess {
        private static final double BUFFER_EPSILON = 1e-7;

        private double localBufferSize;
        private double localBuffer;

        private ConnectorNode(final Environment host, final Visibility reachability, final double localBufferSize) {
            super(host, reachability);
            this.localBufferSize = Math.max(0, localBufferSize);
        }

        @Override
        public double localBuffer() {
            return localBuffer;
        }

        @Override
        public double localBufferSize() {
            return localBufferSize;
        }

        @Override
        public double globalBuffer() {
            return connectorBuffers(this, this).stream().mapToDouble(ConnectorBufferAccess::rawLocalBuffer).sum();
        }

        @Override
        public double globalBufferSize() {
            return connectorBuffers(this, this).stream().mapToDouble(ConnectorBufferAccess::rawLocalBufferSize).sum();
        }

        @Override
        public double changeBuffer(final double delta) {
            if (ModSettings.ignorePower()) {
                return delta < 0 ? 0 : delta;
            }
            return changeConnectorBuffers(this, connectorBuffers(this, this), delta);
        }

        @Override
        public boolean tryChangeBuffer(final double delta) {
            if (Math.abs(delta) <= BUFFER_EPSILON) {
                return true;
            }
            if (ModSettings.ignorePower()) {
                return delta < 0;
            }
            final double newBuffer = globalBuffer() + delta;
            if (newBuffer < -BUFFER_EPSILON || newBuffer - globalBufferSize() > BUFFER_EPSILON) {
                return false;
            }
            return Math.abs(changeBuffer(delta)) <= BUFFER_EPSILON;
        }

        @Override
        public void setLocalBufferSize(final double size) {
            localBufferSize = Math.max(0, size);
            localBuffer = Math.min(localBuffer, localBufferSize);
        }

        @Override
        public void load(final CompoundTag nbt) {
            super.load(nbt);
            localBuffer = Math.min(nbt.getDouble("buffer"), localBufferSize);
        }

        @Override
        public void save(final CompoundTag nbt) {
            super.save(nbt);
            nbt.putDouble("buffer", Math.min(localBuffer, localBufferSize));
        }

        @Override
        public double rawLocalBuffer() {
            return localBuffer;
        }

        @Override
        public double rawLocalBufferSize() {
            return localBufferSize;
        }

        @Override
        public double rawChangeBuffer(final double delta) {
            final double oldBuffer = localBuffer;
            localBuffer = Math.max(0, Math.min(localBuffer + delta, localBufferSize));
            return delta - (localBuffer - oldBuffer);
        }

    }

    private final class ComponentConnectorNode extends ComponentNode implements ComponentConnector, ConnectorBufferAccess {
        private final ConnectorNode connectorDelegate;

        private ComponentConnectorNode(final Environment host, final Visibility reachability, final String name, final Visibility visibility, final double localBufferSize) {
            super(host, reachability, name, visibility);
            connectorDelegate = new ConnectorNode(host, reachability, localBufferSize);
        }

        @Override
        public double localBuffer() {
            return connectorDelegate.localBuffer();
        }

        @Override
        public double localBufferSize() {
            return connectorDelegate.localBufferSize();
        }

        @Override
        public double globalBuffer() {
            return connectorBuffers(this, this).stream().mapToDouble(ConnectorBufferAccess::rawLocalBuffer).sum();
        }

        @Override
        public double globalBufferSize() {
            return connectorBuffers(this, this).stream().mapToDouble(ConnectorBufferAccess::rawLocalBufferSize).sum();
        }

        @Override
        public double changeBuffer(final double delta) {
            if (ModSettings.ignorePower()) {
                return delta < 0 ? 0 : delta;
            }
            return changeConnectorBuffers(this, connectorBuffers(this, this), delta);
        }

        @Override
        public boolean tryChangeBuffer(final double delta) {
            if (Math.abs(delta) <= ConnectorNode.BUFFER_EPSILON) {
                return true;
            }
            if (ModSettings.ignorePower()) {
                return delta < 0;
            }
            final double newBuffer = globalBuffer() + delta;
            if (newBuffer < -ConnectorNode.BUFFER_EPSILON || newBuffer - globalBufferSize() > ConnectorNode.BUFFER_EPSILON) {
                return false;
            }
            return Math.abs(changeBuffer(delta)) <= ConnectorNode.BUFFER_EPSILON;
        }

        @Override
        public void setLocalBufferSize(final double size) {
            connectorDelegate.setLocalBufferSize(size);
        }

        @Override
        public void load(final CompoundTag nbt) {
            super.load(nbt);
            connectorDelegate.load(nbt);
        }

        @Override
        public void save(final CompoundTag nbt) {
            super.save(nbt);
            connectorDelegate.save(nbt);
        }

        @Override
        public double rawLocalBuffer() {
            return connectorDelegate.rawLocalBuffer();
        }

        @Override
        public double rawLocalBufferSize() {
            return connectorDelegate.rawLocalBufferSize();
        }

        @Override
        public double rawChangeBuffer(final double delta) {
            return connectorDelegate.rawChangeBuffer(delta);
        }
    }

    private final class WiredNetwork implements Network {
        private final Map<String, BaseNode> nodesByAddress = new LinkedHashMap<>();
        private final Map<BaseNode, Set<BaseNode>> edges = new LinkedHashMap<>();

        private WiredNetwork(final BaseNode firstNode) {
            add(firstNode);
            notifyConnect(firstNode, firstNode);
        }

        private WiredNetwork() {
        }

        @Override
        public boolean connect(final Node nodeA, final Node nodeB) {
            if (!(nodeA instanceof BaseNode baseA) || !(nodeB instanceof BaseNode baseB) || nodeA == nodeB) {
                return false;
            }
            if (baseA.network != this && baseB.network == this) {
                return connect(baseB, baseA);
            }
            if (baseA.network != this) {
                return false;
            }
            final Set<BaseNode> previousNodes = connectedSet(baseA);
            final Set<BaseNode> mergedNodes = baseB.network instanceof WiredNetwork otherNetwork && otherNetwork != this
                ? new LinkedHashSet<>(otherNetwork.nodesByAddress.values())
                : Set.of();
            if (baseB.network != null && baseB.network != this) {
                merge(baseB.network);
            }
            final boolean addedNode = baseB.network == null;
            if (baseB.network == null) {
                add(baseB);
            }
            final boolean changedA = edges.get(baseA).add(baseB);
            final boolean changedB = edges.get(baseB).add(baseA);
            if (changedA || changedB) {
                if (addedNode) {
                    notifyConnectReachable(baseB);
                    notifyConnect(baseB, baseA);
                } else if (!mergedNodes.isEmpty()) {
                    notifyConnectAcross(previousNodes, mergedNodes);
                    notifyConnectAcross(mergedNodes, previousNodes);
                } else {
                    notifyConnect(baseA, baseB);
                    notifyConnect(baseB, baseA);
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean disconnect(final Node nodeA, final Node nodeB) {
            if (!(nodeA instanceof BaseNode baseA) || !(nodeB instanceof BaseNode baseB) || baseA.network != this || baseB.network != this) {
                return false;
            }
            final boolean changedA = edges.get(baseA).remove(baseB);
            final boolean changedB = edges.get(baseB).remove(baseA);
            if (changedA || changedB) {
                final Set<BaseNode> sideA = connectedSet(baseA);
                if (sideA.contains(baseB)) {
                    notifyDisconnect(baseA, baseB);
                    notifyDisconnect(baseB, baseA);
                } else {
                    final Set<BaseNode> sideB = connectedSet(baseB);
                    notifyDisconnectAcross(sideA, sideB);
                    notifyDisconnectAcross(sideB, sideA);
                    moveToNewNetwork(sideB);
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean remove(final Node node) {
            if (!(node instanceof BaseNode baseNode) || baseNode.network != this) {
                return false;
            }
            final Set<BaseNode> neighbors = new LinkedHashSet<>(edges.get(baseNode));
            for (BaseNode neighbor : neighbors) {
                disconnect(baseNode, neighbor);
            }
            edges.remove(baseNode);
            nodesByAddress.remove(baseNode.address);
            baseNode.network = null;
            notifyDisconnect(baseNode, baseNode);
            return true;
        }

        @Override
        public Node node(final String address) {
            return nodesByAddress.get(address);
        }

        @Override
        public Iterable<Node> nodes() {
            return List.copyOf(nodesByAddress.values());
        }

        @Override
        public Iterable<Node> nodes(final Node reference) {
            if (!(reference instanceof BaseNode baseReference) || baseReference.network != this) {
                return List.of();
            }
            final Set<BaseNode> referenceNeighbors = neighborSet(baseReference);
            return connectedSet(baseReference).stream()
                .filter(node -> node != reference)
                .filter(node -> node.reachability() == Visibility.Network ||
                    node.reachability() == Visibility.Neighbors && referenceNeighbors.contains(node))
                .map(Node.class::cast)
                .toList();
        }

        @Override
        public Iterable<Node> neighbors(final Node node) {
            if (!(node instanceof BaseNode baseNode) || baseNode.network != this) {
                return List.of();
            }
            return List.copyOf(neighborSet(baseNode));
        }

        @Override
        public void sendToAddress(final Node source, final String target, final String name, final Object... data) {
            final Node targetNode = nodesByAddress.get(target);
            if (targetNode != null && targetNode.canBeReachedFrom(source)) {
                deliver(source, List.of(targetNode), name, data);
            }
        }

        @Override
        public void sendToNeighbors(final Node source, final String name, final Object... data) {
            deliver(source, neighbors(source), name, data);
        }

        @Override
        public void sendToReachable(final Node source, final String name, final Object... data) {
            deliver(source, nodes(source), name, data);
        }

        @Override
        public void sendToVisible(final Node source, final String name, final Object... data) {
            final List<Node> targets = new ArrayList<>();
            for (Node node : nodes(source)) {
                if (node instanceof Component component && component.canBeSeenFrom(source)) {
                    targets.add(node);
                }
            }
            deliver(source, targets, name, data);
        }

        private void add(final BaseNode node) {
            if (node.address == null || nodesByAddress.containsKey(node.address)) {
                node.address = nextAddress(this);
            }
            node.network = this;
            nodesByAddress.put(node.address, node);
            edges.put(node, new LinkedHashSet<>());
        }

        private void merge(final WiredNetwork other) {
            final List<BaseNode> otherNodes = new ArrayList<>(other.nodesByAddress.values());
            final Map<BaseNode, Set<BaseNode>> otherEdges = new LinkedHashMap<>(other.edges);
            other.nodesByAddress.clear();
            other.edges.clear();
            for (BaseNode node : otherNodes) {
                node.network = null;
                add(node);
            }
            for (Map.Entry<BaseNode, Set<BaseNode>> edgeSet : otherEdges.entrySet()) {
                for (BaseNode neighbor : edgeSet.getValue()) {
                    edges.get(edgeSet.getKey()).add(neighbor);
                }
            }
        }

        private Set<BaseNode> neighborSet(final BaseNode node) {
            return edges.getOrDefault(node, Set.of());
        }

        private Set<BaseNode> connectedSet(final BaseNode start) {
            final Set<BaseNode> visited = new LinkedHashSet<>();
            final ArrayDeque<BaseNode> open = new ArrayDeque<>();
            if (start.network == this) {
                visited.add(start);
                open.add(start);
            }
            while (!open.isEmpty()) {
                final BaseNode node = open.removeFirst();
                for (final BaseNode neighbor : neighborSet(node)) {
                    if (neighbor.network == this && visited.add(neighbor)) {
                        open.add(neighbor);
                    }
                }
            }
            return visited;
        }

        private void moveToNewNetwork(final Set<BaseNode> movedNodes) {
            final Map<BaseNode, Set<BaseNode>> movedEdges = new LinkedHashMap<>();
            for (final BaseNode node : movedNodes) {
                movedEdges.put(node, new LinkedHashSet<>(neighborSet(node)));
            }
            for (final BaseNode node : movedNodes) {
                nodesByAddress.remove(node.address);
                edges.remove(node);
                node.network = null;
            }
            for (final Set<BaseNode> neighbors : edges.values()) {
                neighbors.removeAll(movedNodes);
            }

            final WiredNetwork movedNetwork = new WiredNetwork();
            for (final BaseNode node : movedNodes) {
                movedNetwork.add(node);
            }
            for (final Map.Entry<BaseNode, Set<BaseNode>> entry : movedEdges.entrySet()) {
                final Set<BaseNode> targetEdges = movedNetwork.edges.get(entry.getKey());
                for (final BaseNode neighbor : entry.getValue()) {
                    if (movedNodes.contains(neighbor)) {
                        targetEdges.add(neighbor);
                    }
                }
            }
        }

        private void deliver(final Node source, final Iterable<Node> targets, final String name, final Object[] data) {
            final MessageImpl message = new MessageImpl(source, name, data);
            for (Node target : targets) {
                if (!message.canceled) {
                    target.host().onMessage(message);
                }
            }
        }

        private void notifyConnect(final BaseNode host, final Node connected) {
            host.host().onConnect(connected);
        }

        private void notifyDisconnect(final BaseNode host, final Node disconnected) {
            host.host().onDisconnect(disconnected);
        }

        private void notifyConnectReachable(final BaseNode connected) {
            for (final BaseNode target : connectedSet(connected)) {
                notifyConnect(target, connected);
            }
        }

        private void notifyConnectAcross(final Iterable<BaseNode> targets, final Iterable<BaseNode> connectedNodes) {
            for (final BaseNode target : targets) {
                for (final BaseNode connected : connectedNodes) {
                    if (target != connected) {
                        notifyConnect(target, connected);
                    }
                }
            }
        }

        private void notifyDisconnectAcross(final Iterable<BaseNode> targets, final Iterable<BaseNode> disconnectedNodes) {
            for (final BaseNode target : targets) {
                for (final BaseNode disconnected : disconnectedNodes) {
                    if (target != disconnected) {
                        notifyDisconnect(target, disconnected);
                    }
                }
            }
        }
    }

    private static final class MessageImpl implements Message {
        private final Node source;
        private final String name;
        private final Object[] data;
        private boolean canceled;

        private MessageImpl(final Node source, final String name, final Object[] data) {
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
            canceled = true;
        }
    }

    private static final class PacketImpl implements Packet {
        private final String source;
        private final String destination;
        private final int port;
        private final Object[] data;
        private final int ttl;
        private final int size;

        private PacketImpl(final String source, final String destination, final int port, final Object[] data, final int ttl) {
            this.source = source;
            this.destination = destination;
            this.port = port;
            this.data = data == null ? new Object[0] : Arrays.copyOf(data, data.length);
            this.ttl = ttl;
            this.size = computeSize(this.data);
        }

        @Override
        public String source() {
            return source;
        }

        @Override
        public String destination() {
            return destination;
        }

        @Override
        public int port() {
            return port;
        }

        @Override
        public Object[] data() {
            return Arrays.copyOf(data, data.length);
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public int ttl() {
            return ttl;
        }

        @Override
        public Packet hop() {
            return new PacketImpl(source, destination, port, data, ttl - 1);
        }

        @Override
        public void save(final CompoundTag nbt) {
            nbt.putString("source", source);
            if (destination != null && !destination.isEmpty()) {
                nbt.putString("dest", destination);
            }
            nbt.putInt("port", port);
            nbt.putInt("ttl", ttl);
            nbt.putInt("dataLength", data.length);
            for (int index = 0; index < data.length; index++) {
                final Object value = data[index];
                if (value instanceof Boolean typedValue) {
                    nbt.putBoolean("data" + index, typedValue);
                } else if (value instanceof Byte typedValue) {
                    nbt.putShort("data" + index, typedValue);
                } else if (value instanceof Short typedValue) {
                    nbt.putShort("data" + index, typedValue);
                } else if (value instanceof Integer typedValue) {
                    nbt.putInt("data" + index, typedValue);
                } else if (value instanceof Long typedValue) {
                    nbt.putLong("data" + index, typedValue);
                } else if (value instanceof Float typedValue) {
                    nbt.putFloat("data" + index, typedValue);
                } else if (value instanceof Double typedValue) {
                    nbt.putDouble("data" + index, typedValue);
                } else if (value instanceof String typedValue) {
                    nbt.putString("data" + index, typedValue);
                } else if (value instanceof byte[] typedValue) {
                    nbt.putByteArray("data" + index, typedValue);
                }
            }
        }

        private static int computeSize(final Object[] data) {
            if (data.length > ModSettings.maxNetworkPacketParts()) {
                throw new IllegalArgumentException("packet has too many parts");
            }
            int computedSize = data.length * 2;
            for (Object value : data) {
                computedSize += switch (value) {
                    case null -> 1;
                    case Boolean ignored -> 1;
                    case Byte ignored -> 2;
                    case Short ignored -> 2;
                    case Integer ignored -> 4;
                    case Long ignored -> 8;
                    case Float ignored -> 4;
                    case Double ignored -> 8;
                    case String string -> Math.max(string.length(), 1);
                    case byte[] bytes -> Math.max(bytes.length, 1);
                    default -> throw new IllegalArgumentException("unsupported data type: " + value.getClass().getCanonicalName());
                };
            }
            return computedSize;
        }
    }

    private record RuntimeArguments(Object[] values) implements li.cil.oc.api.machine.Arguments {
        @Override
        public int count() {
            return values.length;
        }

        @Override
        public Object checkAny(final int index) {
            if (index < 0 || index >= values.length) {
                throw new IllegalArgumentException("missing argument #" + (index + 1));
            }
            return values[index];
        }

        @Override
        public boolean checkBoolean(final int index) {
            final Object value = checkAny(index);
            if (value instanceof Boolean typedValue) {
                return typedValue;
            }
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (boolean expected)");
        }

        @Override
        public int checkInteger(final int index) {
            final Object value = checkAny(index);
            if (value instanceof Double number) {
                return checkInteger(index, number);
            }
            if (value instanceof Float number) {
                return checkInteger(index, number.doubleValue());
            }
            if (value instanceof Long number) {
                if (number > Integer.MAX_VALUE) {
                    return Integer.MAX_VALUE;
                }
                if (number < Integer.MIN_VALUE) {
                    return Integer.MIN_VALUE;
                }
                return number.intValue();
            }
            if (value instanceof Number number) {
                return number.intValue();
            }
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (integer expected)");
        }

        @Override
        public long checkLong(final int index) {
            final Object value = checkAny(index);
            if (value instanceof Double number) {
                return checkLong(index, number);
            }
            if (value instanceof Float number) {
                return checkLong(index, number.doubleValue());
            }
            if (value instanceof Number number) {
                return number.longValue();
            }
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (integer expected)");
        }

        @Override
        public double checkDouble(final int index) {
            final Object value = checkAny(index);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (number expected)");
        }

        @Override
        public String checkString(final int index) {
            final Object value = checkAny(index);
            if (value instanceof String typedValue) {
                return typedValue;
            }
            if (value instanceof byte[] typedValue) {
                return new String(typedValue, StandardCharsets.UTF_8);
            }
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (string expected)");
        }

        @Override
        public byte[] checkByteArray(final int index) {
            final Object value = checkAny(index);
            if (value instanceof byte[] typedValue) {
                return typedValue;
            }
            if (value instanceof String typedValue) {
                return typedValue.getBytes(StandardCharsets.UTF_8);
            }
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (byte array expected)");
        }

        @Override
        public Map checkTable(final int index) {
            final Object value = checkAny(index);
            if (value instanceof Map typedValue) {
                return typedValue;
            }
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (table expected)");
        }

        @Override
        public ItemStack checkItemStack(final int index) {
            final Object value = checkAny(index);
            if (value instanceof ItemStack typedValue) {
                return typedValue;
            }
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (item stack expected)");
        }

        @Override
        public Object optAny(final int index, final Object def) {
            return isDefined(index) ? values[index] : def;
        }

        @Override
        public boolean optBoolean(final int index, final boolean def) {
            return isDefined(index) ? checkBoolean(index) : def;
        }

        @Override
        public int optInteger(final int index, final int def) {
            return isDefined(index) ? checkInteger(index) : def;
        }

        @Override
        public long optLong(final int index, final long def) {
            return isDefined(index) ? checkLong(index) : def;
        }

        @Override
        public double optDouble(final int index, final double def) {
            return isDefined(index) ? checkDouble(index) : def;
        }

        @Override
        public String optString(final int index, final String def) {
            return isDefined(index) ? checkString(index) : def;
        }

        @Override
        public byte[] optByteArray(final int index, final byte[] def) {
            return isDefined(index) ? checkByteArray(index) : def;
        }

        @Override
        public Map optTable(final int index, final Map def) {
            return isDefined(index) ? checkTable(index) : def;
        }

        @Override
        public ItemStack optItemStack(final int index, final ItemStack def) {
            return isDefined(index) ? checkItemStack(index) : def;
        }

        @Override
        public boolean isBoolean(final int index) {
            return index >= 0 && index < values.length && values[index] instanceof Boolean;
        }

        @Override
        public boolean isInteger(final int index) {
            return index >= 0 && index < values.length && isNonNaNNumber(values[index]);
        }

        @Override
        public boolean isLong(final int index) {
            return index >= 0 && index < values.length && isNonNaNNumber(values[index]);
        }

        @Override
        public boolean isDouble(final int index) {
            return index >= 0 && index < values.length && values[index] instanceof Number;
        }

        @Override
        public boolean isString(final int index) {
            return index >= 0 && index < values.length && (values[index] instanceof String || values[index] instanceof byte[]);
        }

        @Override
        public boolean isByteArray(final int index) {
            return index >= 0 && index < values.length && (values[index] instanceof String || values[index] instanceof byte[]);
        }

        @Override
        public boolean isTable(final int index) {
            return index >= 0 && index < values.length && values[index] instanceof Map;
        }

        @Override
        public boolean isItemStack(final int index) {
            return index >= 0 && index < values.length && values[index] instanceof ItemStack;
        }

        @Override
        public Object[] toArray() {
            final Object[] result = Arrays.copyOf(values, values.length);
            for (int index = 0; index < result.length; index++) {
                if (result[index] instanceof byte[] bytes) {
                    result[index] = new String(bytes, StandardCharsets.UTF_8);
                }
            }
            return result;
        }

        @Override
        public java.util.Iterator<Object> iterator() {
            return Arrays.asList(values).iterator();
        }

        private static int checkInteger(final int index, final double value) {
            if (Double.isNaN(value)) {
                throw new IllegalArgumentException("bad argument #" + (index + 1) + " (number has no integer representation)");
            }
            if (value > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            if (value < Integer.MIN_VALUE) {
                return Integer.MIN_VALUE;
            }
            return (int) value;
        }

        private static long checkLong(final int index, final double value) {
            if (Double.isNaN(value)) {
                throw new IllegalArgumentException("bad argument #" + (index + 1) + " (number has no integer representation)");
            }
            if (value > Long.MAX_VALUE) {
                return Long.MAX_VALUE;
            }
            if (value < Long.MIN_VALUE) {
                return Long.MIN_VALUE;
            }
            return (long) value;
        }

        private static boolean isNonNaNNumber(final Object value) {
            if (value instanceof Double number) {
                return !number.isNaN();
            }
            if (value instanceof Float number) {
                return !number.isNaN();
            }
            return value instanceof Number;
        }

        private boolean isDefined(final int index) {
            return index >= 0 && index < values.length && values[index] != null;
        }
    }
}
