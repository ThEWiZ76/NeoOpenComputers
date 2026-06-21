package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Rotatable;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.blockentity.WaypointBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class NavigationUpgradeEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String COMPONENT_NAME = "navigation";
    private static final double RANGE = 16.0D;
    private static final double MAX_WAYPOINT_RANGE = 400.0D;
    private static final double WAYPOINT_COST_PER_RANGE = 0.05D * 0.25D;
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
        DeviceInfo.DeviceAttribute.Description, "Navigation upgrade",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
        DeviceInfo.DeviceAttribute.Product, "PathFinder v3",
        DeviceInfo.DeviceAttribute.Capacity, Integer.toString((int) RANGE)
    );

    private final EnvironmentHost host;

    public NavigationUpgradeEnvironment(final EnvironmentHost host) {
        this.host = host;
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.withComponent(COMPONENT_NAME, Visibility.Neighbors).withConnector().create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return DEVICE_INFO;
    }

    @Override
    public void onMessage(final Message message) {
        super.onMessage(message);
        if (message == null || !"tablet.use".equals(message.name())) {
            return;
        }
        final Object[] data = message.data();
        if (data.length < 4 || !(data[0] instanceof CompoundTag nbt) || !(data[3] instanceof BlockPos blockPos)) {
            return;
        }
        nbt.putInt("posX", blockPos.getX());
        nbt.putInt("posY", blockPos.getY());
        nbt.putInt("posZ", blockPos.getZ());
    }

    @Callback(direct = true, doc = "function():number, number, number -- Returns the current absolute position.")
    public Object[] getPosition(final Context context, final Arguments args) {
        if (host == null) {
            return new Object[]{null, "no host"};
        }
        return new Object[]{host.xPosition(), host.yPosition(), host.zPosition()};
    }

    @Callback(direct = true, doc = "function():number -- Returns the host facing.")
    public Object[] getFacing(final Context context, final Arguments args) {
        if (host instanceof Rotatable rotatable) {
            return new Object[]{rotatable.facing().get3DDataValue()};
        }
        return new Object[]{Direction.NORTH.get3DDataValue()};
    }

    @Callback(direct = true, doc = "function():number -- Returns the waypoint scan range.")
    public Object[] getRange(final Context context, final Arguments args) {
        return new Object[]{RANGE};
    }

    @Callback(doc = "function([range:number]):table -- Finds nearby waypoints.")
    public Object[] findWaypoints(final Context context, final Arguments args) {
        final double range = Math.max(0D, Math.min(args.optDouble(0, RANGE), MAX_WAYPOINT_RANGE));
        if (range <= 0D) {
            return new Object[]{new Map[0]};
        }
        if (!consumeEnergy(context, range * WAYPOINT_COST_PER_RANGE)) {
            return new Object[]{null, "not enough energy"};
        }
        if (context != null) {
            context.pause(0.5D);
        }
        if (host == null || host.world() == null) {
            return new Object[]{new Map[0]};
        }

        final Level level = host.world();
        final BlockPos center = BlockPos.containing(host.xPosition(), host.yPosition(), host.zPosition());
        final int radius = (int) Math.ceil(range);
        final double rangeSquared = range * range;
        final ArrayList<Map<String, Object>> waypoints = new ArrayList<>();

        for (final BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            final BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof WaypointBlockEntity waypoint)) {
                continue;
            }
            final double dx = waypoint.targetXPosition() - host.xPosition();
            final double dy = waypoint.targetYPosition() - host.yPosition();
            final double dz = waypoint.targetZPosition() - host.zPosition();
            if (dx * dx + dy * dy + dz * dz > rangeSquared) {
                continue;
            }
            final Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("position", new Object[]{dx, dy, dz});
            entry.put("redstone", waypoint.redstoneInput());
            entry.put("label", waypoint.label());
            entry.put("address", waypoint.node() == null ? null : waypoint.node().address());
            waypoints.add(entry);
        }

        return new Object[]{waypoints.toArray(new Map[0])};
    }

    private static boolean consumeEnergy(final Context context, final double cost) {
        return context == null || !(context.node() instanceof Connector connector) || connector.tryChangeBuffer(-cost);
    }
}
