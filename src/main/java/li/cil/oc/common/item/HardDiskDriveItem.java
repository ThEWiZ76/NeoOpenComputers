package li.cil.oc.common.item;

import li.cil.oc.api.FileSystem;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.DriveEnvironment;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.menu.DriveMenu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class HardDiskDriveItem extends Item implements DriverItem {
    private static final String DRIVER_DATA_TAG = "oc:data";
    private static final String HDD_DATA_TAG = "oc:hdd";
    private static final String UNMANAGED_TAG = "oc:unmanaged";
    private static final String LOCK_TAG = "oc:lock";
    private final int tier;

    public HardDiskDriveItem(final Properties properties) {
        this(properties, 0);
    }

    public HardDiskDriveItem(final Properties properties, final int tier) {
        super(properties);
        this.tier = Math.max(0, Math.min(2, tier));
    }

    @Override
    public boolean worksWith(final ItemStack stack) {
        return stack != null && stack.getItem() == this;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                player.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, menuPlayer) -> new DriveMenu(containerId, playerInventory, stack),
                    driveTitle()));
            }
            player.swing(hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context, final List<Component> tooltip, final TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        final String label = label(stack);
        if (label != null && !label.isBlank()) {
            tooltip.add(Component.literal(label));
        }
        tooltip.add(Component.translatable(isUnmanaged(stack)
            ? "tooltip.neoopencomputers.drive.mode.unmanaged"
            : "tooltip.neoopencomputers.drive.mode.managed"));
        tooltip.add(isLocked(stack)
            ? Component.translatable("tooltip.neoopencomputers.drive.locked", lockInfo(stack))
            : Component.translatable("tooltip.neoopencomputers.drive.unlocked"));
    }

    @Override
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        if (ItemDriverData.isClientSide(host)) {
            return null;
        }
        return createEnvironment(tier(stack), dataTag(stack), saved -> writeDataTag(stack, saved), host);
    }

    static ManagedEnvironment createEnvironment(final CompoundTag data, final Consumer<CompoundTag> saveData, final EnvironmentHost host) {
        return createEnvironment(0, data, saveData, host);
    }

    static ManagedEnvironment createEnvironment(final int tier, final CompoundTag data, final Consumer<CompoundTag> saveData, final EnvironmentHost host) {
        final int clampedTier = Math.max(0, Math.min(2, tier));
        if (isUnmanaged(data)) {
            final ManagedEnvironment environment = new DriveEnvironment(
                ModSettings.hddSize(clampedTier) * 1024,
                ModSettings.hddPlatterCount(clampedTier),
                new ItemDiskLabel(null),
                host,
                null,
                clampedTier + 2,
                lockInfo(data));
            if (data != null && !data.isEmpty()) {
                environment.load(data);
            }
            return new StackBackedEnvironment(environment, saveData);
        }
        final li.cil.oc.api.fs.FileSystem fileSystem = FileSystem.fromMemory(ModSettings.hddSize(clampedTier) * 1024L);
        if (fileSystem == null) {
            return null;
        }
        final ManagedEnvironment environment = FileSystem.asManagedEnvironment(fileSystem, new ItemDiskLabel(null), host, null, clampedTier + 2);
        if (environment == null) {
            return null;
        }
        if (data != null && !data.isEmpty()) {
            environment.load(data);
        }
        return new StackBackedEnvironment(environment, saveData);
    }

    public static boolean isUnmanaged(final ItemStack stack) {
        return isUnmanaged(stackDataTag(stack));
    }

    public static boolean isUnmanaged(final CompoundTag data) {
        return data != null && data.getBoolean(UNMANAGED_TAG);
    }

    public static boolean isLocked(final ItemStack stack) {
        return isLocked(stackDataTag(stack));
    }

    public static boolean isLocked(final CompoundTag data) {
        return data != null && data.contains(LOCK_TAG) && !data.getString(LOCK_TAG).isEmpty();
    }

    public static String lockInfo(final ItemStack stack) {
        return lockInfo(stackDataTag(stack));
    }

    public static String lockInfo(final CompoundTag data) {
        return isLocked(data) ? data.getString(LOCK_TAG) : "";
    }

    public static void setUnmanaged(final ItemStack stack, final boolean unmanaged) {
        final CompoundTag data = stackDataTag(stack);
        setUnmanaged(data, unmanaged);
    }

    public static void setUnmanaged(final CompoundTag data, final boolean unmanaged) {
        if (data == null) {
            return;
        }
        if (isUnmanaged(data) != unmanaged) {
            data.remove(LOCK_TAG);
        }
        data.putBoolean(UNMANAGED_TAG, unmanaged);
    }

    public static void lock(final ItemStack stack, final Player player) {
        final CompoundTag data = stackDataTag(stack);
        if (!isLocked(data)) {
            final String name = player == null || player.getGameProfile() == null || player.getGameProfile().getName() == null || player.getGameProfile().getName().isBlank()
                ? "notch"
                : player.getGameProfile().getName();
            data.putString(LOCK_TAG, name);
        }
    }

    public static void lock(final CompoundTag data, final String lockInfo) {
        if (data != null && !isLocked(data)) {
            data.putString(LOCK_TAG, lockInfo == null || lockInfo.isBlank() ? "notch" : lockInfo);
        }
    }

    public static Component driveTitle() {
        return Component.translatable("oc:container.drive");
    }

    private static String label(final ItemStack stack) {
        final CompoundTag data = stackDataTag(stack);
        return data.contains(ItemDiskLabel.TAG) ? data.getString(ItemDiskLabel.TAG) : null;
    }

    @Override
    public String slot(final ItemStack stack) {
        return Slot.HDD;
    }

    @Override
    public int tier(final ItemStack stack) {
        return tier;
    }

    @Override
    public CompoundTag dataTag(final ItemStack stack) {
        return stackDataTag(stack);
    }

    public static CompoundTag stackDataTag(final ItemStack stack) {
        if (stack == null) {
            return new CompoundTag();
        }
        final CompoundTag data = ItemDriverData.dataTag(stack);
        if (data.isEmpty()) {
            final CompoundTag legacyData = legacyDataTag(stack);
            if (!legacyData.isEmpty()) {
                data.merge(legacyData);
            }
        }
        return data;
    }

    private static void writeDataTag(final ItemStack stack, final CompoundTag data) {
        if (stack == null) {
            return;
        }
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        final CompoundTag root = customData == null ? new CompoundTag() : customData.copyTag();
        root.put(DRIVER_DATA_TAG, data.copy());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    private static CompoundTag legacyDataTag(final ItemStack stack) {
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return new CompoundTag();
        }
        return customData.copyTag().getCompound(HDD_DATA_TAG);
    }

    private record StackBackedEnvironment(ManagedEnvironment delegate, Consumer<CompoundTag> saveData) implements ManagedEnvironment, DeviceInfo {
        @Override
        public Node node() {
            return delegate.node();
        }

        @Override
        public void onConnect(final Node node) {
            delegate.onConnect(node);
        }

        @Override
        public void onDisconnect(final Node node) {
            delegate.onDisconnect(node);
        }

        @Override
        public void onMessage(final Message message) {
            delegate.onMessage(message);
        }

        @Override
        public boolean canUpdate() {
            return delegate.canUpdate();
        }

        @Override
        public void update() {
            delegate.update();
        }

        @Override
        public void load(final CompoundTag nbt) {
            delegate.load(nbt);
        }

        @Override
        public void save(final CompoundTag nbt) {
            delegate.save(nbt);
            if (saveData != null) {
                saveData.accept(nbt.copy());
            }
        }

        @Override
        public Map<String, String> getDeviceInfo() {
            return delegate instanceof DeviceInfo info ? info.getDeviceInfo() : Map.of();
        }
    }
}
