package li.cil.oc.common.blockentity;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class PowerDistributorBlockEntity extends BlockEntity implements Environment, SidedEnvironment {
    public static final double CONNECTOR_BUFFER_SIZE = 500D;

    private static final String TAG_CONNECTORS = "oc:connectors";
    private static final String TAG_VISUAL_BUFFER_RATIO = "oc:visualBufferRatio";

    private final Node[] nodes = new Node[Direction.values().length];
    private double clientVisualBufferRatio;
    private double visualBufferRatio;

    public PowerDistributorBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.POWER_DISTRIBUTOR.get(), pos, blockState);
        OpenComputersApi.initialize();
        for (final Direction direction : Direction.values()) {
            nodes[direction.ordinal()] = createNode(this);
        }
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final PowerDistributorBlockEntity distributor) {
        distributor.balancePower();
    }

    public double visualBufferRatio() {
        return level != null && level.isClientSide ? clientVisualBufferRatio : visualBufferRatio;
    }

    @Override
    public Node node() {
        return sidedNode(Direction.DOWN);
    }

    @Override
    public Node sidedNode(final Direction side) {
        return side == null ? null : nodes[side.ordinal()];
    }

    @Override
    public boolean canConnect(final Direction side) {
        return side != null;
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

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        final ListTag connectors = tag.getList(TAG_CONNECTORS, CompoundTag.TAG_COMPOUND);
        for (int index = 0; index < Math.min(connectors.size(), nodes.length); index++) {
            nodes[index].load(connectors.getCompound(index));
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        final ListTag connectors = new ListTag();
        for (final Node node : nodes) {
            final CompoundTag connectorTag = new CompoundTag();
            if (node.address() == null) {
                Network.joinNewNetwork(node);
            }
            node.save(connectorTag);
            connectors.add(connectorTag);
        }
        tag.put(TAG_CONNECTORS, connectors);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        tag.putDouble(TAG_VISUAL_BUFFER_RATIO, visualBufferRatio);
        return tag;
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, final HolderLookup.Provider registries) {
        if (tag.contains(TAG_VISUAL_BUFFER_RATIO)) {
            clientVisualBufferRatio = clampRatio(tag.getDouble(TAG_VISUAL_BUFFER_RATIO));
        }
    }

    @Override
    public void onDataPacket(final Connection net, final ClientboundBlockEntityDataPacket packet, final HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
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
        for (final Node node : nodes) {
            if (node != null) {
                node.remove();
            }
        }
    }

    private void balancePower() {
        final List<Connector> connectors = primaryConnectors();
        double totalBuffer = 0D;
        double totalSize = 0D;
        for (final Connector connector : connectors) {
            totalBuffer += connector.globalBuffer();
            totalSize += connector.globalBufferSize();
        }
        if (totalSize <= 0D) {
            setVisualBufferRatio(0D);
            return;
        }
        final double ratio = totalBuffer / totalSize;
        for (final Connector connector : connectors) {
            connector.changeBuffer(connector.globalBufferSize() * ratio - connector.globalBuffer());
        }
        setVisualBufferRatio(ratio);
    }

    private List<Connector> primaryConnectors() {
        final List<Connector> connectors = new ArrayList<>();
        for (final Node node : nodes) {
            if (node instanceof Connector connector && node.network() != null && isPrimary(connector)) {
                connectors.add(connector);
            }
        }
        return connectors;
    }

    private boolean isPrimary(final Connector connector) {
        for (final Node node : nodes) {
            if (node instanceof Connector candidate && candidate.network() == connector.network()) {
                return candidate == connector;
            }
        }
        return false;
    }

    private static Node createNode(final Environment environment) {
        return Network.newNode(environment, Visibility.None)
            .withConnector(connectorBufferSize())
            .create();
    }

    private void setVisualBufferRatio(final double ratio) {
        final double clampedRatio = clampRatio(ratio);
        if (Math.abs(visualBufferRatio - clampedRatio) < 1D / 255D) {
            return;
        }
        visualBufferRatio = clampedRatio;
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private static double clampRatio(final double ratio) {
        return Math.max(0D, Math.min(1D, ratio));
    }

    public static double connectorBufferSize() {
        return ModSettings.powerDistributorBuffer();
    }
}
