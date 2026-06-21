package li.cil.oc.common.blockentity;

import li.cil.oc.api.Driver;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.component.TerminalServerRackMountableEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import li.cil.oc.common.menu.RackMenu;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class RackBlockEntity extends BlockEntity implements Rack, MenuProvider, Analyzable {
    public static final int CONTAINER_SIZE = 4;
    public static final String DATA_TAG = "oc:rack";

    private static final String TAG_MOUNTABLE_DATA = "oc:mountableData";
    private static final String STACK_MOUNTABLE_DATA_TAG = "oc:rackMountable";

    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private final CompoundTag[] mountableData = new CompoundTag[CONTAINER_SIZE];
    private final RackMountable[] mountables = new RackMountable[CONTAINER_SIZE];

    public RackBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.RACK.get(), pos, blockState);
        OpenComputersApi.initialize();
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            mountableData[slot] = new CompoundTag();
        }
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final RackBlockEntity rack) {
        rack.tickServer();
    }

    public static boolean acceptsDriverSlot(final String slot) {
        return Slot.RackMountable.equals(slot);
    }

    public List<ItemStack> stacksForDrop() {
        saveMountableData();
        final List<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            final ItemStack stack = items.get(slot);
            if (!stack.isEmpty()) {
                stacks.add(stackWithMountableData(slot, stack.copy()));
            }
        }
        return stacks;
    }

    public void saveToStack(final ItemStack stack, final HolderLookup.Provider registries) {
        final CompoundTag data = new CompoundTag();
        saveRackData(data, registries);
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        final CompoundTag root = customData == null ? new CompoundTag() : customData.copyTag();
        root.put(DATA_TAG, data);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    public void loadFromStack(final ItemStack stack, final HolderLookup.Provider registries) {
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return;
        }
        final CompoundTag data = customData.copyTag().getCompound(DATA_TAG);
        if (data.isEmpty()) {
            return;
        }
        loadRackData(data, registries);
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.neoopencomputers.rack");
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory playerInventory, final Player player) {
        return new RackMenu(containerId, playerInventory, this);
    }

    @Override
    public int indexOfMountable(final RackMountable mountable) {
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            if (mountables[slot] == mountable) {
                return slot;
            }
        }
        return -1;
    }

    @Override
    public RackMountable getMountable(final int slot) {
        return isValidSlot(slot) ? mountables[slot] : null;
    }

    @Override
    public CompoundTag getMountableData(final int slot) {
        return isValidSlot(slot) ? mountableData[slot] : new CompoundTag();
    }

    @Override
    public void markChanged(final int slot) {
        if (isValidSlot(slot)) {
            saveMountableData(slot);
            setChanged();
        }
    }

    @Override
    public Node sidedNode(final Direction side) {
        return null;
    }

    @Override
    public boolean canConnect(final Direction side) {
        return false;
    }

    @Override
    public Node[] onAnalyze(final Player player, final Direction side, final float hitX, final float hitY, final float hitZ) {
        final LinkedHashSet<Node> nodes = new LinkedHashSet<>();
        for (final RackMountable mountable : mountables) {
            if (mountable instanceof Analyzable analyzable) {
                final Node[] analyzedNodes = analyzable.onAnalyze(player, side, hitX, hitY, hitZ);
                if (analyzedNodes != null) {
                    for (final Node node : analyzedNodes) {
                        if (node != null) {
                            nodes.add(node);
                        }
                    }
                }
                continue;
            }
            if (mountable == null || mountable.node() == null) {
                continue;
            }
            nodes.add(mountable.node());
            for (final Node neighbor : mountable.node().neighbors()) {
                nodes.add(neighbor);
            }
        }
        return nodes.toArray(Node[]::new);
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

    @Override
    public Direction facing() {
        if (getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)) {
            return getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        }
        return Direction.NORTH;
    }

    @Override
    public Direction toGlobal(final Direction value) {
        return ComputerCaseBlockEntity.toGlobal(facing(), value);
    }

    @Override
    public Direction toLocal(final Direction value) {
        return ComputerCaseBlockEntity.toLocal(facing(), value);
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (final ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(final int slot) {
        return isValidSlot(slot) ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(final int slot, final int amount) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        saveMountableData(slot);
        final ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            writeMountableData(removed, mountableData[slot]);
            removeMountable(slot);
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        saveMountableData(slot);
        final ItemStack removed = ContainerHelper.takeItem(items, slot);
        if (!removed.isEmpty()) {
            writeMountableData(removed, mountableData[slot]);
            removeMountable(slot);
        }
        return removed;
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        if (!isValidSlot(slot) || (!stack.isEmpty() && !canPlaceItem(slot, stack))) {
            return;
        }
        removeMountable(slot);
        final ItemStack stored = stack.copy();
        if (!stored.isEmpty() && stored.getCount() > getMaxStackSize()) {
            stored.setCount(getMaxStackSize());
        }
        items.set(slot, stored);
        mountableData[slot] = readMountableData(stored);
        refreshMountable(slot);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        return isValidSlot(slot) && isRackMountableStack(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(final Player player) {
        return !isRemoved();
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            removeMountable(slot);
            items.set(slot, ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadRackData(tag, registries);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveRackData(tag, registries);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        removeMountables();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        removeMountables();
    }

    private static boolean isValidSlot(final int slot) {
        return slot >= 0 && slot < CONTAINER_SIZE;
    }

    private static boolean isRackMountableStack(final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        final DriverItem driver = Driver.driverFor(stack);
        return driver != null && acceptsDriverSlot(driver.slot(stack));
    }

    private ItemStack stackWithMountableData(final int slot, final ItemStack stack) {
        if (isValidSlot(slot)) {
            writeMountableData(stack, mountableData[slot]);
        }
        return stack;
    }

    private static CompoundTag readMountableData(final ItemStack stack) {
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return new CompoundTag();
        }
        return customData.copyTag().getCompound(STACK_MOUNTABLE_DATA_TAG).copy();
    }

    private static void writeMountableData(final ItemStack stack, final CompoundTag data) {
        if (stack.isEmpty() || data == null || data.isEmpty()) {
            return;
        }
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        final CompoundTag root = customData == null ? new CompoundTag() : customData.copyTag();
        root.put(STACK_MOUNTABLE_DATA_TAG, data.copy());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    private void loadRackData(final CompoundTag tag, final HolderLookup.Provider registries) {
        removeMountables();
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            items.set(slot, ItemStack.EMPTY);
            mountableData[slot] = new CompoundTag();
        }
        ContainerHelper.loadAllItems(tag, items, registries);
        if (tag.contains(TAG_MOUNTABLE_DATA)) {
            final ListTag data = tag.getList(TAG_MOUNTABLE_DATA, CompoundTag.TAG_COMPOUND);
            for (int slot = 0; slot < Math.min(data.size(), CONTAINER_SIZE); slot++) {
                mountableData[slot] = data.getCompound(slot).copy();
            }
        }
        refreshMountables();
    }

    private void saveRackData(final CompoundTag tag, final HolderLookup.Provider registries) {
        saveMountableData();
        ContainerHelper.saveAllItems(tag, items, registries);
        final ListTag data = new ListTag();
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            data.add(mountableData[slot] == null ? new CompoundTag() : mountableData[slot].copy());
        }
        tag.put(TAG_MOUNTABLE_DATA, data);
    }

    private void tickServer() {
        for (final RackMountable mountable : mountables) {
            if (mountable != null && mountable.canUpdate()) {
                mountable.update();
            }
        }
    }

    private void refreshMountables() {
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            refreshMountable(slot);
        }
    }

    private void refreshMountable(final int slot) {
        removeMountable(slot);
        final ItemStack stack = items.get(slot);
        final DriverItem driver = Driver.driverFor(stack);
        if (driver == null || !acceptsDriverSlot(driver.slot(stack))) {
            return;
        }
        final ManagedEnvironment environment = driver.createEnvironment(stack, this);
        if (environment instanceof RackMountable rackMountable) {
            rackMountable.load(mountableData[slot]);
            mountables[slot] = rackMountable;
        }
    }

    private void removeMountables() {
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            removeMountable(slot);
        }
    }

    private void removeMountable(final int slot) {
        if (!isValidSlot(slot)) {
            return;
        }
        saveMountableData(slot);
        final RackMountable mountable = mountables[slot];
        if (mountable instanceof TerminalServerRackMountableEnvironment terminalServer) {
            terminalServer.removeVirtualNodes();
        }
        if (mountable != null && mountable.node() != null) {
            mountable.node().remove();
        }
        mountables[slot] = null;
    }

    private void saveMountableData() {
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            saveMountableData(slot);
        }
    }

    private void saveMountableData(final int slot) {
        if (!isValidSlot(slot) || mountables[slot] == null) {
            return;
        }
        final CompoundTag data = mountables[slot].getData();
        mountables[slot].save(data);
        mountableData[slot] = data;
    }
}
