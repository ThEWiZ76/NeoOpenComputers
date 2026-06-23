package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Adapter;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.internal.Database;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.api.prefab.ItemStackArrayValue;
import li.cil.oc.common.util.InventoryComparison;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;

public class InventoryControllerEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String COMPONENT_NAME = "inventory_controller";

    private final EnvironmentHost host;

    public InventoryControllerEnvironment(final EnvironmentHost host) {
        this.host = host;
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            final Visibility visibility = host instanceof Adapter ? Visibility.Network : Visibility.Neighbors;
            setNode(builder.withComponent(COMPONENT_NAME, visibility).create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
            DeviceInfo.DeviceAttribute.Description, "Inventory controller",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
            DeviceInfo.DeviceAttribute.Product, "Item Cataloguer R1"
        );
    }

    @Callback(doc = "function(side:number):number -- Get the number of slots in the inventory on the specified side.")
    public Object[] getInventorySize(final Context context, final Arguments arguments) {
        return new Object[]{container(arguments.checkInteger(0)).getContainerSize()};
    }

    @Callback(doc = "function(side:number):string -- Get the registry name of the inventory on the specified side.")
    public Object[] getInventoryName(final Context context, final Arguments arguments) {
        final BlockEntity blockEntity = blockEntity(arguments.checkInteger(0));
        final var key = BuiltInRegistries.BLOCK.getKey(blockEntity.getBlockState().getBlock());
        return new Object[]{key == null ? "unknown" : key.toString()};
    }

    @Callback(doc = "function(side:number, slot:number):number -- Get the stack size of the item stack in the specified inventory slot.")
    public Object[] getSlotStackSize(final Context context, final Arguments arguments) {
        final Container container = container(arguments.checkInteger(0));
        final ItemStack stack = container.getItem(checkSlot(container, arguments.checkInteger(1)));
        return new Object[]{stack.isEmpty() ? 0 : stack.getCount()};
    }

    @Callback(doc = "function(side:number, slot:number):number -- Get the maximum stack size of the item stack in the specified inventory slot.")
    public Object[] getSlotMaxStackSize(final Context context, final Arguments arguments) {
        final Container container = container(arguments.checkInteger(0));
        final ItemStack stack = container.getItem(checkSlot(container, arguments.checkInteger(1)));
        return new Object[]{stack.isEmpty() ? 0 : Math.min(container.getMaxStackSize(), stack.getMaxStackSize())};
    }

    @Callback(doc = "function(side:number, slotA:number, slotB:number[, checkNBT:boolean=false]):boolean -- Compare two item stacks in the specified inventory.")
    public Object[] compareStacks(final Context context, final Arguments arguments) {
        final Container container = container(arguments.checkInteger(0));
        final int slotA = checkSlot(container, arguments.checkInteger(1));
        final int slotB = checkSlot(container, arguments.checkInteger(2));
        return new Object[]{slotA == slotB || InventoryComparison.sameItem(container.getItem(slotA), container.getItem(slotB), arguments.optBoolean(3, false))};
    }

    @Callback(doc = "function(side:number, slot:number, dbAddress:string, dbSlot:number[, checkNBT:boolean=false]):boolean -- Compare an item in the specified inventory slot with one in the database with the specified address.")
    public Object[] compareStackToDatabase(final Context context, final Arguments arguments) {
        final Container container = container(arguments.checkInteger(0));
        final int slot = checkSlot(container, arguments.checkInteger(1));
        final Database database = database(arguments.checkString(2));
        final int databaseSlot = checkSlot(database, arguments.checkInteger(3));
        return new Object[]{InventoryComparison.sameItem(container.getItem(slot), database.getStackInSlot(databaseSlot), arguments.optBoolean(4, false))};
    }

    @Callback(doc = "function(side:number, slotA:number, slotB:number):boolean -- Check whether two item stacks share an item tag.")
    public Object[] areStacksEquivalent(final Context context, final Arguments arguments) {
        final Container container = container(arguments.checkInteger(0));
        final int slotA = checkSlot(container, arguments.checkInteger(1));
        final int slotB = checkSlot(container, arguments.checkInteger(2));
        return new Object[]{areEquivalent(container.getItem(slotA), container.getItem(slotB))};
    }

    @Callback(doc = "function(side:number, slot:number):table -- Get the raw item stack in the specified inventory slot.")
    public Object[] getStackInSlot(final Context context, final Arguments arguments) {
        final Container container = container(arguments.checkInteger(0));
        return new Object[]{container.getItem(checkSlot(container, arguments.checkInteger(1)))};
    }

    @Callback(doc = "function(side:number):table -- Get raw item stacks for all slots in the specified inventory.")
    public Object[] getAllStacks(final Context context, final Arguments arguments) {
        final Container container = container(arguments.checkInteger(0));
        final ItemStack[] stacks = new ItemStack[container.getContainerSize()];
        for (int slot = 0; slot < stacks.length; slot++) {
            stacks[slot] = container.getItem(slot).copy();
        }
        return new Object[]{new ItemStackArrayValue(stacks)};
    }

    @Callback(doc = "function(side:number, slot:number, dbAddress:string, dbSlot:number):boolean -- Store an item stack description in the specified database slot.")
    public Object[] store(final Context context, final Arguments arguments) {
        final Container container = container(arguments.checkInteger(0));
        final ItemStack stack = container.getItem(checkSlot(container, arguments.checkInteger(1)));
        final Database database = database(arguments.checkString(2));
        final int databaseSlot = checkSlot(database, arguments.checkInteger(3));
        final boolean overwritten = !database.getStackInSlot(databaseSlot).isEmpty();
        database.setStackInSlot(databaseSlot, stack.copy());
        return new Object[]{overwritten};
    }

    private Container container(final int side) {
        final BlockEntity blockEntity = blockEntity(side);
        if (blockEntity instanceof Container container) {
            return container;
        }
        throw new IllegalArgumentException("no inventory");
    }

    private BlockEntity blockEntity(final int side) {
        if (side < 0 || side > 5) {
            throw new IllegalArgumentException("invalid side");
        }
        if (host == null || host.world() == null) {
            throw new IllegalStateException("no world");
        }

        final BlockPos hostPos = BlockPos.containing(host.xPosition(), host.yPosition(), host.zPosition());
        final BlockEntity blockEntity = host.world().getBlockEntity(hostPos.relative(Direction.from3DDataValue(side)));
        if (blockEntity != null) {
            return blockEntity;
        }
        throw new IllegalArgumentException("no inventory");
    }

    private static int checkSlot(final Container container, final int slot) {
        final int index = slot - 1;
        if (index < 0 || index >= container.getContainerSize()) {
            throw new IllegalArgumentException("slot index out of bounds");
        }
        return index;
    }

    private static int checkSlot(final Database database, final int slot) {
        final int index = slot - 1;
        if (index < 0 || index >= database.size()) {
            throw new IllegalArgumentException("slot index out of bounds");
        }
        return index;
    }

    private Database database(final String address) {
        if (node() == null || node().network() == null) {
            throw new IllegalArgumentException("no such component");
        }
        if (!(node().network().node(address) instanceof Component component)) {
            throw new IllegalArgumentException("no such component");
        }
        if (!(component.host() instanceof Database database)) {
            throw new IllegalArgumentException("not a database");
        }
        return database;
    }

    private static boolean areEquivalent(final ItemStack stackA, final ItemStack stackB) {
        if (ItemStack.isSameItemSameComponents(stackA, stackB)) {
            return true;
        }
        if (stackA.isEmpty() || stackB.isEmpty()) {
            return false;
        }
        return stackA.getTags().anyMatch(stackB::is);
    }
}
