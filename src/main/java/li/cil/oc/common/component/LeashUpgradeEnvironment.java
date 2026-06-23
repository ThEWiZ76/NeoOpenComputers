package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.internal.Tablet;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class LeashUpgradeEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String COMPONENT_NAME = "leash";
    private static final int MAX_LEASHED_ENTITIES = 8;
    private static final String TAG_LEASHED_ENTITIES = "leashedEntities";
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
        DeviceInfo.DeviceAttribute.Description, "Leash",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
        DeviceInfo.DeviceAttribute.Product, "FlockControl (FC-3LS)",
        DeviceInfo.DeviceAttribute.Capacity, Integer.toString(MAX_LEASHED_ENTITIES)
    );

    private final EnvironmentHost host;
    private final Set<UUID> leashedEntities = new HashSet<>();

    public LeashUpgradeEnvironment(final EnvironmentHost host) {
        this.host = host;
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.withComponent(COMPONENT_NAME).create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return DEVICE_INFO;
    }

    @Callback(doc = "function(side:number):boolean -- Tries to put an entity on the specified side of the device onto a leash.")
    public Object[] leash(final Context context, final Arguments arguments) {
        if (leashedEntities.size() >= MAX_LEASHED_ENTITIES) {
            return new Object[]{null, "too many leashed entities"};
        }
        final Entity holder = leashHolder();
        final Level level = host == null ? null : host.world();
        if (holder == null || level == null) {
            return new Object[]{null, "no unleashed entity"};
        }

        final Direction direction = Direction.from3DDataValue(arguments.checkInteger(0));
        final AABB bounds = leashBounds(direction);
        for (final Entity entity : level.getEntitiesOfClass(Entity.class, bounds)) {
            if (entity instanceof Leashable leashable && leashable.canHaveALeashAttachedToIt()) {
                leashable.setLeashedTo(holder, true);
                leashedEntities.add(entity.getUUID());
                if (context != null) {
                    context.pause(0.1);
                }
                host.markChanged();
                return new Object[]{true};
            }
        }
        return new Object[]{null, "no unleashed entity"};
    }

    @Callback(doc = "function() -- Unleashes all currently leashed entities.")
    public Object[] unleash(final Context context, final Arguments arguments) {
        unleashAll();
        return null;
    }

    @Override
    public void onDisconnect(final Node node) {
        super.onDisconnect(node);
        if (node == node()) {
            unleashAll();
        }
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        leashedEntities.clear();
        final ListTag list = tag.getList(TAG_LEASHED_ENTITIES, Tag.TAG_STRING);
        for (int index = 0; index < list.size(); index++) {
            try {
                leashedEntities.add(UUID.fromString(list.getString(index)));
            } catch (final IllegalArgumentException ignored) {
                // Ignore malformed legacy data instead of failing environment load.
            }
        }
    }

    @Override
    public void save(final CompoundTag tag) {
        super.save(tag);
        final ListTag list = new ListTag();
        for (final UUID uuid : leashedEntities) {
            list.add(StringTag.valueOf(uuid.toString()));
        }
        tag.put(TAG_LEASHED_ENTITIES, list);
    }

    private void unleashAll() {
        final Level level = host == null ? null : host.world();
        if (level == null || leashedEntities.isEmpty()) {
            leashedEntities.clear();
            return;
        }

        final AABB bounds = new AABB(hostPosition()).inflate(5D);
        for (final Entity entity : level.getEntitiesOfClass(Entity.class, bounds)) {
            if (leashedEntities.contains(entity.getUUID()) && entity instanceof Leashable leashable) {
                leashable.dropLeash(true, false);
            }
        }
        leashedEntities.clear();
        host.markChanged();
    }

    private Entity leashHolder() {
        if (host instanceof Agent agent) {
            return agent.player();
        }
        if (host instanceof Tablet tablet) {
            return tablet.player();
        }
        return host instanceof Entity entity ? entity : null;
    }

    private AABB leashBounds(final Direction direction) {
        final BlockPos origin = hostPosition();
        return new AABB(origin).minmax(new AABB(origin.relative(direction, 2)));
    }

    private BlockPos hostPosition() {
        return BlockPos.containing(host.xPosition(), host.yPosition(), host.zPosition());
    }
}
