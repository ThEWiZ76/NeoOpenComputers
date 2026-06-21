package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public final class MotionSensorEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String TAG_SENSITIVITY = "oc:sensitivity";
    private static final String COMPONENT_NAME = "motion_sensor";
    private static final int RADIUS = 8;
    private static final double MINIMUM_SENSITIVITY = 0.2D;
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
        DeviceInfo.DeviceAttribute.Description, "Motion sensor",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
        DeviceInfo.DeviceAttribute.Product, "Blinker M1K0",
        DeviceInfo.DeviceAttribute.Capacity, Integer.toString(RADIUS)
    );

    private final EnvironmentHost host;
    private final Map<Integer, Vec3> trackedEntities = new HashMap<>();
    private double sensitivity = 0.4D;

    public MotionSensorEnvironment(final EnvironmentHost host) {
        this.host = host;
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.withComponent(COMPONENT_NAME, Visibility.Network).withConnector().create());
        }
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        final Level level = host == null ? null : host.world();
        if (level != null && !level.isClientSide && level.getGameTime() % 10L == 0L) {
            detectMotion(level, BlockPos.containing(host.xPosition(), host.yPosition(), host.zPosition()));
        }
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
        markChanged();
        return new Object[]{oldValue};
    }

    @Override
    public void load(final CompoundTag nbt) {
        super.load(nbt);
        if (nbt.contains(TAG_SENSITIVITY)) {
            sensitivity = Math.max(MINIMUM_SENSITIVITY, nbt.getDouble(TAG_SENSITIVITY));
        }
    }

    @Override
    public void save(final CompoundTag nbt) {
        super.save(nbt);
        nbt.putDouble(TAG_SENSITIVITY, sensitivity);
    }

    private void detectMotion(final Level level, final BlockPos pos) {
        final Vec3 center = Vec3.atCenterOf(pos);
        final AABB bounds = new AABB(pos).inflate(RADIUS);
        final Map<Integer, Vec3> visibleEntities = new HashMap<>();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, bounds, entity -> entity.isAlive() && isVisible(level, center, entity))) {
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

    private static boolean isVisible(final Level level, final Vec3 center, final LivingEntity entity) {
        if (entity.isInvisible()) {
            return false;
        }
        return hasClearPath(level, center, entity.position(), entity) || hasClearPath(level, center, entity.getEyePosition(), entity);
    }

    private static boolean hasClearPath(final Level level, final Vec3 center, final Vec3 target, final LivingEntity entity) {
        return level.clip(new ClipContext(center, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)).getType() == HitResult.Type.MISS;
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

    private void markChanged() {
        if (host != null) {
            host.markChanged();
        }
    }
}
