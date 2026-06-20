package li.cil.oc.common.blockentity;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public class MotionSensorBlockEntity extends BlockEntity implements Environment, DeviceInfo {
    private static final String TAG_NODE = "node";
    private static final String TAG_SENSITIVITY = "oc:sensitivity";
    private static final int RADIUS = 8;
    private static final double MINIMUM_SENSITIVITY = 0.2D;
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
        DeviceInfo.DeviceAttribute.Description, "Motion sensor",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
        DeviceInfo.DeviceAttribute.Product, "Blinker M1K0",
        DeviceInfo.DeviceAttribute.Capacity, Integer.toString(RADIUS)
    );

    private final Map<Integer, Vec3> trackedEntities = new HashMap<>();
    private Node node;
    private double sensitivity = 0.4D;

    public MotionSensorBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.MOTION_SENSOR.get(), pos, blockState);
        OpenComputersApi.initialize();
        node = createNode(this);
    }

    @Override
    public Node node() {
        if (node == null) {
            node = createNode(this);
        }
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
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return DEVICE_INFO;
    }

    @Callback(direct = true, doc = "function():number -- Gets the current sensor sensitivity.")
    public Object[] getSensitivity(final Context context, final Arguments args) {
        return new Object[]{sensitivity};
    }

    @Callback(direct = true, doc = "function(value:number):number -- Sets the sensor's sensitivity. Returns the old value.")
    public Object[] setSensitivity(final Context context, final Arguments args) {
        final double oldValue = sensitivity;
        sensitivity = Math.max(MINIMUM_SENSITIVITY, args.checkDouble(0));
        setChanged();
        return new Object[]{oldValue};
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final MotionSensorBlockEntity sensor) {
        if (level.getGameTime() % 10L == 0L) {
            sensor.detectMotion(level, pos);
        }
    }

    @Override
    protected void loadAdditional(final CompoundTag nbt, final HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        if (nbt.contains(TAG_NODE)) {
            node().load(nbt.getCompound(TAG_NODE));
        }
        if (nbt.contains(TAG_SENSITIVITY)) {
            sensitivity = Math.max(MINIMUM_SENSITIVITY, nbt.getDouble(TAG_SENSITIVITY));
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag nbt, final HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        saveNode(nbt);
        nbt.putDouble(TAG_SENSITIVITY, sensitivity);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        removeNode();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        removeNode();
    }

    public void removeNode() {
        if (node != null) {
            node.remove();
        }
    }

    private void detectMotion(final Level level, final BlockPos pos) {
        final Vec3 center = Vec3.atCenterOf(pos);
        final AABB bounds = new AABB(pos).inflate(RADIUS);
        final Map<Integer, Vec3> visibleEntities = new HashMap<>();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, bounds, entity -> entity.isAlive() && !entity.isInvisible())) {
            final Vec3 entityPosition = entity.position();
            if (entityPosition.distanceToSqr(center) > RADIUS * RADIUS) {
                continue;
            }
            visibleEntities.put(entity.getId(), entityPosition);
            final Vec3 previous = trackedEntities.get(entity.getId());
            if (previous == null || entityPosition.distanceToSqr(previous) > sensitivity * sensitivity * 2.0D) {
                sendMotionSignal(center, entityPosition);
            }
        }
        trackedEntities.clear();
        trackedEntities.putAll(visibleEntities);
    }

    private void sendMotionSignal(final Vec3 center, final Vec3 entityPosition) {
        if (node() != null) {
            node().sendToReachable(
                "computer.signal",
                "motion",
                entityPosition.x() - center.x(),
                entityPosition.y() - center.y(),
                entityPosition.z() - center.z());
        }
    }

    private void saveNode(final CompoundTag nbt) {
        if (node() == null) {
            return;
        }

        final CompoundTag nodeTag = new CompoundTag();
        if (node().address() == null) {
            Network.joinNewNetwork(node());
            node().save(nodeTag);
            node().remove();
        } else {
            node().save(nodeTag);
        }
        nbt.put(TAG_NODE, nodeTag);
    }

    private static Node createNode(final Environment environment) {
        return Network.newNode(environment, Visibility.Network)
            .withComponent("motion_sensor", Visibility.Network)
            .withConnector()
            .create();
    }
}
