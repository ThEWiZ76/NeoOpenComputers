package li.cil.oc.common.blockentity;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.internal.Microcontroller;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.util.StateAware;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.block.MicrocontrollerBlock;
import li.cil.oc.common.component.RedstoneControllerHost;
import li.cil.oc.common.menu.MicrocontrollerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.IMenuProviderExtension;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

public class MicrocontrollerBlockEntity extends BlockEntity implements Microcontroller, Container, MenuProvider, IMenuProviderExtension, DeviceInfo, RedstoneControllerHost, StateAware, Analyzable {
    private static final String SLOT_TYPE_EEPROM = "eeprom";
    private static final int TIER_ANY = Integer.MAX_VALUE;
    private static final int BUNDLED_COLOR_COUNT = 16;
    private static final MicrocontrollerSlot[][] SLOT_LAYOUTS = {
        {
            new MicrocontrollerSlot(Slot.CPU, 0),
            new MicrocontrollerSlot(Slot.Memory, 0),
            new MicrocontrollerSlot(SLOT_TYPE_EEPROM, TIER_ANY),
            new MicrocontrollerSlot(Slot.Card, 0),
            new MicrocontrollerSlot(Slot.Card, 0),
            new MicrocontrollerSlot(Slot.Upgrade, 1)
        },
        {
            new MicrocontrollerSlot(Slot.CPU, 0),
            new MicrocontrollerSlot(Slot.Memory, 0),
            new MicrocontrollerSlot(Slot.Memory, 0),
            new MicrocontrollerSlot(SLOT_TYPE_EEPROM, TIER_ANY),
            new MicrocontrollerSlot(Slot.Card, 1),
            new MicrocontrollerSlot(Slot.Card, 0),
            new MicrocontrollerSlot(Slot.Upgrade, 2)
        },
        {
            new MicrocontrollerSlot(Slot.CPU, 2),
            new MicrocontrollerSlot(Slot.Memory, 2),
            new MicrocontrollerSlot(Slot.Memory, 2),
            new MicrocontrollerSlot(SLOT_TYPE_EEPROM, TIER_ANY),
            new MicrocontrollerSlot(Slot.Card, 2),
            new MicrocontrollerSlot(Slot.Card, 2),
            new MicrocontrollerSlot(Slot.Card, 2),
            new MicrocontrollerSlot(Slot.Upgrade, 2),
            new MicrocontrollerSlot(Slot.Upgrade, 2),
            new MicrocontrollerSlot(Slot.Upgrade, 2),
            new MicrocontrollerSlot(Slot.Upgrade, 2),
            new MicrocontrollerSlot(Slot.Upgrade, 2),
            new MicrocontrollerSlot(Slot.Upgrade, 2),
            new MicrocontrollerSlot(Slot.Upgrade, 2),
            new MicrocontrollerSlot(Slot.Upgrade, 2),
            new MicrocontrollerSlot(Slot.Upgrade, 2)
        }
    };

    private final Machine machine;
    private final NonNullList<ItemStack> items;
    private final Map<String, Integer> componentSlots = new HashMap<>();
    private final int tier;
    private int pendingComponentSlot = -1;
    private volatile boolean pendingServerThreadChangeMark;
    private int wakeThreshold;
    private final int[] redstoneOutputs = new int[6];
    private final int[] redstoneInputs = new int[6];
    private final int[][] bundledRedstoneOutputs = new int[6][BUNDLED_COLOR_COUNT];

