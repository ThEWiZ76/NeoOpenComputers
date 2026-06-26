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
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class NavigationUpgradeEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String COMPONENT_NAME = "navigation";
    private static final int DEFAULT_MAP_SIZE = 128;
    private static final double MAX_WAYPOINT_RANGE = 400.0D;
    private static final double WAYPOINT_COST_PER_RANGE = 0.05D * 0.25D;

    private final EnvironmentHost host;
    private NavigationMapData mapData;

    public NavigationUpgradeEnvironment(final EnvironmentHost host) {
        this(host, NavigationMapData.DEFAULT);
    }

    public NavigationUpgradeEnvironment(final EnvironmentHost host, final NavigationMapData mapData) {
        this.host = host;
        this.mapData = mapData == null ? NavigationMapData.DEFAULT : mapData.resolve(host == null ? null : host.world());
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.withComponent(COMPONENT_NAME, Visibility.Neighbors).withConnector().create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
            DeviceInfo.DeviceAttribute.Description, "Navigation upgrade",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
            DeviceInfo.DeviceAttribute.Product, "PathFinder v3",
            DeviceInfo.DeviceAttribute.Capacity, Integer.toString(currentMapData().size())
        );
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
        final NavigationMapData currentMap = currentMapData();
        nbt.putInt("posX", blockPos.getX() - currentMap.centerX());
        nbt.putInt("posY", blockPos.getY());
        nbt.putInt("posZ", blockPos.getZ() - currentMap.centerZ());
    }

    @Callback(doc = "function():number, number, number -- Returns the current map-relative position.")
    public Object[] getPosition(final Context context, final Arguments args) {
        if (host == null) {
            return new Object[]{null, "no host"};
        }
        final NavigationMapData data = currentMapData();
        final double relativeX = host.xPosition() - data.centerX();
        final double relativeZ = host.zPosition() - data.centerZ();
        final double range = data.size() / 2.0D;
        if (Math.abs(relativeX) <= range && Math.abs(relativeZ) <= range) {
            return new Object[]{relativeX, host.yPosition(), relativeZ};
        }
        return new Object[]{null, "out of range"};
    }

    @Callback(doc = "function():number -- Returns the host facing.")
    public Object[] getFacing(final Context context, final Arguments args) {
        if (host instanceof Rotatable rotatable) {
            return new Object[]{rotatable.facing().get3DDataValue()};
        }
        return new Object[]{Direction.NORTH.get3DDataValue()};
    }

    @Callback(doc = "function():number -- Returns the waypoint scan range.")
    public Object[] getRange(final Context context, final Arguments args) {
        return new Object[]{currentMapData().size() / 2.0D};
    }

    @Callback(doc = "function([range:number]):table -- Finds nearby waypoints.")
    public Object[] findWaypoints(final Context context, final Arguments args) {
        final double range = Math.max(0D, Math.min(args.checkDouble(0), MAX_WAYPOINT_RANGE));
        if (range <= 0D) {
            return new Object[]{new Map[0]};
        }
        if (!consumeEnergy(range * WAYPOINT_COST_PER_RANGE)) {
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

    private boolean consumeEnergy(final double cost) {
        return !(node() instanceof Connector connector) || connector.tryChangeBuffer(-cost);
    }

    private NavigationMapData currentMapData() {
        final NavigationMapData resolved = mapData.resolve(host == null ? null : host.world());
        if (!resolved.equals(mapData)) {
            mapData = resolved;
        }
        return mapData;
    }

    @Override
    public void load(final CompoundTag nbt) {
        super.load(nbt);
        mapData = NavigationMapData.load(nbt, mapData);
    }

    @Override
    public void save(final CompoundTag nbt) {
        super.save(nbt);
        currentMapData().save(nbt);
    }

    public record NavigationMapData(int centerX, int centerZ, int scale, int mapId) {
        public static final int NO_MAP_ID = -1;
        public static final NavigationMapData DEFAULT = new NavigationMapData(0, 0, 0, NO_MAP_ID);

        private static final String TAG_MAP = "oc:map";
        private static final String TAG_CENTER_X = "centerX";
        private static final String TAG_CENTER_Z = "centerZ";
        private static final String TAG_SCALE = "scale";
        private static final String TAG_MAP_ID = "mapId";

        public NavigationMapData {
            scale = Mth.clamp(scale, 0, MapItemSavedData.MAX_SCALE);
        }

        public int size() {
            return DEFAULT_MAP_SIZE * (1 << scale);
        }

        public NavigationMapData resolve(final Level level) {
            if (mapId == NO_MAP_ID || level == null) {
                return this;
            }
            final MapItemSavedData savedData = net.minecraft.world.item.MapItem.getSavedData(new MapId(mapId), level);
            return savedData == null ? this : fromSavedData(savedData, mapId);
        }

        public void save(final CompoundTag nbt) {
            final CompoundTag mapTag = new CompoundTag();
            mapTag.putInt(TAG_CENTER_X, centerX);
            mapTag.putInt(TAG_CENTER_Z, centerZ);
            mapTag.putInt(TAG_SCALE, scale);
            if (mapId != NO_MAP_ID) {
                mapTag.putInt(TAG_MAP_ID, mapId);
            }
            nbt.put(TAG_MAP, mapTag);
        }

        public void saveToDataTag(final CompoundTag dataTag) {
            save(dataTag);
        }

        public static NavigationMapData load(final CompoundTag nbt, final NavigationMapData fallback) {
            if (nbt == null || !nbt.contains(TAG_MAP, Tag.TAG_COMPOUND)) {
                return fallback == null ? DEFAULT : fallback;
            }
            final CompoundTag mapTag = nbt.getCompound(TAG_MAP);
            final int mapId = mapTag.contains(TAG_MAP_ID, Tag.TAG_INT) ? mapTag.getInt(TAG_MAP_ID) : NO_MAP_ID;
            final int centerX = mapTag.contains(TAG_CENTER_X, Tag.TAG_INT) ? mapTag.getInt(TAG_CENTER_X) : 0;
            final int centerZ = mapTag.contains(TAG_CENTER_Z, Tag.TAG_INT) ? mapTag.getInt(TAG_CENTER_Z) : 0;
            final int scale = mapTag.contains(TAG_SCALE, Tag.TAG_INT) ? mapTag.getInt(TAG_SCALE) : 0;
            return new NavigationMapData(centerX, centerZ, scale, mapId);
        }

        public static NavigationMapData fromDataTag(final CompoundTag dataTag, final Level level) {
            return load(dataTag, DEFAULT).resolve(level);
        }

        public static NavigationMapData fromSavedData(final MapItemSavedData savedData, final int mapId) {
            return new NavigationMapData(savedData.centerX, savedData.centerZ, savedData.scale, mapId);
        }

        public static NavigationMapData fromMapId(final int mapId, final Level level) {
            if (level == null) {
                return new NavigationMapData(0, 0, 0, mapId);
            }
            final MapItemSavedData savedData = net.minecraft.world.item.MapItem.getSavedData(new MapId(mapId), level);
            return savedData == null ? new NavigationMapData(0, 0, 0, mapId) : fromSavedData(savedData, mapId);
        }
    }
}
