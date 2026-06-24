package li.cil.oc.common.blockentity;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Database;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.ItemStackArrayValue;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.util.FluidDescriptions;
import li.cil.oc.common.util.InventoryComparison;
import li.cil.oc.common.util.TransposerTransfers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

import java.util.Map;

public class TransposerBlockEntity extends BlockEntity implements Environment, EnvironmentHost, DeviceInfo {
    private static final String TAG_NODE = "node";
    private static final String COMPONENT_NAME = "transposer";
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
        DeviceInfo.DeviceAttribute.Description, "Transposer",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
        DeviceInfo.DeviceAttribute.Product, "TP4k-iX"
    );

    private Node node;

    public TransposerBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.TRANSPOSER.get(), pos, blockState);
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

    @Override
    public Level world() {
        return getLevel();
    }

    @Override
    public double xPosition() {
        return getBlockPos().getX() + 0.5D;
    }

    @Override
    public double yPosition() {
        return getBlockPos().getY() + 0.5D;
    }

    @Override
    public double zPosition() {
        return getBlockPos().getZ() + 0.5D;
    }

    @Override
    public void markChanged() {
        setChanged();
    }

    @Callback(doc = "function(side:number):number -- Get the number of slots in the inventory on the specified side.")
    public Object[] getInventorySize(final Context context, final Arguments args) {
        return new Object[]{container(args.checkInteger(0)).getContainerSize()};
    }

    @Callback(doc = "function(side:number, slot:number):number -- Get the stack size in the specified slot.")
    public Object[] getSlotStackSize(final Context context, final Arguments args) {
        final Container container = container(args.checkInteger(0));
        final ItemStack stack = container.getItem(checkSlot(container, args.checkInteger(1)));
        return new Object[]{stack.isEmpty() ? 0 : stack.getCount()};
    }

    @Callback(doc = "function(side:number, slot:number):number -- Get the maximum stack size in the specified slot.")
    public Object[] getSlotMaxStackSize(final Context context, final Arguments args) {
        final Container container = container(args.checkInteger(0));
        final ItemStack stack = container.getItem(checkSlot(container, args.checkInteger(1)));
        return new Object[]{stack.isEmpty() ? 0 : Math.min(container.getMaxStackSize(), stack.getMaxStackSize())};
    }

    @Callback(doc = "function(side:number, slotA:number, slotB:number[, checkNBT:boolean=false]):boolean -- Compare two stacks.")
    public Object[] compareStacks(final Context context, final Arguments args) {
        final Container container = container(args.checkInteger(0));
        final int slotA = checkSlot(container, args.checkInteger(1));
        final int slotB = checkSlot(container, args.checkInteger(2));
        return new Object[]{slotA == slotB || InventoryComparison.sameItem(container.getItem(slotA), container.getItem(slotB), args.optBoolean(3, false))};
    }

    @Callback(doc = "function(side:number, slot:number, dbAddress:string, dbSlot:number[, checkNBT:boolean=false]):boolean -- Compare an item in the specified inventory slot with one in the database with the specified address.")
    public Object[] compareStackToDatabase(final Context context, final Arguments args) {
        final Container container = container(args.checkInteger(0));
        final int slot = checkSlot(container, args.checkInteger(1));
        final Database database = database(args.checkString(2));
        final int databaseSlot = checkSlot(database, args.checkInteger(3));
        return new Object[]{InventoryComparison.sameItem(container.getItem(slot), database.getStackInSlot(databaseSlot), args.optBoolean(4, false))};
    }

    @Callback(doc = "function(side:number, slotA:number, slotB:number):boolean -- Check whether two stacks share an item tag.")
    public Object[] areStacksEquivalent(final Context context, final Arguments args) {
        final Container container = container(args.checkInteger(0));
        final int slotA = checkSlot(container, args.checkInteger(1));
        final int slotB = checkSlot(container, args.checkInteger(2));
        return new Object[]{areEquivalent(container.getItem(slotA), container.getItem(slotB))};
    }

    @Callback(doc = "function(side:number, slot:number):table -- Get the raw item stack in the specified slot.")
    public Object[] getStackInSlot(final Context context, final Arguments args) {
        final Container container = container(args.checkInteger(0));
        return new Object[]{container.getItem(checkSlot(container, args.checkInteger(1)))};
    }

    @Callback(doc = "function(side:number):table -- Get raw item stacks for all slots in the specified inventory.")
    public Object[] getAllStacks(final Context context, final Arguments args) {
        final Container container = container(args.checkInteger(0));
        final ItemStack[] stacks = new ItemStack[container.getContainerSize()];
        for (int slot = 0; slot < stacks.length; slot++) {
            stacks[slot] = container.getItem(slot).copy();
        }
        return new Object[]{new ItemStackArrayValue(stacks)};
    }

    @Callback(doc = "function(side:number):string -- Get the registry name of the inventory block on the specified side.")
    public Object[] getInventoryName(final Context context, final Arguments args) {
        final BlockEntity blockEntity = blockEntity(args.checkInteger(0));
        final var key = BuiltInRegistries.BLOCK.getKey(blockEntity.getBlockState().getBlock());
        return new Object[]{key == null ? "unknown" : key.toString()};
    }

    @Callback(doc = "function(side:number, slot:number, dbAddress:string, dbSlot:number):boolean -- Store an item stack description in the specified database slot.")
    public Object[] store(final Context context, final Arguments args) {
        final Container container = container(args.checkInteger(0));
        final ItemStack stack = container.getItem(checkSlot(container, args.checkInteger(1)));
        final Database database = database(args.checkString(2));
        final int databaseSlot = checkSlot(database, args.checkInteger(3));
        final boolean overwritten = !database.getStackInSlot(databaseSlot).isEmpty();
        database.setStackInSlot(databaseSlot, stack.copy());
        return new Object[]{overwritten};
    }

    @Callback(doc = "function(sourceSide:number, sinkSide:number[, count:number[, sourceSlot:number[, sinkSlot:number]]]):boolean -- Transfer items between adjacent inventories.")
    public Object[] transferItem(final Context context, final Arguments args) {
        final Container source = container(args.checkInteger(0));
        final Container sink = container(args.checkInteger(1));
        final int count = Math.max(0, Math.min(args.optInteger(2, 64), source.getMaxStackSize()));
        if (count == 0) {
            return new Object[]{false};
        }
        if (!consumeTransferEnergy()) {
            return noEnergy();
        }
        if (args.count() > 3) {
            final int sourceSlot = checkSlot(source, args.checkInteger(3));
            final int sinkSlot = args.count() > 4 && args.checkAny(4) != null ? checkSlot(sink, args.checkInteger(4)) : -1;
            return new Object[]{transferFromSlot(source, sink, sourceSlot, sinkSlot, count)};
        }
        for (int sourceSlot = 0; sourceSlot < source.getContainerSize(); sourceSlot++) {
            if (transferFromSlot(source, sink, sourceSlot, -1, count)) {
                return new Object[]{true};
            }
        }
        return new Object[]{false};
    }

    @Callback(doc = "function(side:number):number -- Get the number of tanks exposed by the fluid handler on the specified side.")
    public Object[] getTankCount(final Context context, final Arguments args) {
        return new Object[]{fluidHandler(args.checkInteger(0)).getTanks()};
    }

    @Callback(doc = "function(side:number[, tank:number]):number -- Get the amount of fluid in the specified tank, or the total amount in all tanks.")
    public Object[] getTankLevel(final Context context, final Arguments args) {
        final IFluidHandler handler = fluidHandler(args.checkInteger(0));
        if (args.count() > 1 && args.checkAny(1) != null) {
            final FluidStack stack = handler.getFluidInTank(checkTank(handler, args.checkInteger(1)));
            return new Object[]{stack.isEmpty() ? 0 : stack.getAmount()};
        }
        int amount = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            final FluidStack stack = handler.getFluidInTank(tank);
            if (!stack.isEmpty()) {
                amount += stack.getAmount();
            }
        }
        return new Object[]{amount};
    }

    @Callback(doc = "function(side:number[, tank:number]):number -- Get the capacity of the specified tank, or the maximum capacity on the side.")
    public Object[] getTankCapacity(final Context context, final Arguments args) {
        final IFluidHandler handler = fluidHandler(args.checkInteger(0));
        if (args.count() > 1 && args.checkAny(1) != null) {
            return new Object[]{handler.getTankCapacity(checkTank(handler, args.checkInteger(1)))};
        }
        int capacity = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            capacity = Math.max(capacity, handler.getTankCapacity(tank));
        }
        return new Object[]{capacity};
    }

    @Callback(doc = "function(side:number[, tank:number]):table -- Get fluid info for the specified tank, or all tanks on the side.")
    public Object[] getFluidInTank(final Context context, final Arguments args) {
        final IFluidHandler handler = fluidHandler(args.checkInteger(0));
        if (args.count() > 1 && args.checkAny(1) != null) {
            return FluidDescriptions.describe(handler, checkTank(handler, args.checkInteger(1)));
        }
        return new Object[]{FluidDescriptions.describeAll(handler)};
    }

    @Callback(doc = "function(sourceSide:number, sinkSide:number[, count:number[, sourceTank:number]]):boolean, number -- Transfer fluid between adjacent tanks.")
    public Object[] transferFluid(final Context context, final Arguments args) {
        final IFluidHandler source = fluidHandler(args.checkInteger(0));
        final IFluidHandler sink = fluidHandler(args.checkInteger(1));
        final int count = Math.max(0, args.optInteger(2, 1000));
        if (count == 0) {
            return new Object[]{false, 0};
        }
        if (!consumeTransferEnergy()) {
            return noEnergy();
        }

        final FluidStack drainable;
        if (args.count() > 3 && args.checkAny(3) != null) {
            final FluidStack selected = source.getFluidInTank(checkTank(source, args.checkInteger(3)));
            if (selected.isEmpty()) {
                return new Object[]{false, 0};
            }
            final FluidStack requested = selected.copy();
            requested.setAmount(Math.min(count, selected.getAmount()));
            drainable = source.drain(requested, FluidAction.SIMULATE);
        } else {
            drainable = source.drain(count, FluidAction.SIMULATE);
        }
        if (drainable.isEmpty()) {
            return new Object[]{false, 0};
        }

        final int fillable = sink.fill(drainable, FluidAction.SIMULATE);
        if (fillable <= 0) {
            return new Object[]{false, 0};
        }

        final FluidStack requested = drainable.copy();
        requested.setAmount(Math.min(drainable.getAmount(), fillable));
        final FluidStack drained = source.drain(requested, FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return new Object[]{false, 0};
        }
        final int filled = sink.fill(drained, FluidAction.EXECUTE);
        if (filled > 0 && context != null) {
            context.pause(TransposerTransfers.pauseSeconds(filled));
        }
        return new Object[]{filled > 0, filled};
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_NODE)) {
            node().load(tag.getCompound(TAG_NODE));
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveNode(tag);
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

    private boolean consumeTransferEnergy() {
        return node() instanceof Connector connector && connector.tryChangeBuffer(-ModSettings.transposerCost());
    }

    private static Object[] noEnergy() {
        return new Object[]{null, "not enough energy"};
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
        if (level == null) {
            throw new IllegalStateException("no world");
        }
        final BlockEntity blockEntity = level.getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(side)));
        if (blockEntity == null) {
            throw new IllegalArgumentException("no inventory");
        }
        return blockEntity;
    }

    private IFluidHandler fluidHandler(final int side) {
        if (side < 0 || side > 5) {
            throw new IllegalArgumentException("invalid side");
        }
        if (level == null) {
            throw new IllegalStateException("no world");
        }
        final Direction direction = Direction.from3DDataValue(side);
        final IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, getBlockPos().relative(direction), direction.getOpposite());
        if (handler != null) {
            return handler;
        }
        throw new IllegalArgumentException("no tank");
    }

    private static boolean transferFromSlot(final Container source, final Container sink, final int sourceSlot, final int sinkSlot, final int count) {
        final ItemStack sourceStack = source.getItem(sourceSlot);
        if (sourceStack.isEmpty()) {
            return false;
        }
        if (sinkSlot >= 0) {
            return insertIntoSlot(source, sink, sourceSlot, sinkSlot, count);
        }
        for (int targetSlot = 0; targetSlot < sink.getContainerSize(); targetSlot++) {
            if (insertIntoSlot(source, sink, sourceSlot, targetSlot, count)) {
                return true;
            }
        }
        return false;
    }

    private static boolean insertIntoSlot(final Container source, final Container sink, final int sourceSlot, final int sinkSlot, final int count) {
        final ItemStack sourceStack = source.getItem(sourceSlot);
        if (sourceStack.isEmpty()) {
            return false;
        }
        final ItemStack targetStack = sink.getItem(sinkSlot);
        final int amount = Math.min(count, sourceStack.getCount());
        if (targetStack.isEmpty()) {
            sink.setItem(sinkSlot, source.removeItem(sourceSlot, amount));
            source.setChanged();
            sink.setChanged();
            return true;
        }
        if (!InventoryComparison.sameItem(sourceStack, targetStack, true)) {
            return false;
        }
        final int space = Math.min(sink.getMaxStackSize(), targetStack.getMaxStackSize()) - targetStack.getCount();
        final int moved = Math.min(space, amount);
        if (moved <= 0) {
            return false;
        }
        sourceStack.shrink(moved);
        targetStack.grow(moved);
        if (sourceStack.isEmpty()) {
            source.setItem(sourceSlot, ItemStack.EMPTY);
        }
        source.setChanged();
        sink.setChanged();
        return true;
    }

    private void saveNode(final CompoundTag tag) {
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
        tag.put(TAG_NODE, nodeTag);
    }

    private static Node createNode(final Environment environment) {
        return Network.newNode(environment, Visibility.Network)
            .withComponent(COMPONENT_NAME, Visibility.Network)
            .withConnector()
            .create();
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

    private static int checkTank(final IFluidHandler handler, final int tank) {
        final int index = tank - 1;
        if (index < 0 || index >= handler.getTanks()) {
            throw new IllegalArgumentException("invalid tank index");
        }
        return index;
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