    public MicrocontrollerBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.MICROCONTROLLER.get(), pos, blockState);
        tier = tierFromBlockState(blockState);
        items = NonNullList.withSize(slotCount(tier), ItemStack.EMPTY);
        OpenComputersApi.initialize();
        machine = li.cil.oc.api.Machine.create(this);
    }

    public static int slotCount(final int tier) {
        return slotLayout(tier).length;
    }

    public static String slotType(final int tier, final int slot) {
        final MicrocontrollerSlot[] layout = slotLayout(tier);
        return slot >= 0 && slot < layout.length ? layout[slot].type() : Slot.None;
    }

    public static int slotTier(final int tier, final int slot) {
        final MicrocontrollerSlot[] layout = slotLayout(tier);
        return slot >= 0 && slot < layout.length ? layout[slot].tier() : -1;
    }

    public int tier() {
        return tier;
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final MicrocontrollerBlockEntity blockEntity) {
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
        return Component.translatable("gui.neoopencomputers.microcontroller.title");
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory playerInventory, final Player player) {
        return new MicrocontrollerMenu(containerId, playerInventory, this);
    }

    @Override
    public void writeClientSideData(final AbstractContainerMenu menu, final RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(tier);
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
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.System,
            DeviceInfo.DeviceAttribute.Description, "Microcontroller",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
            DeviceInfo.DeviceAttribute.Product, "Microcontroller",
            DeviceInfo.DeviceAttribute.Capacity, Integer.toString(Math.max(0, getContainerSize()))
        );
    }

    @Override
    public EnumSet<StateAware.State> getCurrentState() {
        if (machine != null && machine.isRunning()) {
            return EnumSet.of(StateAware.State.IsWorking);
        }
        return EnumSet.noneOf(StateAware.State.class);
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
    public Node[] onAnalyze(final Player player, final Direction side, final float hitX, final float hitY, final float hitZ) {
        return new Node[]{machine.node()};
    }

    @Override
    public Iterable<ItemStack> internalComponents() {
        return () -> new Iterator<>() {
            private int nextSlot = nextComponentSlot(0);

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
                nextSlot = nextComponentSlot(slot + 1);
                pendingComponentSlot = slot;
                return items.get(slot);
            }
        };
    }

    @Override
    public int componentSlot(final String address) {
        if (address == null) {
            return -1;
        }
        return componentSlots.getOrDefault(address, -1);
    }

    @Override
    public void onMachineConnect(final Node node) {
        if (node != null && node.address() != null && pendingComponentSlot >= 0) {
            componentSlots.put(node.address(), pendingComponentSlot);
        }
        pendingComponentSlot = -1;
    }

    @Override
    public void onMachineDisconnect(final Node node) {
        if (node != null && node.address() != null) {
            componentSlots.remove(node.address());
        }
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
        if (level != null && level.getServer() != null && !level.getServer().isSameThread()) {
            if (!pendingServerThreadChangeMark) {
                pendingServerThreadChangeMark = true;
                level.getServer().execute(this::markChangedOnServerThread);
            }
            return;
        }

        markChangedOnServerThread();
    }

    @Override
    public int redstoneOutput(final Direction direction) {
        synchronized (redstoneOutputs) {
            return redstoneOutputs[direction.get3DDataValue()];
        }
    }

    @Override
    public int redstoneInput(final Direction direction) {
        synchronized (redstoneInputs) {
            return redstoneInputs[direction.get3DDataValue()];
        }
    }

    @Override
    public void setRedstoneOutput(final Direction direction, final int value) {
        final int clampedValue = Math.clamp(value, 0, 15);
        final int index = direction.get3DDataValue();
        synchronized (redstoneOutputs) {
            if (redstoneOutputs[index] == clampedValue) {
                return;
            }
            redstoneOutputs[index] = clampedValue;
        }

        scheduleRedstoneUpdate(direction);
    }

    @Override
    public int bundledRedstoneOutput(final Direction direction, final int color) {
        synchronized (bundledRedstoneOutputs) {
            return bundledRedstoneOutputs[direction.get3DDataValue()][color];
        }
    }

    @Override
    public void setBundledRedstoneOutput(final Direction direction, final int color, final int value) {
        final int clampedValue = Math.clamp(value, 0, 255);
        final int side = direction.get3DDataValue();
        synchronized (bundledRedstoneOutputs) {
            if (bundledRedstoneOutputs[side][color] == clampedValue) {
                return;
            }
            bundledRedstoneOutputs[side][color] = clampedValue;
        }

        scheduleRedstoneUpdate(direction);
    }

    @Override
    public int wakeThreshold() {
        return wakeThreshold;
    }

    @Override
    public void setWakeThreshold(final int value) {
        wakeThreshold = value;
        setChanged();
    }

    @Override
    public Direction facing() {
        final BlockState state = getBlockState();
        if (state.getBlock() instanceof MicrocontrollerBlock && state.hasProperty(MicrocontrollerBlock.FACING)) {
            return state.getValue(MicrocontrollerBlock.FACING);
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
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        final ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
            notifyHardwareChanged(machine);
            notifyItemRemoved(machine, tier, slot);
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        final ItemStack removed = ContainerHelper.takeItem(items, slot);
        if (!removed.isEmpty()) {
            notifyHardwareChanged(machine);
            notifyItemRemoved(machine, tier, slot);
        }
        return removed;
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        if (!isValidSlot(slot)) {
            return;
        }
        final ItemStack previous = items.get(slot).copy();
        items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
        notifyHardwareChanged(machine);
        if (!previous.isEmpty() && !ItemStack.matches(previous, items.get(slot))) {
            notifyItemRemoved(machine, tier, slot);
        }
    }

    @Override
    public boolean stillValid(final Player player) {
        return true;
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        if (!isValidSlot(slot) || stack.isEmpty()) {
            return false;
        }
        final DriverItem driver = Driver.driverFor(stack, getClass());
        return driver != null
            && slotType(tier, slot).equals(driver.slot(stack))
            && driver.tier(stack) <= slotTier(tier, slot);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    private boolean isValidSlot(final int slot) {
        return slot >= 0 && slot < items.size();
    }

    private int nextComponentSlot(final int start) {
        for (int slot = start; slot < items.size(); slot++) {
            if (slotAcceptsStack(tier, slot, items.get(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private void tickServer() {
        updateRedstoneInputs();
        if (machine.canUpdate()) {
            machine.update();
        }
    }

    private boolean canStartMachine() {
        boolean hasCpu = false;
        boolean hasMemory = false;
        boolean hasEeprom = false;
        for (int slot = 0; slot < items.size(); slot++) {
            final String expected = slotType(tier, slot);
            if (!slotAcceptsStack(tier, slot, items.get(slot))) {
                continue;
            }
            if (Slot.CPU.equals(expected)) {
                hasCpu = true;
            } else if (Slot.Memory.equals(expected)) {
                hasMemory = true;
            } else if (SLOT_TYPE_EEPROM.equals(expected)) {
                hasEeprom = true;
            }
        }
        return hasCpu && hasMemory && hasEeprom;
    }

    private void markChangedOnServerThread() {
        pendingServerThreadChangeMark = false;
        super.setChanged();
    }

    private void scheduleRedstoneUpdate(final Direction direction) {
        if (level != null && level.getServer() != null && !level.getServer().isSameThread()) {
            level.getServer().execute(() -> updateRedstoneNeighbors(direction));
            return;
        }
        updateRedstoneNeighbors(direction);
    }

    private void updateRedstoneNeighbors(final Direction direction) {
        markChangedOnServerThread();
        if (level != null) {
            level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
            level.updateNeighborsAt(getBlockPos().relative(direction), getBlockState().getBlock());
        }
    }

    private void updateRedstoneInputs() {
        if (level == null) {
            return;
        }
        final String redstoneAddress = componentAddress("redstone");
        synchronized (redstoneInputs) {
            for (Direction direction : Direction.values()) {
                final int index = direction.get3DDataValue();
                final int oldValue = redstoneInputs[index];
                final int newValue = level.getSignal(worldPosition.relative(direction), direction.getOpposite());
                redstoneInputs[index] = newValue;
                if (redstoneAddress != null && oldValue != newValue) {
                    machine.signal("redstone_changed", redstoneAddress, toLocal(direction).get3DDataValue(), oldValue, newValue);
                    if (oldValue < wakeThreshold && newValue >= wakeThreshold) {
                        machine.start();
                    }
                }
            }
        }
    }

    private String componentAddress(final String componentName) {
        for (Map.Entry<String, String> entry : machine.components().entrySet()) {
            if (componentName.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static void notifyHardwareChanged(final Machine machine) {
        machine.onHostChanged();
    }

    private static void notifyItemRemoved(final Machine machine, final int tier, final int slot) {
        if (Slot.CPU.equals(slotType(tier, slot))) {
            machine.stop();
        }
    }

    private static boolean slotAcceptsStack(final int tier, final int slot, final ItemStack stack) {
        final DriverItem driver = Driver.driverFor(stack, MicrocontrollerBlockEntity.class);
        return driver != null
            && slotType(tier, slot).equals(driver.slot(stack))
            && driver.tier(stack) <= slotTier(tier, slot);
    }

    static Direction toGlobal(final Direction facing, final Direction value) {
        return rotateHorizontal(value, horizontalSteps(facing));
    }

    static Direction toLocal(final Direction facing, final Direction value) {
        return rotateHorizontal(value, (4 - horizontalSteps(facing)) % 4);
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

    private static MicrocontrollerSlot[] slotLayout(final int tier) {
        if (tier <= 0) {
            return SLOT_LAYOUTS[0];
        }
        if (tier == 1) {
            return SLOT_LAYOUTS[1];
        }
        return SLOT_LAYOUTS[2];
    }

    private static int tierFromBlockState(final BlockState blockState) {
        if (blockState != null && blockState.getBlock() instanceof MicrocontrollerBlock microcontrollerBlock) {
            return microcontrollerBlock.tier();
        }
        return 0;
    }

    private record MicrocontrollerSlot(String type, int tier) {
    }
}
