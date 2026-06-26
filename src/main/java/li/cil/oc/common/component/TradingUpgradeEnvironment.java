package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.ModSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TradingUpgradeEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String COMPONENT_NAME = "trading";
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
        DeviceInfo.DeviceAttribute.Description, "Trading upgrade",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
        DeviceInfo.DeviceAttribute.Product, "Capitalism H.O. 1200T"
    );

    private final EnvironmentHost host;

    public TradingUpgradeEnvironment(final EnvironmentHost host) {
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

    @Callback(doc = "function():table -- Returns a table of trades in range as userdata objects.")
    public Object[] getTrades(final Context context, final Arguments arguments) {
        if (host == null || host.world() == null) {
            return new Object[]{List.of()};
        }

        final List<Entity> merchants = merchants();
        final Map<UUID, Integer> merchantIds = merchantIds(merchants);
        final ArrayList<TradeValue> trades = new ArrayList<>();
        for (final Entity entity : merchants) {
            final Merchant merchant = (Merchant) entity;
            final int merchantId = merchantIds.get(entity.getUUID());
            for (int index = 0; index < merchant.getOffers().size(); index++) {
                trades.add(new TradeValue(host, entity, merchant, index, merchantId));
            }
        }
        return new Object[]{trades};
    }

    private List<Entity> merchants() {
        final Level level = host.world();
        final BlockPos center = BlockPos.containing(host.xPosition(), host.yPosition(), host.zPosition());
        final double range = ModSettings.tradingRange();
        return level.getEntitiesOfClass(Entity.class, AABB.ofSize(center.getCenter(), range * 2D, range * 2D, range * 2D))
            .stream()
            .filter(entity -> entity instanceof Merchant)
            .filter(this::isInRange)
            .sorted(Comparator.comparing(Entity::getUUID))
            .toList();
    }

    private boolean isInRange(final Entity entity) {
        final double range = ModSettings.tradingRange();
        return entity.distanceToSqr(host.xPosition(), host.yPosition(), host.zPosition()) <= range * range;
    }

    private static Map<UUID, Integer> merchantIds(final List<Entity> merchants) {
        final HashMap<UUID, Integer> ids = new HashMap<>();
        int nextId = 1;
        for (final Entity merchant : merchants) {
            ids.put(merchant.getUUID(), nextId++);
        }
        return ids;
    }
}
