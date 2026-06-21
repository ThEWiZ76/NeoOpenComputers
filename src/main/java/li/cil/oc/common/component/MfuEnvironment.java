package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;

public final class MfuEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    public static final int LEGACY_TARGET_TAG_LENGTH = 4;
    public static final int TARGET_TAG_LENGTH = 5;
    public static final double DEFAULT_RANGE = 3D;

    private static final String TARGET_TAG = "oc:target";
    private static final String SIDE_TAG = "oc:side";
    private static final String BLOCK_TAG = "oc:adapter.block";
    private static final String NAME_TAG = "name";
    private static final String DATA_TAG = "data";
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Bus,
        DeviceInfo.DeviceAttribute.Description, "Remote Adapter",
        DeviceInfo.DeviceAttribute.Vendor, "Scummtech, Inc.",
        DeviceInfo.DeviceAttribute.Product, "ERR NAME NOT FOUND"
    );

    private final EnvironmentHost host;
    private final BlockPos target;
    private final Direction side;
    private ManagedEnvironment targetEnvironment;
    private DriverBlock targetDriver;
    private Node targetNode;
    private String targetEnvironmentName;
    private CompoundTag targetEnvironmentData;

    public MfuEnvironment(final EnvironmentHost host, final BlockPos target, final Direction side) {
        this.host = host;
        this.target = target;
        this.side = side == null ? Direction.NORTH : side;
        final var builder = Network.newNode(this, Visibility.None);
        if (builder != null) {
            setNode(builder.withConnector().create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return DEVICE_INFO;
    }

    public BlockPos target() {
        return target;
    }

    public Direction side() {
        return side;
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        refreshTargetEnvironment();
        if (targetEnvironment != null && targetEnvironment.canUpdate()) {
            targetEnvironment.update();
        }
    }

    @Override
    public void onConnect(final Node node) {
        super.onConnect(node);
        if (node == node()) {
            refreshTargetEnvironment();
        }
    }

    @Override
    public void onDisconnect(final Node node) {
        super.onDisconnect(node);
        if (targetEnvironment != null && node == targetEnvironment.node()) {
            targetEnvironment = null;
            targetDriver = null;
        } else if (targetNode != null && node == targetNode) {
            targetNode = null;
        } else if (node == node()) {
            removeTargetEnvironment();
            disconnectTargetNode();
        }
    }

    @Override
    public void onMessage(final Message message) {
        refreshTargetEnvironment();
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        if (tag.contains(BLOCK_TAG)) {
            final CompoundTag blockTag = tag.getCompound(BLOCK_TAG);
            if (blockTag.contains(NAME_TAG) && blockTag.contains(DATA_TAG)) {
                targetEnvironmentName = blockTag.getString(NAME_TAG);
                targetEnvironmentData = blockTag.getCompound(DATA_TAG);
            }
        }
    }

    @Override
    public void save(final CompoundTag tag) {
        super.save(tag);
        tag.putIntArray(TARGET_TAG, new int[]{target.getX(), target.getY(), target.getZ()});
        tag.putInt(SIDE_TAG, side.ordinal());
        saveTargetEnvironment(tag);
    }

    private void refreshTargetEnvironment() {
        if (node() == null || node().network() == null || host == null || host.world() == null || host.world().isClientSide()) {
            return;
        }

        final Level world = host.world();
        if (!targetInRange()) {
            removeTargetEnvironment();
            disconnectTargetNode();
            return;
        }

        final BlockEntity blockEntity = world.getBlockEntity(target);
        if (blockEntity instanceof final Environment tileEnvironment) {
            final Node tileNode = tileNode(tileEnvironment);
            removeTargetEnvironment();
            if (tileNode == null) {
                disconnectTargetNode();
                return;
            }
            if (targetNode != null && targetNode != tileNode) {
                disconnectTargetNode();
            }
            targetNode = tileNode;
            if (tileNode.network() != node().network()) {
                node().connect(tileNode);
            }
            return;
        }

        disconnectTargetNode();
        final DriverBlock driver = li.cil.oc.api.Driver.driverFor(world, target, side);
        if (driver == null) {
            removeTargetEnvironment();
            return;
        }
        if (targetEnvironment != null && targetDriver == driver) {
            if (targetEnvironment.node() != null && targetEnvironment.node().network() != node().network()) {
                node().connect(targetEnvironment.node());
            }
            return;
        }

        removeTargetEnvironment();
        final ManagedEnvironment environment = driver.createEnvironment(world, target, side);
        if (environment == null || environment.node() == null) {
            return;
        }
        final String environmentName = environment.getClass().getName();
        if (targetEnvironmentData != null && environmentName.equals(targetEnvironmentName)) {
            environment.load(targetEnvironmentData);
        }
        targetEnvironment = environment;
        targetDriver = driver;
        targetEnvironmentName = environmentName;
        if (targetEnvironmentData == null) {
            targetEnvironmentData = new CompoundTag();
        }
        node().connect(environment.node());
    }

    private void removeTargetEnvironment() {
        final ManagedEnvironment environment = targetEnvironment;
        if (environment != null) {
            final CompoundTag data = targetEnvironmentData != null ? targetEnvironmentData : new CompoundTag();
            environment.save(data);
            targetEnvironmentData = data;
            final Node environmentNode = environment.node();
            if (environmentNode != null && node() != null) {
                node().disconnect(environmentNode);
                environmentNode.remove();
            }
        }
        targetEnvironment = null;
        targetDriver = null;
    }

    private void disconnectTargetNode() {
        final Node remoteNode = targetNode;
        if (remoteNode != null && node() != null) {
            node().disconnect(remoteNode);
        }
        targetNode = null;
    }

    private Node tileNode(final Environment environment) {
        if (environment instanceof final SidedEnvironment sidedEnvironment) {
            return sidedEnvironment.sidedNode(side);
        }
        return environment.node();
    }

    private boolean targetInRange() {
        final double dx = target.getX() - host.xPosition();
        final double dy = target.getY() - host.yPosition();
        final double dz = target.getZ() - host.zPosition();
        return dx * dx + dy * dy + dz * dz <= DEFAULT_RANGE * DEFAULT_RANGE;
    }

    private void saveTargetEnvironment(final CompoundTag tag) {
        if (targetEnvironment != null) {
            final CompoundTag data = targetEnvironmentData != null ? targetEnvironmentData : new CompoundTag();
            targetEnvironment.save(data);
            targetEnvironmentData = data;
        }
        if (targetEnvironmentData == null || targetEnvironmentName == null) {
            return;
        }
        final CompoundTag blockTag = new CompoundTag();
        blockTag.putString(NAME_TAG, targetEnvironmentName);
        blockTag.put(DATA_TAG, targetEnvironmentData);
        tag.put(BLOCK_TAG, blockTag);
    }
}
