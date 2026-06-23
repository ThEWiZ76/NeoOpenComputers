package li.cil.oc.common.blockentity;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PowerDistributorBlockEntity extends BlockEntity implements Environment, SidedEnvironment, DeviceInfo {
    public static final double CONNECTOR_BUFFER_SIZE = 500D;

    private static final String TAG_CONNECTORS = "oc:connectors";
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Power,
        DeviceInfo.DeviceAttribute.Description, "Power distributor",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
        DeviceInfo.DeviceAttribute.Product, "Power Distributor"
    );

    private final Node[] nodes = new Node[Direction.values().length];

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
    public Map<String, String> getDeviceInfo() {
        final Map<String, String> metadata = new java.util.HashMap<>(DEVICE_INFO);
        metadata.put(DeviceInfo.DeviceAttribute.Capacity, Double.toString(connectorBufferSize()));
        return metadata;
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
            return;
        }
        final double ratio = totalBuffer / totalSize;
        for (final Connector connector : connectors) {
            connector.changeBuffer(connector.globalBufferSize() * ratio - connector.globalBuffer());
        }
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

    public static double connectorBufferSize() {
        return ModSettings.powerDistributorBuffer();
    }
}
