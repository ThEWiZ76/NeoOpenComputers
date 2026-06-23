package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.internal.Tablet;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;

public class TractorBeamUpgradeEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String COMPONENT_NAME = "tractor_beam";
    private static final double PICKUP_RADIUS = 3.0D;
    private static final double SUCK_DELAY = 0.44D;
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
        DeviceInfo.DeviceAttribute.Description, "Tractor beam",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
        DeviceInfo.DeviceAttribute.Product, "T313-K1N.3515"
    );

    private final EnvironmentHost host;
    private final PickupStrategy pickupStrategy;

    public TractorBeamUpgradeEnvironment(final Agent host) {
        this(host, new AgentInventoryPickupStrategy(host));
    }

    public TractorBeamUpgradeEnvironment(final Tablet host) {
        this(host, item -> pickupByPlayer(host.player(), item));
    }

    private TractorBeamUpgradeEnvironment(final EnvironmentHost host, final PickupStrategy pickupStrategy) {
        this.host = host;
        this.pickupStrategy = pickupStrategy;
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.withComponent(COMPONENT_NAME).create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return DEVICE_INFO;
    }

    @Callback(doc = "function():boolean -- Tries to pick up a random item stack nearby.")
    public Object[] suck(final Context context, final Arguments arguments) {
        final Level level = host.world();
        if (level == null) {
            return new Object[]{false};
        }

        final List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, pickupBounds())
            .stream()
            .filter(item -> !item.isRemoved())
            .filter(item -> !item.getItem().isEmpty())
            .filter(item -> !item.hasPickUpDelay())
            .toList();
        if (items.isEmpty()) {
            return new Object[]{false};
        }

        final ItemEntity item = items.get(level.random.nextInt(items.size()));
        final int originalCount = item.getItem().getCount();
        pickupStrategy.pickup(item);
        if (item.isRemoved() || item.getItem().isEmpty() || item.getItem().getCount() < originalCount) {
            if (context != null) {
                context.pause(SUCK_DELAY);
            }
            level.levelEvent(LevelEvent.PARTICLES_EYE_OF_ENDER_DEATH, item.blockPosition(), 0);
            host.markChanged();
            return new Object[]{true};
        }
        return new Object[]{false};
    }

    private AABB pickupBounds() {
        return new AABB(
            host.xPosition() - PICKUP_RADIUS,
            host.yPosition() - PICKUP_RADIUS,
            host.zPosition() - PICKUP_RADIUS,
            host.xPosition() + PICKUP_RADIUS,
            host.yPosition() + PICKUP_RADIUS,
            host.zPosition() + PICKUP_RADIUS);
    }

    private static void pickupByPlayer(final Player player, final ItemEntity entity) {
        if (player != null) {
            entity.playerTouch(player);
        }
    }

    private interface PickupStrategy {
        void pickup(ItemEntity entity);
    }

    private static final class AgentInventoryPickupStrategy implements PickupStrategy {
        private final Agent host;

        private AgentInventoryPickupStrategy(final Agent host) {
            this.host = host;
        }

        @Override
        public void pickup(final ItemEntity entity) {
            insertIntoInventory(entity);
        }

        private void insertIntoInventory(final ItemEntity entity) {
            final ItemStack stack = entity.getItem();
            final int remaining = insertIntoInventory(host.mainInventory(), stack);
            stack.setCount(remaining);
            if (remaining <= 0) {
                entity.discard();
            } else {
                entity.setItem(stack);
            }
        }

        private int insertIntoInventory(final Container inventory, final ItemStack stack) {
            int remaining = stack.getCount();
            final int size = inventory.getContainerSize();
            final int selectedSlot = host.selectedSlot();
            final int startSlot = selectedSlot >= 0 && selectedSlot < size ? selectedSlot : 0;
            for (int offset = 0; offset < size && remaining > 0; offset++) {
                final int slot = (startSlot + offset) % size;
                final ItemStack existing = inventory.getItem(slot);
                if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, stack)) {
                    continue;
                }
                final int limit = Math.min(existing.getMaxStackSize(), inventory.getMaxStackSize());
                final int inserted = Math.min(remaining, limit - existing.getCount());
                if (inserted > 0 && inventory.canPlaceItem(slot, stack)) {
                    existing.grow(inserted);
                    remaining -= inserted;
                }
            }
            for (int offset = 0; offset < size && remaining > 0; offset++) {
                final int slot = (startSlot + offset) % size;
                if (!inventory.getItem(slot).isEmpty()) {
                    continue;
                }
                final int inserted = Math.min(remaining, Math.min(stack.getMaxStackSize(), inventory.getMaxStackSize()));
                final ItemStack insertedStack = stack.copy();
                insertedStack.setCount(inserted);
                if (inventory.canPlaceItem(slot, insertedStack)) {
                    inventory.setItem(slot, insertedStack);
                    remaining -= inserted;
                }
            }
            return remaining;
        }
    }
}
