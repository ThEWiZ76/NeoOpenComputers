package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public class GeneratorUpgradeEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String COMPONENT_NAME = "generator";
    private static final double BUFFER_SIZE = 1D;
    private static final double ENERGY_PER_TICK = 1D;
    private static final String TAG_INVENTORY = "inventory";
    private static final String TAG_REMAINING_TICKS = "remainingTicks";

    private final Agent host;
    private ItemStack queuedFuel = ItemStack.EMPTY;
    private int remainingTicks;

    public GeneratorUpgradeEnvironment(final Agent host) {
        this.host = host;
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.withComponent(COMPONENT_NAME, Visibility.Neighbors).withConnector(BUFFER_SIZE).create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Power,
            DeviceInfo.DeviceAttribute.Description, "Generator",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
            DeviceInfo.DeviceAttribute.Product, "Portagen 2.0 (Rev. 3)",
            DeviceInfo.DeviceAttribute.Capacity, "1"
        );
    }

    @Callback(doc = "function([count:number]):boolean -- Tries to insert fuel from the selected slot into the generator's queue.")
    public Object[] insert(final Context context, final Arguments arguments) {
        final int requestedCount = Math.max(0, arguments.optInteger(0, 64));
        final Container inventory = host.mainInventory();
        final int selectedSlot = host.selectedSlot();
        if (requestedCount == 0) {
            return new Object[]{true, 0};
        }
        if (selectedSlot < 0 || selectedSlot >= inventory.getContainerSize()) {
            return new Object[]{null, "selected slot is empty"};
        }

        final ItemStack selectedStack = inventory.getItem(selectedSlot);
        if (selectedStack.isEmpty()) {
            return new Object[]{null, "selected slot is empty"};
        }
        if (burnTime(selectedStack) <= 0) {
            return new Object[]{null, "selected slot does not contain fuel"};
        }
        if (!queuedFuel.isEmpty() && !ItemStack.isSameItemSameComponents(queuedFuel, selectedStack)) {
            return new Object[]{null, "different fuel type already queued"};
        }

        final int queueCount = queuedFuel.isEmpty() ? 0 : queuedFuel.getCount();
        final int queueLimit = queuedFuel.isEmpty() ? selectedStack.getMaxStackSize() : queuedFuel.getMaxStackSize();
        final int space = queueLimit - queueCount;
        if (space <= 0) {
            return new Object[]{null, "queue is full"};
        }

        final int inserted = Math.min(selectedStack.getCount(), Math.min(space, requestedCount));
        final ItemStack previousSelectedFuel = selectedStack.copy();
        final ItemStack container = selectedStack.getCraftingRemainingItem();
        final ItemStack moved = selectedStack.split(inserted);
        if (selectedStack.isEmpty()) {
            inventory.setItem(selectedSlot, ItemStack.EMPTY);
        }

        if (!container.isEmpty()) {
            if (host.player() == null) {
                inventory.setItem(selectedSlot, previousSelectedFuel);
                return new Object[]{false, "no inventory space available for fuel containers"};
            }
            container.setCount(container.getCount() * inserted);
            if (!host.player().getInventory().add(container)) {
                inventory.setItem(selectedSlot, previousSelectedFuel);
                return new Object[]{false, "no inventory space available for fuel containers"};
            }
            if (!container.isEmpty()) {
                host.player().drop(container.copy(), false);
            }
        }

        if (queuedFuel.isEmpty()) {
            queuedFuel = moved;
        } else {
            queuedFuel.grow(inserted);
        }
        host.markChanged();
        return new Object[]{true, inserted};
    }

    @Callback(doc = "function():number -- Get the size of the item stack in the generator's queue.")
    public Object[] count(final Context context, final Arguments arguments) {
        if (queuedFuel.isEmpty()) {
            return new Object[]{0};
        }
        return new Object[]{queuedFuel.getCount(), queuedFuel.getHoverName().getString()};
    }

    @Callback(doc = "function([count:number]):boolean -- Tries to remove items from the generator's queue.")
    public Object[] remove(final Context context, final Arguments arguments) {
        final int count = Math.max(0, arguments.optInteger(0, Integer.MAX_VALUE));
        if (count == 0) {
            return new Object[]{true, 0};
        }
        if (queuedFuel.isEmpty()) {
            return new Object[]{false, "queue is empty"};
        }
        if (host.player() == null) {
            return new Object[]{false, "no player inventory available"};
        }

        final ItemStack requiredContainer = queuedFuel.getCraftingRemainingItem();
        final ItemStack previousSelectedItem = host.mainInventory().getItem(host.selectedSlot()).copy();
        int removeLimit = Math.min(count, queuedFuel.getCount());
        if (!requiredContainer.isEmpty()) {
            if (previousSelectedItem.isEmpty() || !ItemStack.isSameItemSameComponents(previousSelectedItem, requiredContainer)) {
                return new Object[]{false, "removing this fuel requires the appropriate container in the selected slot"};
            }
            removeLimit = Math.min(removeLimit, previousSelectedItem.getCount() / Math.max(1, requiredContainer.getCount()));
        }
        if (removeLimit <= 0) {
            return new Object[]{false, "removing this fuel requires the appropriate container in the selected slot"};
        }

        final ItemStack previousQueue = queuedFuel.copy();
        final ItemStack removed = queuedFuel.split(removeLimit);
        final int removedCount = removed.getCount();
        if (!requiredContainer.isEmpty()) {
            final ItemStack selectedStack = host.mainInventory().getItem(host.selectedSlot());
            selectedStack.shrink(removeLimit * Math.max(1, requiredContainer.getCount()));
            if (selectedStack.isEmpty()) {
                host.mainInventory().setItem(host.selectedSlot(), ItemStack.EMPTY);
            }
        }
        if (!host.player().getInventory().add(removed)) {
            queuedFuel = previousQueue;
            host.mainInventory().setItem(host.selectedSlot(), previousSelectedItem);
            return new Object[]{false, "no inventory space available for fuel"};
        }
        final int actualRemoval = removedCount - removed.getCount();
        if (actualRemoval < removedCount) {
            queuedFuel.grow(removed.getCount());
            if (!requiredContainer.isEmpty()) {
                final ItemStack selectedStack = host.mainInventory().getItem(host.selectedSlot());
                if (selectedStack.isEmpty()) {
                    host.mainInventory().setItem(host.selectedSlot(), requiredContainer.copyWithCount(removed.getCount()));
                } else {
                    selectedStack.grow(removed.getCount() * Math.max(1, requiredContainer.getCount()));
                }
            }
        }
        if (queuedFuel.isEmpty()) {
            queuedFuel = ItemStack.EMPTY;
        }
        host.markChanged();
        return new Object[]{true, actualRemoval};
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        if (remainingTicks <= 0 && !queuedFuel.isEmpty()) {
            remainingTicks = burnTime(queuedFuel);
            if (remainingTicks > 0) {
                queuedFuel.shrink(1);
                if (queuedFuel.isEmpty()) {
                    queuedFuel = ItemStack.EMPTY;
                }
                host.markChanged();
            }
        }
        if (remainingTicks > 0) {
            remainingTicks--;
            if (node() instanceof Connector connector) {
                connector.changeBuffer(ENERGY_PER_TICK);
            }
            host.markChanged();
        }
    }

    @Override
    public void onDisconnect(final Node node) {
        super.onDisconnect(node);
        if (node == node()) {
            if (!queuedFuel.isEmpty() && host.world() != null) {
                final ItemEntity entity = new ItemEntity(host.world(), host.xPosition(), host.yPosition(), host.zPosition(), queuedFuel.copy());
                entity.setDeltaMovement(0D, 0.04D, 0D);
                entity.setPickUpDelay(5);
                host.world().addFreshEntity(entity);
            }
            queuedFuel = ItemStack.EMPTY;
            remainingTicks = 0;
        }
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        queuedFuel = tag.contains(TAG_INVENTORY)
            ? ItemStack.OPTIONAL_CODEC.parse(NbtOps.INSTANCE, tag.get(TAG_INVENTORY)).result().orElse(ItemStack.EMPTY)
            : ItemStack.EMPTY;
        remainingTicks = tag.getInt(TAG_REMAINING_TICKS);
    }

    @Override
    public void save(final CompoundTag tag) {
        super.save(tag);
        if (!queuedFuel.isEmpty()) {
            tag.put(TAG_INVENTORY, ItemStack.OPTIONAL_CODEC.encodeStart(NbtOps.INSTANCE, queuedFuel).result().orElseGet(CompoundTag::new));
        }
        if (remainingTicks > 0) {
            tag.putInt(TAG_REMAINING_TICKS, remainingTicks);
        }
    }

    private static int burnTime(final ItemStack stack) {
        return stack.getBurnTime(null);
    }
}
