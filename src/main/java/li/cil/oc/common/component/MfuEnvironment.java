package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.ModSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class MfuEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    public static final int LEGACY_TARGET_TAG_LENGTH = 4;
    public static final int TARGET_TAG_LENGTH = 5;

    private static final String TARGET_TAG = "oc:target";
    private static final String SIDE_TAG = "oc:side";
    private static final String BLOCK_TAG = "oc:adapter.block";
    private static final String NAME_TAG = "name";
    private static final String DATA_TAG = "data";
    private static final Set<MfuEnvironment> ACTIVE = Collections.newSetFromMap(new WeakHashMap<>());
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
        synchronized (ACTIVE) {
            ACTIVE.add(this);
        }
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

    public static void refreshTargetChanged(final LevelAccessor level, final BlockPos pos) {
        if (!(level instanceof final Level world) || world.isClientSide()) {
            return;
        }

        final List<MfuEnvironment> environments;
        synchronized (ACTIVE) {
            environments = List.copyOf(ACTIVE);
        }
        for (final MfuEnvironment environment : environments) {
            environment.refreshIfTargetChanged(world, pos);
        }
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
        if (shouldDrainEnergy() && !tryConsumeEnergy()) {
            removeTargetEnvironment();
            disconnectTargetNode();
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
            synchronized (ACTIVE) {
                ACTIVE.remove(this);
            }
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

    private void refreshIfTargetChanged(final Level world, final BlockPos pos) {
        if (host != null && host.world() == world && target.equals(pos)) {
            refreshTargetEnvironment();
        }
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

    private boolean shouldDrainEnergy() {
        return host != null
            && host.world() != null
            && host.world().getGameTime() % ModSettings.mfuTickFrequency() == 0;
    }

    private boolean tryConsumeEnergy() {
        if (!(node() instanceof final Connector connector)) {
            return true;
        }
        final double cost = ModSettings.mfuRelayCost() * ModSettings.mfuTickFrequency() * distanceToTarget();
        return cost <= 0D || connector.tryChangeBuffer(-cost);
    }

    private double distanceToTarget() {
        final double dx = target.getX() - host.xPosition();
        final double dy = target.getY() - host.yPosition();
        final double dz = target.getZ() - host.zPosition();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private boolean targetInRange() {
        final double dx = target.getX() - host.xPosition();
        final double dy = target.getY() - host.yPosition();
        final double dz = target.getZ() - host.zPosition();
        final double range = ModSettings.mfuRange();
        return dx * dx + dy * dy + dz * dz <= range * range;
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
