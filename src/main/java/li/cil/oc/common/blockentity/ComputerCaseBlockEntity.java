package li.cil.oc.common.blockentity;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.internal.Case;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.block.ComputerCaseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ComputerCaseBlockEntity extends BlockEntity implements Case {
    public static final int SLOT_CPU = 0;
    public static final int SLOT_MEMORY_0 = 1;
    public static final int SLOT_MEMORY_1 = 2;
    public static final int CONTAINER_SIZE = 3;

    private static final String TAG_COLOR = "oc:color";
    private static final String TAG_MACHINE = "oc:machine";

    private final Machine machine;
    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private int color;

    public ComputerCaseBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.COMPUTER_CASE.get(), pos, blockState);
        OpenComputersApi.initialize();
        machine = li.cil.oc.api.Machine.create(this);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final ComputerCaseBlockEntity blockEntity) {
        blockEntity.tickServer();
    }

    @Override
    public Machine machine() {
        return machine;
    }

    @Override
    public Node node() {
        return machine.node();
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
    public Iterable<ItemStack> internalComponents() {
        return items.stream()
            .filter(stack -> !stack.isEmpty())
            .toList();
    }

    @Override
    public int componentSlot(final String address) {
        return -1;
    }

    @Override
    public void onMachineConnect(final Node node) {
    }

    @Override
    public void onMachineDisconnect(final Node node) {
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
    public int getColor() {
        return color;
    }

    @Override
    public void setColor(final int value) {
        color = value;
        setChanged();
    }

    @Override
    public boolean controlsConnectivity() {
        return false;
    }

    @Override
    public Direction facing() {
        final BlockState state = getBlockState();
        if (state.getBlock() instanceof ComputerCaseBlock && state.hasProperty(ComputerCaseBlock.FACING)) {
            return state.getValue(ComputerCaseBlock.FACING);
        }
        return Direction.NORTH;
    }

    @Override
    public Direction toGlobal(final Direction value) {
        return toGlobal(facing(), value);
    }

    @Override
    public Direction toLocal(final Direction value) {
        return toLocal(facing(), value);
    }

    static Direction toGlobal(final Direction facing, final Direction value) {
        return rotateHorizontal(value, horizontalSteps(facing));
    }

    static Direction toLocal(final Direction facing, final Direction value) {
        return rotateHorizontal(value, (4 - horizontalSteps(facing)) % 4);
    }

    static String slotType(final int slot) {
        return switch (slot) {
            case SLOT_CPU -> Slot.CPU;
            case SLOT_MEMORY_0, SLOT_MEMORY_1 -> Slot.Memory;
            default -> Slot.None;
        };
    }

    @Override
    public int tier() {
        return 0;
    }

    @Override
    public int getContainerSize() {
        return items.size();
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
        final ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        if (!isValidSlot(slot)) {
            return;
        }
        items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        final DriverItem driver = Driver.driverFor(stack);
        return driver != null && slotType(slot).equals(driver.slot(stack));
    }

    @Override
    public boolean stillValid(final Player player) {
        return !isRemoved();
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        removeMachineNode();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        removeMachineNode();
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        color = tag.getInt(TAG_COLOR);
        ContainerHelper.loadAllItems(tag, items, registries);
        machine.load(tag.getCompound(TAG_MACHINE));
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(TAG_COLOR, color);
        ContainerHelper.saveAllItems(tag, items, registries);
        final CompoundTag machineTag = new CompoundTag();
        machine.save(machineTag);
        tag.put(TAG_MACHINE, machineTag);
    }

    private void tickServer() {
    }

    private void removeMachineNode() {
        if (machine.node() != null) {
            machine.node().remove();
        }
    }

    private static Direction rotateHorizontal(final Direction value, final int steps) {
        if (value.getAxis().isVertical()) {
            return value;
        }
        Direction result = value;
        for (int index = 0; index < steps; index++) {
            result = result.getClockWise();
        }
        return result;
    }

    private static int horizontalSteps(final Direction facing) {
        return switch (facing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
    }

    private static boolean isValidSlot(final int slot) {
        return slot >= 0 && slot < CONTAINER_SIZE;
    }
}
