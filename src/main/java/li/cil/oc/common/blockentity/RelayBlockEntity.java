package li.cil.oc.common.blockentity;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Queue;

public class RelayBlockEntity extends BlockEntity implements SidedEnvironment {
    public static final double CONNECTOR_BUFFER_SIZE = 600D;
    public static final int MAX_QUEUE_SIZE = 20;

    private static final String NETWORK_MESSAGE = "network.message";
    private static final String TAG_PLUGS = "oc:plugs";
    private static final String TAG_QUEUE = "oc:queue";
    private static final String TAG_SIDE = "side";
    private static final String TAG_PACKET = "packet";

    private final Plug[] plugs = new Plug[Direction.values().length];
    private final Queue<QueuedPacket> queue = new ArrayDeque<>();

    public RelayBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.RELAY.get(), pos, blockState);
        OpenComputersApi.initialize();
        for (final Direction direction : Direction.values()) {
            plugs[direction.ordinal()] = new Plug(direction);
        }
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final RelayBlockEntity relay) {
        relay.relayQueuedPacket();
    }

    @Override
    public Node sidedNode(final Direction side) {
        return side == null ? null : plugs[side.ordinal()].node();
    }

    @Override
    public boolean canConnect(final Direction side) {
        return side != null;
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        final ListTag plugTags = tag.getList(TAG_PLUGS, CompoundTag.TAG_COMPOUND);
        for (int index = 0; index < Math.min(plugTags.size(), plugs.length); index++) {
            plugs[index].node().load(plugTags.getCompound(index));
        }
        queue.clear();
        final ListTag queueTags = tag.getList(TAG_QUEUE, CompoundTag.TAG_COMPOUND);
        for (int index = 0; index < queueTags.size() && queue.size() < MAX_QUEUE_SIZE; index++) {
            final CompoundTag queued = queueTags.getCompound(index);
            if (queued.contains(TAG_PACKET)) {
                queue.add(new QueuedPacket(
                    Direction.from3DDataValue(queued.getInt(TAG_SIDE)),
                    Network.newPacket(queued.getCompound(TAG_PACKET))));
            }
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        final ListTag plugTags = new ListTag();
        for (final Plug plug : plugs) {
            final CompoundTag plugTag = new CompoundTag();
            if (plug.node().address() == null) {
                Network.joinNewNetwork(plug.node());
            }
            plug.node().save(plugTag);
            plugTags.add(plugTag);
        }
        tag.put(TAG_PLUGS, plugTags);

        final ListTag queueTags = new ListTag();
        for (final QueuedPacket queued : queue) {
            final CompoundTag queuedTag = new CompoundTag();
            queuedTag.putInt(TAG_SIDE, queued.sourceSide().get3DDataValue());
            final CompoundTag packetTag = new CompoundTag();
            queued.packet().save(packetTag);
            queuedTag.put(TAG_PACKET, packetTag);
            queueTags.add(queuedTag);
        }
        tag.put(TAG_QUEUE, queueTags);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        removeNodes();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        removeNodes();
    }

    public void removeNodes() {
        for (final Plug plug : plugs) {
            plug.node().remove();
        }
    }

    private void enqueue(final Direction sourceSide, final Packet packet) {
        if (packet.ttl() <= 0 || queue.size() >= MAX_QUEUE_SIZE) {
            return;
        }
        queue.add(new QueuedPacket(sourceSide, packet.hop()));
        setChanged();
    }

    private void relayQueuedPacket() {
        final QueuedPacket queued = queue.poll();
        if (queued == null) {
            return;
        }
        for (final Direction direction : Direction.values()) {
            if (direction != queued.sourceSide()) {
                plugs[direction.ordinal()].node().sendToReachable(NETWORK_MESSAGE, queued.packet());
            }
        }
        setChanged();
    }

    private boolean isRelayPlug(final Node node) {
        for (final Plug plug : plugs) {
            if (plug.node() == node) {
                return true;
            }
        }
        return false;
    }

    private boolean isPrimary(final Plug plug) {
        for (final Plug candidate : plugs) {
            if (candidate.node().network() == plug.node().network()) {
                return candidate == plug;
            }
        }
        return false;
    }

    private static Node createNode(final Environment environment) {
        return Network.newNode(environment, Visibility.Network)
            .withConnector(CONNECTOR_BUFFER_SIZE)
            .create();
    }

    private record QueuedPacket(Direction sourceSide, Packet packet) {
    }

    private final class Plug implements Environment {
        private final Direction side;
        private final Node node;

        private Plug(final Direction side) {
            this.side = side;
            node = createNode(this);
        }

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
            if (!isPrimary(this) || !NETWORK_MESSAGE.equals(message.name()) || isRelayPlug(message.source())) {
                return;
            }
            if (message.data().length == 1 && message.data()[0] instanceof Packet packet) {
                enqueue(side, packet);
            }
        }
    }
}
