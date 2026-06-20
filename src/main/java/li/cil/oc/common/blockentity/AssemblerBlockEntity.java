package li.cil.oc.common.blockentity;

import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.item.TabletCaseItem;
import li.cil.oc.common.item.TabletItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Map;

public class AssemblerBlockEntity extends BlockEntity implements ManagedEnvironment, SidedEnvironment, EnvironmentHost, Container, DeviceInfo {
    public static final int SLOT_TEMPLATE = 0;
    public static final int SLOT_CONTAINER_START = 1;
    public static final int CONTAINER_SLOT_COUNT = 3;
    public static final int SLOT_UPGRADE_START = 4;
    public static final int UPGRADE_SLOT_COUNT = 9;
    public static final int SLOT_COMPONENT_START = 13;
    public static final int COMPONENT_SLOT_COUNT = 9;
    public static final int CONTAINER_SIZE = 22;

    private static final String COMPONENT_NAME = "assembler";
    private static final String TAG_NODE = "node";
    private static final String TAG_TOTAL_ENERGY = "totalEnergy";
    private static final String TAG_REMAINING_ENERGY = "remainingEnergy";
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
        DeviceInfo.DeviceAttribute.Description, "Assembler",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
        DeviceInfo.DeviceAttribute.Product, "Factorizer R1D1"
    );

    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private Node node;
    private double totalRequiredEnergy;
    private double requiredEnergy;

    public AssemblerBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.ASSEMBLER.get(), pos, blockState);
        OpenComputersApi.initialize();
        node = createNode(this);
    }

    public boolean canAssemble() {
        return !isAssembling() && isTabletCase(items.get(SLOT_TEMPLATE)) && hasCpu() && hasMemory();
    }

    public boolean isAssembling() {
        return requiredEnergy > 0D;
    }

    public double progress() {
        if (totalRequiredEnergy <= 0D) {
            return 100D;
        }
        return (1D - requiredEnergy / totalRequiredEnergy) * 100D;
    }

    public boolean start(final boolean finishImmediately) {
        if (!canAssemble()) {
            return false;
        }

        final ItemStack template = items.get(SLOT_TEMPLATE).copy();
        final ItemStack container = firstStoredStack(SLOT_CONTAINER_START, CONTAINER_SLOT_COUNT);
        final ItemStack[] components = componentStacks();
        clearContent();
        final ItemStack output = ModItems.TABLET.get().assembleFromCase(template, container, components);
        items.set(SLOT_TEMPLATE, output);
        totalRequiredEnergy = finishImmediately ? 0D : 1D;
        requiredEnergy = 0D;
        setChanged();
        return true;
    }

    @Callback(doc = "function():string, number or boolean -- The current state of the assembler.")
    public Object[] status(final Context context, final Arguments arguments) {
        if (isAssembling()) {
            return new Object[]{"busy", progress()};
        }
        return new Object[]{"idle", canAssemble()};
    }

    @Callback(doc = "function():boolean -- Start assembling, if possible.")
    public Object[] start(final Context context, final Arguments arguments) {
        return new Object[]{start(false)};
    }

    public int firstSlotFor(final ItemStack stack) {
        for (int slot = 0; slot < items.size(); slot++) {
            if (items.get(slot).isEmpty() && canPlaceItem(slot, stack)) {
                return slot;
            }
        }
        return -1;
    }

    @Override
    public Node node() {
        if (node == null) {
            node = createNode(this);
        }
        return node;
    }

    @Override
    public Node sidedNode(final Direction side) {
        return canConnect(side) ? node() : null;
    }

    @Override
    public boolean canConnect(final Direction side) {
        return side != Direction.UP;
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
    public boolean canUpdate() {
        return false;
    }

    @Override
    public void update() {
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

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
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
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        if (!isValidSlot(slot) || stack.isEmpty()) {
            return false;
        }
        if (slot == SLOT_TEMPLATE) {
            return isTabletCase(stack);
        }
        final DriverItem driver = Driver.driverFor(stack);
        if (driver == null) {
            return false;
        }
        if (isContainerSlot(slot)) {
            return driver instanceof li.cil.oc.api.driver.item.Container;
        }
        if (isUpgradeSlot(slot)) {
            return Slot.Upgrade.equals(driver.slot(stack));
        }
        if (isComponentSlot(slot)) {
            final String driverSlot = driver.slot(stack);
            return !Slot.Container.equals(driverSlot) && !Slot.Upgrade.equals(driverSlot);
        }
        return false;
    }

    @Override
    public boolean stillValid(final Player player) {
        return !isRemoved();
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < items.size(); slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    public void load(final CompoundTag nbt) {
        if (nbt.contains(TAG_NODE) && node() != null) {
            node().load(nbt.getCompound(TAG_NODE));
        }
        totalRequiredEnergy = nbt.getDouble(TAG_TOTAL_ENERGY);
        requiredEnergy = nbt.getDouble(TAG_REMAINING_ENERGY);
    }

    @Override
    public void save(final CompoundTag nbt) {
        if (node() != null) {
            final CompoundTag nodeTag = new CompoundTag();
            if (node().address() == null) {
                Network.joinNewNetwork(node());
                node().save(nodeTag);
                node().remove();
                node = createNode(this);
            } else {
                node().save(nodeTag);
            }
            nbt.put(TAG_NODE, nodeTag);
        }
        nbt.putDouble(TAG_TOTAL_ENERGY, totalRequiredEnergy);
        nbt.putDouble(TAG_REMAINING_ENERGY, requiredEnergy);
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        load(tag);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        save(tag);
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

    private ItemStack[] componentStacks() {
        final ArrayList<ItemStack> stacks = new ArrayList<>();
        for (int slot = SLOT_COMPONENT_START; slot < SLOT_COMPONENT_START + COMPONENT_SLOT_COUNT; slot++) {
            final ItemStack stack = items.get(slot);
            if (!stack.isEmpty()) {
                stacks.add(stack.copy());
            }
        }
        return stacks.toArray(ItemStack[]::new);
    }

    private ItemStack firstStoredStack(final int start, final int count) {
        for (int slot = start; slot < start + count; slot++) {
            final ItemStack stack = items.get(slot);
            if (!stack.isEmpty()) {
                return stack.copy();
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean hasCpu() {
        return hasDriverSlot(Slot.CPU);
    }

    private boolean hasMemory() {
        return hasDriverSlot(Slot.Memory);
    }

    private boolean hasDriverSlot(final String expectedSlot) {
        for (int slot = SLOT_COMPONENT_START; slot < SLOT_COMPONENT_START + COMPONENT_SLOT_COUNT; slot++) {
            final ItemStack stack = items.get(slot);
            final DriverItem driver = Driver.driverFor(stack);
            if (driver != null && expectedSlot.equals(driver.slot(stack))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTabletCase(final ItemStack stack) {
        return stack.getItem() instanceof TabletCaseItem;
    }

    private static boolean isValidSlot(final int slot) {
        return slot >= 0 && slot < CONTAINER_SIZE;
    }

    private static boolean isContainerSlot(final int slot) {
        return slot >= SLOT_CONTAINER_START && slot < SLOT_CONTAINER_START + CONTAINER_SLOT_COUNT;
    }

    private static boolean isUpgradeSlot(final int slot) {
        return slot >= SLOT_UPGRADE_START && slot < SLOT_UPGRADE_START + UPGRADE_SLOT_COUNT;
    }

    private static boolean isComponentSlot(final int slot) {
        return slot >= SLOT_COMPONENT_START && slot < SLOT_COMPONENT_START + COMPONENT_SLOT_COUNT;
    }

    private static Node createNode(final ManagedEnvironment host) {
        final var builder = Network.newNode(host, Visibility.Network);
        return builder == null ? null : builder.withComponent(COMPONENT_NAME, Visibility.Network).create();
    }

    private void removeNode() {
        if (node != null) {
            node.remove();
        }
    }
}
