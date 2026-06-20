package li.cil.oc.common.driver;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Map;

public final class InventoryBlockDriver implements DriverBlock {
    @Override
    public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
        return world != null && pos != null && world.getBlockEntity(pos) instanceof Container;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        if (world != null && pos != null && world.getBlockEntity(pos) instanceof Container container) {
            return new Environment(container);
        }
        return null;
    }

    public static final class Environment implements ManagedEnvironment, DeviceInfo {
        private static final Map<String, String> DEVICE_INFO = Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
            DeviceInfo.DeviceAttribute.Description, "Inventory",
            DeviceInfo.DeviceAttribute.Vendor, "Minecraft",
            DeviceInfo.DeviceAttribute.Product, "Container"
        );

        private final Container container;
        private final Node node;

        public Environment(final Container container) {
            this.container = container;
            node = Network.newNode(this, Visibility.Network)
                .withComponent("inventory", Visibility.Network)
                .create();
        }

        @Override
        public Node node() {
            return node;
        }

        @Override
        public boolean canUpdate() {
            return false;
        }

        @Override
        public void update() {
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
        public void load(final CompoundTag nbt) {
            if (nbt.contains("node")) {
                node().load(nbt.getCompound("node"));
            }
        }

        @Override
        public void save(final CompoundTag nbt) {
            final CompoundTag nodeTag = new CompoundTag();
            if (node().address() == null) {
                Network.joinNewNetwork(node());
                node().save(nodeTag);
                node().remove();
            } else {
                node().save(nodeTag);
            }
            nbt.put("node", nodeTag);
        }

        @Override
        public Map<String, String> getDeviceInfo() {
            return DEVICE_INFO;
        }

        @Callback(doc = "function():string -- Get the name of this inventory.")
        public Object[] getInventoryName(final Context context, final Arguments arguments) {
            return new Object[]{container instanceof net.minecraft.world.Nameable nameable ? nameable.getDisplayName().getString() : "Inventory"};
        }

        @Callback(doc = "function():number -- Get the number of slots in this inventory.")
        public Object[] getInventorySize(final Context context, final Arguments arguments) {
            return new Object[]{container.getContainerSize()};
        }

        @Callback(doc = "function(slot:number):number -- Get the stack size of the item stack in the specified slot.")
        public Object[] getSlotStackSize(final Context context, final Arguments arguments) {
            final ItemStack stack = container.getItem(checkSlot(arguments, 0));
            return new Object[]{stack.isEmpty() ? 0 : stack.getCount()};
        }

        @Callback(doc = "function(slot:number):number -- Get the maximum stack size of the item stack in the specified slot.")
        public Object[] getSlotMaxStackSize(final Context context, final Arguments arguments) {
            final ItemStack stack = container.getItem(checkSlot(arguments, 0));
            return new Object[]{stack.isEmpty() ? container.getMaxStackSize() : Math.min(container.getMaxStackSize(), stack.getMaxStackSize())};
        }

        @Callback(doc = "function(slotA:number, slotB:number):boolean -- Compare the two item stacks in the specified slots for equality.")
        public Object[] compareStacks(final Context context, final Arguments arguments) {
            final int slotA = checkSlot(arguments, 0);
            final int slotB = checkSlot(arguments, 1);
            return new Object[]{slotA == slotB || ItemStack.isSameItemSameComponents(container.getItem(slotA), container.getItem(slotB))};
        }

        @Callback(doc = "function(slotA:number, slotB:number[, count:number=64]):boolean -- Move up to the specified number of items from the first specified slot to the second.")
        public Object[] transferStack(final Context context, final Arguments arguments) {
            final int slotA = checkSlot(arguments, 0);
            final int slotB = checkSlot(arguments, 1);
            final int count = Math.max(0, Math.min(arguments.count() > 2 && arguments.checkAny(2) != null ? arguments.checkInteger(2) : 64, container.getMaxStackSize()));
            if (slotA == slotB || count == 0) {
                return new Object[]{true};
            }

            final ItemStack source = container.getItem(slotA);
            final ItemStack target = container.getItem(slotB);
            if (source.isEmpty()) {
                return new Object[]{false};
            }
            if (target.isEmpty()) {
                container.setItem(slotB, container.removeItem(slotA, count));
                container.setChanged();
                return new Object[]{true};
            }
            if (ItemStack.isSameItemSameComponents(source, target)) {
                final int space = Math.min(container.getMaxStackSize(), target.getMaxStackSize()) - target.getCount();
                final int amount = Math.min(count, Math.min(space, source.getCount()));
                if (amount > 0) {
                    source.shrink(amount);
                    target.grow(amount);
                    if (source.isEmpty()) {
                        container.setItem(slotA, ItemStack.EMPTY);
                    }
                    container.setChanged();
                    return new Object[]{true};
                }
            } else if (count >= source.getCount()) {
                container.setItem(slotB, source);
                container.setItem(slotA, target);
                container.setChanged();
                return new Object[]{true};
            }
            return new Object[]{false};
        }

        @Callback(doc = "function(slot:number):table -- Get the raw item stack in the specified slot.")
        public Object[] getStackInSlot(final Context context, final Arguments arguments) {
            return new Object[]{container.getItem(checkSlot(arguments, 0))};
        }

        @Callback(doc = "function():table -- Get a list of raw item stacks for all slots in this inventory.")
        public Object[] getAllStacks(final Context context, final Arguments arguments) {
            final ItemStack[] stacks = new ItemStack[container.getContainerSize()];
            for (int slot = 0; slot < stacks.length; slot++) {
                stacks[slot] = container.getItem(slot).copy();
            }
            return new Object[]{stacks};
        }

        private int checkSlot(final Arguments arguments, final int index) {
            final int slot = arguments.checkInteger(index) - 1;
            if (slot < 0 || slot >= container.getContainerSize()) {
                throw new IllegalArgumentException("slot index out of bounds");
            }
            return slot;
        }
    }
}
