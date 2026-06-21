package li.cil.oc.common.blockentity;

import li.cil.oc.api.Driver;
import li.cil.oc.api.FileSystem;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import li.cil.oc.common.menu.RaidMenu;

import java.util.Map;

public class RaidBlockEntity extends BlockEntity implements ManagedEnvironment, EnvironmentHost, Container, DeviceInfo, MenuProvider, Analyzable {
    public static final int CONTAINER_SIZE = 3;
    public static final String DATA_TAG = "oc:raid";

    private static final String TAG_NODE = "node";
    private static final String TAG_FILESYSTEM = "filesystem";
    private static final String TAG_LABEL = "label";
    private static final long[] HDD_CAPACITIES = {
        1024L * 1024L,
        2048L * 1024L,
        4096L * 1024L
    };
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Disk,
        DeviceInfo.DeviceAttribute.Description, "RAID",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
        DeviceInfo.DeviceAttribute.Product, "OC RAID"
    );

    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private final RaidLabel label = new RaidLabel();
    private Node node;
    private ManagedEnvironment filesystem;

    public RaidBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.RAID.get(), pos, blockState);
        OpenComputersApi.initialize();
        node = createNode(this);
    }

    public static boolean acceptsDriverSlot(final String slot) {
        return Slot.HDD.equals(slot);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.neoopencomputers.raid");
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory playerInventory, final Player player) {
        return new RaidMenu(containerId, playerInventory, this);
    }

    public void saveToStack(final ItemStack stack, final HolderLookup.Provider registries) {
        final CompoundTag data = new CompoundTag();
        ContainerHelper.saveAllItems(data, items, registries);
        save(data);
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
        ContainerHelper.loadAllItems(data, items, registries);
        load(data);
        setChanged();
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
        connectFilesystem();
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
    public Node[] onAnalyze(final Player player, final Direction side, final float hitX, final float hitY, final float hitZ) {
        return filesystem == null || filesystem.node() == null ? null : new Node[]{filesystem.node()};
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
        final ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            refreshFilesystem();
            setChanged();
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
            refreshFilesystem();
        }
        return removed;
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        if (!isValidSlot(slot) || (!stack.isEmpty() && !canPlaceItem(slot, stack))) {
            return;
        }
        final ItemStack stored = stack.copy();
        if (!stored.isEmpty() && stored.getCount() > getMaxStackSize()) {
            stored.setCount(getMaxStackSize());
        }
        items.set(slot, stored);
        refreshFilesystem();
        setChanged();
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        if (!isValidSlot(slot) || stack.isEmpty()) {
            return false;
        }
        final DriverItem driver = Driver.driverFor(stack, getClass());
        return driver != null && acceptsDriverSlot(driver.slot(stack));
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
            items.set(slot, ItemStack.EMPTY);
        }
        refreshFilesystem();
        setChanged();
    }

    @Override
    public void load(final CompoundTag nbt) {
        if (nbt.contains(TAG_NODE) && node() != null) {
            node().load(nbt.getCompound(TAG_NODE));
        }
        label.load(nbt);
        if (nbt.contains(TAG_FILESYSTEM) && isComplete()) {
            createFilesystem();
            if (filesystem != null) {
                filesystem.load(nbt.getCompound(TAG_FILESYSTEM));
            }
        } else {
            refreshFilesystem();
        }
    }

    @Override
    public void save(final CompoundTag nbt) {
        if (node() != null) {
            final CompoundTag nodeTag = new CompoundTag();
            if (node().address() == null) {
                Network.joinNewNetwork(node());
            }
            node().save(nodeTag);
            nbt.put(TAG_NODE, nodeTag);
        }
        label.save(nbt);
        if (filesystem != null) {
            final CompoundTag filesystemTag = new CompoundTag();
            filesystem.save(filesystemTag);
            nbt.put(TAG_FILESYSTEM, filesystemTag);
        }
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
        removeNodes();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        removeNodes();
    }

    private static boolean isValidSlot(final int slot) {
        return slot >= 0 && slot < CONTAINER_SIZE;
    }

    private void refreshFilesystem() {
        removeFilesystem();
        if (isComplete()) {
            createFilesystem();
        }
    }

    private boolean isComplete() {
        for (final ItemStack stack : items) {
            if (stack.isEmpty() || !canPlaceItem(0, stack)) {
                return false;
            }
        }
        return true;
    }

    private void createFilesystem() {
        final li.cil.oc.api.fs.FileSystem fileSystem = FileSystem.fromMemory(totalCapacity());
        filesystem = FileSystem.asManagedEnvironment(fileSystem, label, this, null, 6);
        connectFilesystem();
    }

    private long totalCapacity() {
        long total = 0L;
        for (final ItemStack stack : items) {
            final DriverItem driver = Driver.driverFor(stack, getClass());
            if (driver != null && acceptsDriverSlot(driver.slot(stack))) {
                total += HDD_CAPACITIES[Math.max(0, Math.min(HDD_CAPACITIES.length - 1, driver.tier(stack)))];
            }
        }
        return total;
    }

    private void connectFilesystem() {
        if (node() == null || filesystem == null || filesystem.node() == null) {
            return;
        }
        if (node().network() == null) {
            Network.joinNewNetwork(node());
        }
        node().connect(filesystem.node());
    }

    private void removeFilesystem() {
        if (filesystem != null && filesystem.node() != null) {
            filesystem.node().remove();
        }
        filesystem = null;
    }

    private void removeNodes() {
        removeFilesystem();
        if (node != null) {
            node.remove();
        }
    }

    private static Node createNode(final ManagedEnvironment host) {
        final var builder = Network.newNode(host, Visibility.None);
        return builder == null ? null : builder.create();
    }

    private static final class RaidLabel implements Label {
        private String value = "raid";

        @Override
        public String getLabel() {
            return value;
        }

        @Override
        public void setLabel(final String value) {
            this.value = value == null ? null : value.substring(0, Math.min(16, value.length()));
        }

        @Override
        public void load(final CompoundTag nbt) {
            if (nbt.contains(TAG_LABEL)) {
                value = nbt.getString(TAG_LABEL);
            }
        }

        @Override
        public void save(final CompoundTag nbt) {
            if (value != null) {
                nbt.putString(TAG_LABEL, value);
            }
        }
    }
}
