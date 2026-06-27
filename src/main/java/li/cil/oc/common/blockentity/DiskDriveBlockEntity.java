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
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.ModSounds;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.block.DiskDriveBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.Connection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import li.cil.oc.common.menu.DiskDriveMenu;

import java.util.Map;

public class DiskDriveBlockEntity extends BlockEntity implements ManagedEnvironment, EnvironmentHost, Container, DeviceInfo, MenuProvider, Analyzable {
    public static final int SLOT_FLOPPY = 0;
    public static final int CONTAINER_SIZE = 1;

    private static final String TAG_NODE = "node";
    private static final String TAG_DISK = "disk";
    private static final String TAG_LAST_ACCESS = "lastAccess";
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Disk,
        DeviceInfo.DeviceAttribute.Description, "Floppy disk drive",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
        DeviceInfo.DeviceAttribute.Product, "Spinner 520p1"
    );

    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private Node node;
    private ManagedEnvironment diskEnvironment;
    private long lastAccess;

    public DiskDriveBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.DISK_DRIVE.get(), pos, blockState);
        OpenComputersApi.initialize();
        node = createNode(this);
    }

    public static boolean acceptsDriverSlot(final String slot) {
        return Slot.Floppy.equals(slot);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.neoopencomputers.disk_drive");
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory playerInventory, final Player player) {
        return new DiskDriveMenu(containerId, playerInventory, this);
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
        if (diskEnvironment != null) {
            connectDiskEnvironment();
        }
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

    @Callback(doc = "function():boolean -- Checks whether some medium is currently in the drive.")
    public Object[] isEmpty(final Context context, final Arguments args) {
        return new Object[]{diskEnvironment == null || diskEnvironment.node() == null};
    }

    @Callback(doc = "function([velocity:number]):boolean -- Eject the currently present medium from the drive.")
    public Object[] eject(final Context context, final Arguments args) {
        final double velocity = Math.max(0D, Math.min(args.optDouble(0, 0D), 1D));
        final ItemStack ejected = removeItem(SLOT_FLOPPY, 1);
        if (ejected.isEmpty()) {
            return new Object[]{false};
        }
        if (level != null && !level.isClientSide) {
            final ItemEntity entity = new ItemEntity(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D, ejected);
            final Direction facing = getBlockState().getValue(DiskDriveBlock.FACING);
            entity.setDeltaMovement(facing.getStepX() * velocity, facing.getStepY() * velocity, facing.getStepZ() * velocity);
            level.addFreshEntity(entity);
            ModSounds.playDiskEject(this);
        }
        return new Object[]{true};
    }

    @Callback(doc = "function():string -- Return the internal floppy disk address.")
    public Object[] media(final Context context, final Arguments args) {
        if (diskEnvironment == null || diskEnvironment.node() == null) {
            return new Object[]{null, "drive is empty"};
        }
        return new Object[]{diskEnvironment.node().address()};
    }

    public long getLastAccess() {
        return lastAccess;
    }

    public boolean recordFileSystemAccess(final Node accessedNode, final long timestamp) {
        if (!filesystemNodeMatches(accessedNode)) {
            return false;
        }
        lastAccess = timestamp;
        syncClientData();
        return true;
    }

    @Override
    public Node[] onAnalyze(final Player player, final Direction side, final float hitX, final float hitY, final float hitZ) {
        return diskEnvironment == null || diskEnvironment.node() == null ? null : new Node[]{diskEnvironment.node()};
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
        return items.get(SLOT_FLOPPY).isEmpty();
    }

    @Override
    public ItemStack getItem(final int slot) {
        return slot == SLOT_FLOPPY ? items.get(SLOT_FLOPPY) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(final int slot, final int amount) {
        final ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
            refreshDiskEnvironment();
            syncClientData();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        final ItemStack removed = ContainerHelper.takeItem(items, slot);
        if (!removed.isEmpty()) {
            refreshDiskEnvironment();
            syncClientData();
        }
        return removed;
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        if (slot != SLOT_FLOPPY) {
            return;
        }
        items.set(SLOT_FLOPPY, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
        refreshDiskEnvironment();
        syncClientData();
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        if (slot != SLOT_FLOPPY) {
            return false;
        }
        final DriverItem driver = Driver.driverFor(stack);
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
        items.set(SLOT_FLOPPY, ItemStack.EMPTY);
        setChanged();
        refreshDiskEnvironment();
        syncClientData();
    }

    @Override
    public void load(final CompoundTag nbt) {
        if (nbt.contains(TAG_NODE) && node() != null) {
            node().load(nbt.getCompound(TAG_NODE));
        }
        if (nbt.contains(TAG_DISK) && diskEnvironment != null) {
            diskEnvironment.load(nbt.getCompound(TAG_DISK));
        }
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
        if (diskEnvironment != null) {
            final CompoundTag diskTag = new CompoundTag();
            diskEnvironment.save(diskTag);
            nbt.put(TAG_DISK, diskTag);
        }
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        load(tag);
        refreshDiskEnvironment();
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        save(tag);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        saveClientData(tag, registries);
        return tag;
    }

    @Override
    public void onDataPacket(final Connection net, final ClientboundBlockEntityDataPacket packet, final HolderLookup.Provider registries) {
        loadClientData(packet.getTag(), registries);
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, final HolderLookup.Provider registries) {
        loadClientData(tag, registries);
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

    private void refreshDiskEnvironment() {
        if (diskEnvironment != null && diskEnvironment.node() != null) {
            diskEnvironment.node().remove();
        }
        diskEnvironment = null;
        final ItemStack stack = items.get(SLOT_FLOPPY);
        final DriverItem driver = Driver.driverFor(stack);
        if (driver == null || !acceptsDriverSlot(driver.slot(stack))) {
            return;
        }
        diskEnvironment = driver.createEnvironment(stack, this);
        connectDiskEnvironment();
    }

    private void syncClientData() {
        if (level == null || level.isClientSide) {
            return;
        }
        final BlockState state = level.getBlockState(worldPosition);
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    private void saveClientData(final CompoundTag tag, final HolderLookup.Provider registries) {
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putLong(TAG_LAST_ACCESS, lastAccess);
    }

    private void loadClientData(final CompoundTag tag, final HolderLookup.Provider registries) {
        ContainerHelper.loadAllItems(tag, items, registries);
        lastAccess = tag.getLong(TAG_LAST_ACCESS);
    }

    private void connectDiskEnvironment() {
        if (node() == null || diskEnvironment == null || diskEnvironment.node() == null) {
            return;
        }
        if (node().network() == null) {
            Network.joinNewNetwork(node());
        }
        if (diskEnvironment.node() instanceof li.cil.oc.api.network.Component component) {
            component.setVisibility(Visibility.Network);
        }
        node().connect(diskEnvironment.node());
    }

    private static Node createNode(final ManagedEnvironment host) {
        final var builder = Network.newNode(host, Visibility.Network);
        return builder == null ? null : builder.withComponent("disk_drive", Visibility.Network).create();
    }

    private boolean filesystemNodeMatches(final Node accessedNode) {
        if (accessedNode == null || diskEnvironment == null || diskEnvironment.node() == null) {
            return false;
        }
        final Node diskNode = diskEnvironment.node();
        if (diskNode == accessedNode) {
            return true;
        }
        return diskNode.address() != null && diskNode.address().equals(accessedNode.address());
    }

    private void removeNodes() {
        if (diskEnvironment != null && diskEnvironment.node() != null) {
            diskEnvironment.node().remove();
        }
        if (node != null) {
            node.remove();
        }
    }
}
