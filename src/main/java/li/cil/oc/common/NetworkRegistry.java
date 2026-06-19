package li.cil.oc.common;

import li.cil.oc.api.detail.Builder;
import li.cil.oc.api.detail.NetworkAPI;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Network;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.network.WirelessEndpoint;
import net.minecraft.core.BlockPos;
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
    private static final int INITIAL_PACKET_TTL = 32;

    private int nextNodeId = 1;

    @Override
    public void joinOrCreateNetwork(final BlockEntity blockEntity) {
    }

    @Override
    public void joinOrCreateNetwork(final BlockGetter world, final BlockPos pos) {
    }

    @Override
    public void joinNewNetwork(final Node node) {
        if (node instanceof BaseNode baseNode && baseNode.network == null) {
            new WiredNetwork(baseNode);
        }
    }

    @Override
    public void joinWirelessNetwork(final WirelessEndpoint endpoint) {
    }

    @Override
    public void updateWirelessNetwork(final WirelessEndpoint endpoint) {
    }

    @Override
    public void leaveWirelessNetwork(final WirelessEndpoint endpoint) {
    }

    @Override
    public void leaveWirelessNetwork(final WirelessEndpoint endpoint, final ResourceKey<Level> dimension) {
    }

    @Override
    public void sendWirelessPacket(final WirelessEndpoint source, final double strength, final Packet packet) {
    }

    @Override
    public Builder.NodeBuilder newNode(final Environment host, final Visibility reachability) {
        return new NodeBuilder(host, reachability);
    }

    @Override
    public Packet newPacket(final String source, final String destination, final int port, final Object[] data) {
        return new PacketImpl(source, destination, port, data, INITIAL_PACKET_TTL);
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
        private Visibility visibility;

        private ComponentNode(final Environment host, final Visibility reachability, final String name, final Visibility visibility) {
            super(host, reachability);
            this.name = name;
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
            return Collections.emptySet();
        }

        @Override
        public Callback annotation(final String method) {
            throw new NoSuchElementException(method);
        }

        @Override
        public Object[] invoke(final String method, final Context context, final Object... arguments) throws Exception {
            throw new NoSuchMethodException(method);
        }
    }

    private class ConnectorNode extends BaseNode implements Connector {
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
            return localBuffer;
        }

        @Override
        public double globalBufferSize() {
            return localBufferSize;
        }

        @Override
        public double changeBuffer(final double delta) {
            final double oldBuffer = localBuffer;
            localBuffer = Math.max(0, Math.min(localBuffer + delta, localBufferSize));
            return delta - (localBuffer - oldBuffer);
        }

        @Override
        public boolean tryChangeBuffer(final double delta) {
            final double newBuffer = localBuffer + delta;
            if (newBuffer < 0 || newBuffer > localBufferSize) {
                return false;
            }
            localBuffer = newBuffer;
            return true;
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
    }

    private final class ComponentConnectorNode extends ComponentNode implements ComponentConnector {
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
            return connectorDelegate.globalBuffer();
        }

        @Override
        public double globalBufferSize() {
            return connectorDelegate.globalBufferSize();
        }

        @Override
        public double changeBuffer(final double delta) {
            return connectorDelegate.changeBuffer(delta);
        }

        @Override
        public boolean tryChangeBuffer(final double delta) {
            return connectorDelegate.tryChangeBuffer(delta);
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
    }

    private final class WiredNetwork implements Network {
        private final Map<String, BaseNode> nodesByAddress = new LinkedHashMap<>();
        private final Map<BaseNode, Set<BaseNode>> edges = new LinkedHashMap<>();

        private WiredNetwork(final BaseNode firstNode) {
            add(firstNode);
            notifyConnect(firstNode, firstNode);
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
            if (baseB.network != null && baseB.network != this) {
                merge(baseB.network);
            }
            if (baseB.network == null) {
                add(baseB);
                notifyConnect(baseB, baseB);
            }
            final boolean changedA = edges.get(baseA).add(baseB);
            final boolean changedB = edges.get(baseB).add(baseA);
            if (changedA || changedB) {
                notifyConnect(baseA, baseB);
                notifyConnect(baseB, baseA);
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
                notifyDisconnect(baseA, baseB);
                notifyDisconnect(baseB, baseA);
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
            return nodesByAddress.values().stream()
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
}
