package li.cil.oc.common.blockentity;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.internal.Case;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.util.StateAware;
import li.cil.oc.common.ForgeEnergyStorageView;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.ModSounds;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.block.ComputerCaseBlock;
import li.cil.oc.common.component.RedstoneControllerHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.client.extensions.IMenuProviderExtension;

import java.util.HashMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class ComputerCaseBlockEntity extends BlockEntity implements Case, MenuProvider, IMenuProviderExtension, DeviceInfo, RedstoneControllerHost, StateAware, Analyzable {
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
    private static final String TAG_RUNNING = "oc:isRunning";
    private static final String TAG_HAS_ERRORED = "oc:hasErrored";
    private static final String TAG_LAST_FILE_SYSTEM_ACCESS = "oc:lastFileSystemAccess";
    private static final String TAG_REDSTONE_OUTPUTS = "oc:redstoneOutputs";
    private static final String TAG_BUNDLED_REDSTONE_OUTPUTS = "oc:bundledRedstoneOutputs";
    private static final String TAG_WAKE_THRESHOLD = "oc:wakeThreshold";
    private static final String SLOT_TYPE_EEPROM = "eeprom";
    private static final int TIER_ANY = Integer.MAX_VALUE;
    private static final int BUNDLED_COLOR_COUNT = 16;
    private static final double MACHINE_ERROR_MESSAGE_RANGE_SQUARED = 64D * 64D;
    private static final CaseSlot[][] SLOT_LAYOUTS = {
        {
            new CaseSlot(Slot.Card, 0),
            new CaseSlot(Slot.Card, 0),
            new CaseSlot(Slot.Memory, 0),
            new CaseSlot(Slot.HDD, 0),
            new CaseSlot(Slot.CPU, 0),
            new CaseSlot(Slot.Memory, 0),
            new CaseSlot(SLOT_TYPE_EEPROM, TIER_ANY)
        },
        {
            new CaseSlot(Slot.Card, 1),
            new CaseSlot(Slot.Card, 0),
            new CaseSlot(Slot.Memory, 1),
            new CaseSlot(Slot.Memory, 1),
            new CaseSlot(Slot.HDD, 1),
            new CaseSlot(Slot.HDD, 0),
            new CaseSlot(Slot.CPU, 1),
            new CaseSlot(SLOT_TYPE_EEPROM, TIER_ANY)
        },
        {
            new CaseSlot(Slot.Card, 2),
            new CaseSlot(Slot.Card, 1),
            new CaseSlot(Slot.Card, 1),
            new CaseSlot(Slot.Memory, 2),
            new CaseSlot(Slot.Memory, 2),
            new CaseSlot(Slot.HDD, 2),
            new CaseSlot(Slot.HDD, 1),
            new CaseSlot(Slot.Floppy, 0),
            new CaseSlot(Slot.CPU, 2),
            new CaseSlot(SLOT_TYPE_EEPROM, TIER_ANY)
        }
    };

    private final Machine machine;
    private final IEnergyStorage energyStorage = new ForgeEnergyStorageView(this::connectorNode, this::energyThroughput);
    private final NonNullList<ItemStack> items;
    private final Map<String, Integer> componentSlots = new HashMap<>();
    private volatile boolean pendingServerThreadChangeMark;
    private int pendingComponentSlot = -1;
    private int tier;
    private int color;
    private int wakeThreshold;
    private boolean clientRunning;
    private boolean clientErrored;
    private boolean lastSyncedRunning;
    private boolean lastSyncedErrored;
    private long lastFileSystemAccess;
    private final int[] redstoneOutputs = new int[6];
    private final int[] redstoneInputs = new int[6];
    private final int[][] bundledRedstoneOutputs = new int[6][BUNDLED_COLOR_COUNT];

    public ComputerCaseBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.COMPUTER_CASE.get(), pos, blockState);
        tier = tierFromBlockState(blockState);
        items = NonNullList.withSize(slotCount(tier), ItemStack.EMPTY);
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
        return Component.translatable("gui.neoopencomputers.computer_case.title");
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory playerInventory, final Player player) {
        return new ComputerCaseMenu(containerId, playerInventory, this);
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

    public IEnergyStorage energyStorage(final Direction side) {
        return energyStorage;
    }

    public boolean isClientRunning() {
        if (level != null && level.isClientSide) {
            return clientRunning;
        }
        return isMachineRunningForClient();
    }

    public boolean isClientErrored() {
        if (level != null && level.isClientSide) {
            return clientErrored;
        }
        return isMachineErroredForClient();
    }

    public double visualFileSystemActivity() {
        final long elapsed = System.currentTimeMillis() - lastFileSystemAccess;
        if (elapsed < 0L || elapsed >= 400L) {
            return 0D;
        }
        return 1D - elapsed / 400D;
    }

    public boolean recordFileSystemAccess(final Node accessedNode, final long timestamp) {
        lastFileSystemAccess = timestamp;
        syncClientData();
        return true;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo(getContainerSize());
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
            private int nextSlot = nextComponentSlot(items, tier, 0);

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
                nextSlot = nextComponentSlot(items, tier, slot + 1);
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
    public int getColor() {
        return color;
    }

    @Override
    public void setColor(final int value) {
        color = value;
        setChanged();
    }

    public int redstoneOutput(final Direction direction) {
        synchronized (redstoneOutputs) {
            return redstoneOutputs[direction.get3DDataValue()];
        }
    }

    public int redstoneInput(final Direction direction) {
        synchronized (redstoneInputs) {
            return redstoneInputs[direction.get3DDataValue()];
        }
    }

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

    public static String slotType(final int slot) {
        return slotType(0, slot);
    }

    public static int slotCount(final int tier) {
        return slotLayout(tier).length;
    }

    public static String slotType(final int tier, final int slot) {
        final CaseSlot[] layout = slotLayout(tier);
        return slot >= 0 && slot < layout.length ? layout[slot].type() : Slot.None;
    }

    public static int slotTier(final int tier, final int slot) {
        final CaseSlot[] layout = slotLayout(tier);
        return slot >= 0 && slot < layout.length ? layout[slot].tier() : -1;
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

    static void notifyItemRemoved(final Machine machine, final int tier, final int slot) {
        if (Slot.CPU.equals(slotType(tier, slot))) {
            machine.stop();
        }
    }

    static boolean shouldReportMachineError(final boolean wasRunning, final boolean running, final String lastError) {
        return wasRunning && !running && lastError != null && !lastError.isEmpty();
    }

    static Component machineErrorMessage(final String lastError) {
        return Component.literal("Computer error: " + firstErrorLine(lastError));
    }

    public static void activateMachineFromBlockUse(final Machine machine) {
        if (machine != null && !machine.isRunning()) {
            machine.start();
        }
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
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
            DeviceInfo.DeviceAttribute.Product, "Blocker",
            DeviceInfo.DeviceAttribute.Capacity, Integer.toString(Math.max(0, capacity))
        );
    }

    private static String firstErrorLine(final String lastError) {
        int end = lastError.length();
        final int carriageReturn = lastError.indexOf('\r');
        final int lineFeed = lastError.indexOf('\n');
        if (carriageReturn >= 0) {
            end = Math.min(end, carriageReturn);
        }
        if (lineFeed >= 0) {
            end = Math.min(end, lineFeed);
        }
        return lastError.substring(0, end);
    }

    private static int nextComponentSlot(final List<ItemStack> items, final int tier, final int start) {
        for (int slot = start; slot < items.size(); slot++) {
            if (slotAcceptsStack(tier, slot, items.get(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private static int cpuSlot(final int tier) {
        final CaseSlot[] layout = slotLayout(tier);
        for (int slot = 0; slot < layout.length; slot++) {
            if (Slot.CPU.equals(layout[slot].type())) {
                return slot;
            }
        }
        return -1;
    }

    @Override
    public int tier() {
        return tier;
    }

    public double energyThroughput() {
        return ModSettings.caseRate(tier);
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
        return isValidSlotForTier(slot) ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(final int slot, final int amount) {
        if (!isValidSlotForTier(slot)) {
            return ItemStack.EMPTY;
        }
        final ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
            notifyHardwareChanged(machine);
            notifyItemRemoved(machine, tier, slot);
            playDiskRemoveSound(slot);
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        if (!isValidSlotForTier(slot)) {
            return ItemStack.EMPTY;
        }
        final ItemStack removed = ContainerHelper.takeItem(items, slot);
        if (!removed.isEmpty()) {
            notifyHardwareChanged(machine);
            notifyItemRemoved(machine, tier, slot);
            playDiskRemoveSound(slot);
        }
        return removed;
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        if (!isValidSlotForTier(slot)) {
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
        playDiskChangeSound(slot, previous, items.get(slot));
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        return slotAcceptsStack(tier, slot, stack);
    }

    @Override
    public boolean stillValid(final Player player) {
        return !isRemoved();
    }

    @Override
    public void clearContent() {
        final ItemStack previousFloppy = floppyStack().copy();
        final boolean hadCpu = hasCpuStack();
        fillExistingSlots(items, ItemStack.EMPTY);
        setChanged();
        notifyHardwareChanged(machine);
        if (hadCpu) {
            notifyItemRemoved(machine, tier, cpuSlot(tier));
        }
        if (!previousFloppy.isEmpty()) {
            ModSounds.playDiskEject(this);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        stopClientRunningSound();
        removeMachineNode();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        stopClientRunningSound();
        removeMachineNode();
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        color = tag.getInt(TAG_COLOR);
        wakeThreshold = tag.getInt(TAG_WAKE_THRESHOLD);
        loadRedstoneOutputs(tag);
        ContainerHelper.loadAllItems(tag, items, registries);
        notifyHardwareChanged(machine);
        machine.load(tag.getCompound(TAG_MACHINE));
        lastSyncedRunning = isMachineRunningForClient();
        lastSyncedErrored = isMachineErroredForClient();
        loadClientData(tag);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(TAG_COLOR, color);
        tag.putInt(TAG_WAKE_THRESHOLD, wakeThreshold);
        tag.putIntArray(TAG_REDSTONE_OUTPUTS, redstoneOutputs);
        tag.putIntArray(TAG_BUNDLED_REDSTONE_OUTPUTS, saveBundledRedstoneOutputs());
        final CompoundTag machineTag = new CompoundTag();
        machine.save(machineTag);
        tag.put(TAG_MACHINE, machineTag);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        saveClientData(tag);
        return tag;
    }

    @Override
    public void onDataPacket(final Connection net, final ClientboundBlockEntityDataPacket packet, final HolderLookup.Provider registries) {
        loadClientData(packet.getTag());
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, final HolderLookup.Provider registries) {
        loadClientData(tag);
    }

    private void tickServer() {
        updateRedstoneInputs();
        tickHostedMachine(machine);
        syncMachineStateIfChanged();
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

    private void removeMachineNode() {
        if (machine.node() != null) {
            machine.node().remove();
        }
    }

    private ItemStack floppyStack() {
        for (int slot = 0; slot < items.size(); slot++) {
            if (Slot.Floppy.equals(slotType(tier, slot))) {
                return items.get(slot);
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean hasCpuStack() {
        final int slot = cpuSlot(tier);
        return slot >= 0 && slot < items.size() && !items.get(slot).isEmpty();
    }

    private void playDiskRemoveSound(final int slot) {
        if (Slot.Floppy.equals(slotType(tier, slot))) {
            ModSounds.playDiskEject(this);
        }
    }

    private void playDiskChangeSound(final int slot, final ItemStack previous, final ItemStack current) {
        if (!Slot.Floppy.equals(slotType(tier, slot)) || ItemStack.matches(previous, current)) {
            return;
        }
        if (!previous.isEmpty()) {
            ModSounds.playDiskEject(this);
        }
        if (!current.isEmpty()) {
            ModSounds.playDiskInsert(this);
        }
    }

    private void markChangedOnServerThread() {
        pendingServerThreadChangeMark = false;
        super.setChanged();
    }

    private boolean isMachineRunningForClient() {
        return machine != null && (machine.isRunning() || machine.isPaused());
    }

    private boolean isMachineErroredForClient() {
        return machine != null && machine.lastError() != null;
    }

    private void syncMachineStateIfChanged() {
        final boolean wasRunning = lastSyncedRunning;
        final boolean running = isMachineRunningForClient();
        final String lastError = machine == null ? null : machine.lastError();
        final boolean errored = lastError != null;
        if ((running == lastSyncedRunning && errored == lastSyncedErrored) || level == null || level.isClientSide) {
            return;
        }
        if (shouldReportMachineError(wasRunning, running, lastError)) {
            reportMachineError(lastError);
        }
        lastSyncedRunning = running;
        lastSyncedErrored = errored;
        setChanged();
        final BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, 3);
    }

    private void reportMachineError(final String lastError) {
        final Component message = machineErrorMessage(lastError);
        for (final Player player : level.players()) {
            if (player.distanceToSqr(xPosition(), yPosition(), zPosition()) <= MACHINE_ERROR_MESSAGE_RANGE_SQUARED) {
                player.sendSystemMessage(message);
            }
        }
    }

    private void syncClientData() {
        if (level == null || level.isClientSide) {
            return;
        }
        setChanged();
        final BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, 3);
    }

    private void saveClientData(final CompoundTag tag) {
        tag.putBoolean(TAG_RUNNING, isMachineRunningForClient());
        tag.putBoolean(TAG_HAS_ERRORED, isMachineErroredForClient());
        tag.putLong(TAG_LAST_FILE_SYSTEM_ACCESS, lastFileSystemAccess);
    }

    private void loadClientData(final CompoundTag tag) {
        if (!tag.contains(TAG_RUNNING)) {
            return;
        }
        clientRunning = tag.getBoolean(TAG_RUNNING);
        clientErrored = tag.getBoolean(TAG_HAS_ERRORED);
        lastFileSystemAccess = tag.getLong(TAG_LAST_FILE_SYSTEM_ACCESS);
        updateClientRunningSound();
    }

    private void updateClientRunningSound() {
        if (level != null && level.isClientSide) {
            li.cil.oc.client.ComputerCaseSounds.update(this);
        }
    }

    private void stopClientRunningSound() {
        if (level != null && level.isClientSide) {
            clientRunning = false;
            clientErrored = false;
            li.cil.oc.client.ComputerCaseSounds.update(this);
        }
    }

    private void loadRedstoneOutputs(final CompoundTag tag) {
        final int[] savedOutputs = tag.getIntArray(TAG_REDSTONE_OUTPUTS);
        for (int index = 0; index < redstoneOutputs.length; index++) {
            redstoneOutputs[index] = index < savedOutputs.length ? Math.clamp(savedOutputs[index], 0, 15) : 0;
        }
        loadBundledRedstoneOutputs(tag.getIntArray(TAG_BUNDLED_REDSTONE_OUTPUTS));
    }

    private int[] saveBundledRedstoneOutputs() {
        final int[] saved = new int[bundledRedstoneOutputs.length * BUNDLED_COLOR_COUNT];
        synchronized (bundledRedstoneOutputs) {
            for (Direction direction : Direction.values()) {
                final int side = direction.get3DDataValue();
                System.arraycopy(bundledRedstoneOutputs[side], 0, saved, side * BUNDLED_COLOR_COUNT, BUNDLED_COLOR_COUNT);
            }
        }
        return saved;
    }

    private void loadBundledRedstoneOutputs(final int[] saved) {
        synchronized (bundledRedstoneOutputs) {
            for (Direction direction : Direction.values()) {
                final int side = direction.get3DDataValue();
                for (int color = 0; color < BUNDLED_COLOR_COUNT; color++) {
                    final int index = side * BUNDLED_COLOR_COUNT + color;
                    bundledRedstoneOutputs[side][color] = index < saved.length ? Math.clamp(saved[index], 0, 255) : 0;
                }
            }
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

    private static String driverSlotType(final ItemStack stack) {
        final DriverItem driver = Driver.driverFor(stack);
        return driver == null ? Slot.None : driver.slot(stack);
    }

    private static boolean slotAcceptsStack(final int tier, final int slot, final ItemStack stack) {
        final DriverItem driver = Driver.driverFor(stack);
        return driver != null
            && slotType(tier, slot).equals(driver.slot(stack))
            && driver.tier(stack) <= slotTier(tier, slot);
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

    private boolean isValidSlotForTier(final int slot) {
        return slot >= 0 && slot < items.size();
    }

    private Connector connectorNode() {
        return node() instanceof Connector connector ? connector : null;
    }

    private static CaseSlot[] slotLayout(final int tier) {
        return SLOT_LAYOUTS[Math.clamp(tier, 0, SLOT_LAYOUTS.length - 1)];
    }

    private static int tierFromBlockState(final BlockState blockState) {
        if (blockState != null && blockState.getBlock() instanceof ComputerCaseBlock computerCaseBlock) {
            return computerCaseBlock.tier();
        }
        return 0;
    }

    private record CaseSlot(String type, int tier) {
    }
}
