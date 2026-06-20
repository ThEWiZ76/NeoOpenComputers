package li.cil.oc.common.blockentity;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DeviceInfo;
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
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import li.cil.oc.common.menu.ComputerCaseMenu;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class ComputerCaseBlockEntity extends BlockEntity implements Case, MenuProvider, DeviceInfo {
    public static final int SLOT_CARD_0 = 0;
    public static final int SLOT_CARD_1 = 1;
    public static final int SLOT_MEMORY_0 = 2;
    public static final int SLOT_HDD = 3;
    public static final int SLOT_CPU = 4;
    public static final int SLOT_MEMORY_1 = 5;
    public static final int SLOT_EEPROM = 6;
    public static final int CONTAINER_SIZE = 7;

    private static final String TAG_COLOR = "oc:color";
    private static final String TAG_MACHINE = "oc:machine";
    private static final String SLOT_TYPE_EEPROM = "eeprom";

    private final Machine machine;
    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private final Map<String, Integer> componentSlots = new HashMap<>();
    private int pendingComponentSlot = -1;
    private int color;

    public ComputerCaseBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.COMPUTER_CASE.get(), pos, blockState);
        OpenComputersApi.initialize();
        machine = li.cil.oc.api.Machine.create(this);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final ComputerCaseBlockEntity blockEntity) {
        blockEntity.tickServer();
    }

    public boolean toggleMachine() {
        if (machine.isRunning() || machine.isPaused()) {
            return machine.stop();
        }
        if (!canStartMachine()) {
            machine.crash("missing required components");
            return false;
        }
        return machine.start();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.neoopencomputers.computer_case_tier1");
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory playerInventory, final Player player) {
        return new ComputerCaseMenu(containerId, playerInventory, this);
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
    public Map<String, String> getDeviceInfo() {
        return deviceInfo(getContainerSize());
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
        return () -> new Iterator<>() {
            private int nextSlot = nextComponentSlot(items, 0);

            @Override
            public boolean hasNext() {
                return nextSlot >= 0;
            }

            @Override
            public ItemStack next() {
                if (nextSlot < 0) {
                    throw new NoSuchElementException();
                }
                final int slot = nextSlot;
                nextSlot = nextComponentSlot(items, slot + 1);
                pendingComponentSlot = slot;
                return items.get(slot);
            }
        };
    }

    @Override
    public int componentSlot(final String address) {
        return componentSlot(componentSlots, address);
    }

    @Override
    public void onMachineConnect(final Node node) {
        recordComponentSlot(componentSlots, node, pendingComponentSlot);
        pendingComponentSlot = -1;
    }

    @Override
    public void onMachineDisconnect(final Node node) {
        removeComponentSlot(componentSlots, node);
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
            case SLOT_CARD_0, SLOT_CARD_1 -> Slot.Card;
            case SLOT_CPU -> Slot.CPU;
            case SLOT_MEMORY_0, SLOT_MEMORY_1 -> Slot.Memory;
            case SLOT_HDD -> Slot.HDD;
            case SLOT_EEPROM -> SLOT_TYPE_EEPROM;
            default -> Slot.None;
        };
    }

    static boolean hasRequiredComponents(final String cpuSlot, final String memorySlot0, final String memorySlot1, final String hddSlot, final String eepromSlot) {
        return Slot.CPU.equals(cpuSlot)
            && (Slot.Memory.equals(memorySlot0) || Slot.Memory.equals(memorySlot1))
            && SLOT_TYPE_EEPROM.equals(eepromSlot);
    }

    static void tickHostedMachine(final Machine machine) {
        if (machine.canUpdate()) {
            machine.update();
        }
    }

    static void notifyHardwareChanged(final Machine machine) {
        machine.onHostChanged();
    }

    static <T> void fillExistingSlots(final List<T> items, final T value) {
        for (int slot = 0; slot < items.size(); slot++) {
            items.set(slot, value);
        }
    }

    static int componentSlot(final Map<String, Integer> slots, final String address) {
        if (address == null) {
            return -1;
        }
        return slots.getOrDefault(address, -1);
    }

    static void recordComponentSlot(final Map<String, Integer> slots, final Node node, final int slot) {
        if (node != null && node.address() != null && slot >= 0) {
            slots.put(node.address(), slot);
        }
    }

    static void removeComponentSlot(final Map<String, Integer> slots, final Node node) {
        if (node != null && node.address() != null) {
            slots.remove(node.address());
        }
    }

    static Map<String, String> deviceInfo(final int capacity) {
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.System,
            DeviceInfo.DeviceAttribute.Description, "Computer",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
            DeviceInfo.DeviceAttribute.Product, "Blocker",
            DeviceInfo.DeviceAttribute.Capacity, Integer.toString(Math.max(0, capacity))
        );
    }

    private static int nextComponentSlot(final List<ItemStack> items, final int start) {
        for (int slot = start; slot < items.size(); slot++) {
            if (!items.get(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
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
            notifyHardwareChanged(machine);
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        final ItemStack removed = ContainerHelper.takeItem(items, slot);
        if (!removed.isEmpty()) {
            notifyHardwareChanged(machine);
        }
        return removed;
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
        notifyHardwareChanged(machine);
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
        fillExistingSlots(items, ItemStack.EMPTY);
        setChanged();
        notifyHardwareChanged(machine);
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
        notifyHardwareChanged(machine);
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
        tickHostedMachine(machine);
    }

    private boolean canStartMachine() {
        return hasRequiredComponents(
            driverSlotType(items.get(SLOT_CPU)),
            driverSlotType(items.get(SLOT_MEMORY_0)),
            driverSlotType(items.get(SLOT_MEMORY_1)),
            driverSlotType(items.get(SLOT_HDD)),
            driverSlotType(items.get(SLOT_EEPROM)));
    }

    private void removeMachineNode() {
        if (machine.node() != null) {
            machine.node().remove();
        }
    }

    private static String driverSlotType(final ItemStack stack) {
        final DriverItem driver = Driver.driverFor(stack);
        return driver == null ? Slot.None : driver.slot(stack);
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
