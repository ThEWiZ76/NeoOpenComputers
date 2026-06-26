package li.cil.oc.common.component;

import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.component.RackBusConnectable;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.api.util.StateAware;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.blockentity.DiskDriveBlockEntity;
import li.cil.oc.common.menu.DiskDriveMenu;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.Map;

public final class DiskDriveMountableEnvironment extends AbstractManagedEnvironment implements RackMountable, EnvironmentHost, Container, DeviceInfo, Analyzable {
    private static final String TAG_KIND = "kind";
    private static final String TAG_DISK = "disk";
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Disk,
        DeviceInfo.DeviceAttribute.Description, "Floppy disk drive",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
        DeviceInfo.DeviceAttribute.Product, "RackDrive 100 Rev. 2"
    );

    private final EnvironmentHost host;
    private final int slot;
    private final NonNullList<ItemStack> items = NonNullList.withSize(DiskDriveBlockEntity.CONTAINER_SIZE, ItemStack.EMPTY);
    private ManagedEnvironment diskEnvironment;

    public DiskDriveMountableEnvironment(final EnvironmentHost host, final int slot) {
        OpenComputersApi.initialize();
        this.host = host;
        this.slot = slot;
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.withComponent("disk_drive", Visibility.Network).create());
        }
    }

    @Override
    public CompoundTag getData() {
        final CompoundTag data = new CompoundTag();
        data.putString(TAG_KIND, "disk_drive_mountable");
        return data;
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
    public Level world() {
        return host == null ? null : host.world();
    }

    @Override
    public double xPosition() {
        return host == null ? 0D : host.xPosition();
    }

    @Override
    public double yPosition() {
        return host == null ? 0D : host.yPosition();
    }

    @Override
    public double zPosition() {
        return host == null ? 0D : host.zPosition();
    }

    @Override
    public void markChanged() {
        if (host != null) {
            host.markChanged();
        }
    }

    @Override
    public int getConnectableCount() {
        return 0;
    }

    @Override
    public RackBusConnectable getConnectableAt(final int index) {
        return null;
    }

    @Override
    public boolean onActivate(final Player player, final InteractionHand hand, final ItemStack heldItem, final float hitX, final float hitY) {
        if (player == null) {
            return false;
        }
        player.openMenu(new SimpleMenuProvider(
            (containerId, playerInventory, menuPlayer) -> new DiskDriveMenu(containerId, playerInventory, this),
            net.minecraft.network.chat.Component.translatable("item.neoopencomputers.disk_drive_mountable")));
        return true;
    }

    @Override
    public EnumSet<StateAware.State> getCurrentState() {
        return EnumSet.noneOf(StateAware.State.class);
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
        final ItemStack ejected = removeItem(DiskDriveBlockEntity.SLOT_FLOPPY, 1);
        if (ejected.isEmpty()) {
            return new Object[]{false};
        }
        final Level level = world();
        if (level != null && !level.isClientSide) {
            level.addFreshEntity(new ItemEntity(level, xPosition(), yPosition(), zPosition(), ejected));
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

    @Override
    public Node[] onAnalyze(final Player player, final Direction side, final float hitX, final float hitY, final float hitZ) {
        return diskEnvironment == null || diskEnvironment.node() == null ? null : new Node[]{diskEnvironment.node()};
    }

    @Override
    public void load(final CompoundTag nbt) {
        super.load(nbt);
        ContainerHelper.loadAllItems(nbt, items, world() == null ? null : world().registryAccess());
        refreshDiskEnvironment();
        if (nbt.contains(TAG_DISK) && diskEnvironment != null) {
            diskEnvironment.load(nbt.getCompound(TAG_DISK));
            connectDiskEnvironment();
        }
    }

    @Override
    public void save(final CompoundTag nbt) {
        super.save(nbt);
        if (world() != null) {
            ContainerHelper.saveAllItems(nbt, items, world().registryAccess());
        }
        if (diskEnvironment != null) {
            final CompoundTag diskTag = new CompoundTag();
            diskEnvironment.save(diskTag);
            nbt.put(TAG_DISK, diskTag);
        }
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.get(DiskDriveBlockEntity.SLOT_FLOPPY).isEmpty();
    }

    @Override
    public ItemStack getItem(final int slot) {
        return slot == DiskDriveBlockEntity.SLOT_FLOPPY ? items.get(DiskDriveBlockEntity.SLOT_FLOPPY) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(final int slot, final int amount) {
        if (slot != DiskDriveBlockEntity.SLOT_FLOPPY) {
            return ItemStack.EMPTY;
        }
        final ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            markChanged();
            refreshDiskEnvironment();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        if (slot != DiskDriveBlockEntity.SLOT_FLOPPY) {
            return ItemStack.EMPTY;
        }
        final ItemStack removed = ContainerHelper.takeItem(items, slot);
        if (!removed.isEmpty()) {
            refreshDiskEnvironment();
        }
        return removed;
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        if (slot != DiskDriveBlockEntity.SLOT_FLOPPY || (!stack.isEmpty() && !canPlaceItem(slot, stack))) {
            return;
        }
        final ItemStack stored = stack.copy();
        if (!stored.isEmpty() && stored.getCount() > getMaxStackSize()) {
            stored.setCount(getMaxStackSize());
        }
        items.set(DiskDriveBlockEntity.SLOT_FLOPPY, stored);
        markChanged();
        refreshDiskEnvironment();
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        if (slot != DiskDriveBlockEntity.SLOT_FLOPPY || stack.isEmpty()) {
            return false;
        }
        final DriverItem driver = Driver.driverFor(stack);
        return driver != null && DiskDriveBlockEntity.acceptsDriverSlot(driver.slot(stack));
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(final Player player) {
        if (host instanceof li.cil.oc.api.internal.Rack rack && slot >= 0) {
            return rack.getMountable(slot) == this && ((Container) rack).stillValid(player);
        }
        return host == null || player == null || player.isAlive();
    }

    @Override
    public void setChanged() {
        markChanged();
    }

    @Override
    public void clearContent() {
        items.set(DiskDriveBlockEntity.SLOT_FLOPPY, ItemStack.EMPTY);
        markChanged();
        refreshDiskEnvironment();
    }

    private void refreshDiskEnvironment() {
        if (diskEnvironment != null && diskEnvironment.node() != null) {
            diskEnvironment.node().remove();
        }
        diskEnvironment = null;
        final ItemStack stack = items.get(DiskDriveBlockEntity.SLOT_FLOPPY);
        final DriverItem driver = Driver.driverFor(stack);
        if (driver == null || !DiskDriveBlockEntity.acceptsDriverSlot(driver.slot(stack))) {
            return;
        }
        diskEnvironment = driver.createEnvironment(stack, this);
        connectDiskEnvironment();
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
}
