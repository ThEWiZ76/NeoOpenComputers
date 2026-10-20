package li.cil.oc.common.item;

import li.cil.oc.api.API;
import li.cil.oc.api.FileSystem;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModLootDisks;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class FloppyItem extends Item implements DriverItem {
    private static final String DRIVER_DATA_TAG = "oc:data";
    private static final String FLOPPY_DATA_TAG = "oc:floppy";
    private static final String LEGACY_LOOT_PATH_TAG = "oc:lootPath";
    private static final String LEGACY_LOOT_FACTORY_TAG = "oc:lootFactory";
    private static final String LEGACY_LABEL_TAG = "oc:fs.label";
    private static final int DEFAULT_FLOPPY_COLOR_INDEX = 8;

    public FloppyItem(final Properties properties) {
        super(properties);
    }

    public static boolean isLootDisk(ItemStack stack) {
        final var data = rootData(stack);
        return data.contains(LEGACY_LOOT_PATH_TAG) || data.contains(LEGACY_LOOT_FACTORY_TAG) || data.contains(ItemRegistry.FLOPPY_FACTORY_ID_TAG);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        final var stack = player.getItemInHand(hand);
        return isLootDisk(stack) ? InteractionResultHolder.sidedSuccess(stack, level.isClientSide) : HardDiskDriveItem.useDrive(level, player, hand);
    }

    public static int floppyColorIndex(final ItemStack stack) {
        return floppyColorIndex(rootData(stack));
    }

    public static int floppyColorIndex(final CompoundTag rootData) {
        if (rootData == null) {
            return DEFAULT_FLOPPY_COLOR_INDEX;
        }
        if (rootData.contains(ItemRegistry.FLOPPY_COLOR_TAG, Tag.TAG_INT)) {
            return Math.clamp(rootData.getInt(ItemRegistry.FLOPPY_COLOR_TAG), 0, 15);
        }
        if (rootData.contains(ItemRegistry.FLOPPY_COLOR_TAG, Tag.TAG_STRING)) {
            return dyeIndex(rootData.getString(ItemRegistry.FLOPPY_COLOR_TAG));
        }
        return DEFAULT_FLOPPY_COLOR_INDEX;
    }

    @Override
    public Component getName(final ItemStack stack) {
        final CompoundTag rootData = rootData(stack);
        if (rootData.contains(ItemRegistry.FLOPPY_LABEL_TAG)) {
            return Component.literal(rootData.getString(ItemRegistry.FLOPPY_LABEL_TAG));
        }
        final CompoundTag driverData = dataTag(stack);
        if (driverData.contains(ItemRegistry.FLOPPY_LABEL_TAG)) {
            return Component.literal(driverData.getString(ItemRegistry.FLOPPY_LABEL_TAG));
        }
        return super.getName(stack);
    }

    @Override
    public boolean worksWith(final ItemStack stack) {
        return stack != null && stack.getItem() == this;
    }

    @Override
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        if (ItemDriverData.isClientSide(host)) {
            return null;
        }
        final CompoundTag rootData = rootData(stack);
        if (rootData.contains(LEGACY_LOOT_PATH_TAG)) {
            return createReadOnlyEnvironment(() -> ModLootDisks.legacyFileSystem(rootData.getString(LEGACY_LOOT_PATH_TAG)), stack, rootData, host);
        }
        if (rootData.contains(LEGACY_LOOT_FACTORY_TAG)) {
            if (!(API.items instanceof ItemRegistry registry)) {
                return null;
            }
            return createReadOnlyEnvironment(registry.floppyFactory(rootData.getString(LEGACY_LOOT_FACTORY_TAG)), stack, rootData, host);
        }
        if (rootData.contains(ItemRegistry.FLOPPY_FACTORY_ID_TAG)) {
            if (!(API.items instanceof ItemRegistry registry)) {
                return null;
            }
            return createReadOnlyEnvironment(registry.floppyFactory(stack), stack, rootData, host);
        }

        return createWritableEnvironment(dataTag(stack), saved -> writeDataTag(stack, saved), rootData, host);
    }

    private ManagedEnvironment createReadOnlyEnvironment(final Callable<li.cil.oc.api.fs.FileSystem> factory, final ItemStack stack, final CompoundTag rootData, final EnvironmentHost host) {
        if (factory == null) {
            return null;
        }
        try {
            final li.cil.oc.api.fs.FileSystem fileSystem = factory.call();
            if (fileSystem == null) {
                return null;
            }
            final String label = readLabel(rootData);
            final ManagedEnvironment environment = FileSystem.asManagedEnvironment(fileSystem, label.isEmpty() ? null : label, host, ModSounds.FLOPPY_ACCESS_ID);
            if (environment != null && environment.node() instanceof li.cil.oc.api.network.Component component) {
                component.setVisibility(Visibility.Network);
            }
            if (environment == null) return null;
            // Read-only contents still have persistent node identity and open file handles.
            environment.load(dataTag(stack));
            return new DiskItemEnvironment(environment, saved -> writeDataTag(stack, saved), "");
        } catch (Exception e) {
            return null;
        }
    }

    private static String readLabel(final CompoundTag rootData) {
        if (rootData.contains(ItemRegistry.FLOPPY_LABEL_TAG)) {
            return rootData.getString(ItemRegistry.FLOPPY_LABEL_TAG);
        }
        return rootData.getString(LEGACY_LABEL_TAG);
    }

    static ManagedEnvironment createWritableEnvironment(final CompoundTag fileSystemData, final Consumer<CompoundTag> saveData, final CompoundTag itemData, final EnvironmentHost host) {
        final String label = itemData == null ? "" : itemData.getString(ItemRegistry.FLOPPY_LABEL_TAG);
        final var environment = DiskItemEnvironment.create(ModSettings.floppySize() * 1024L, 1, 1, ModSounds.FLOPPY_ACCESS_ID,
            fileSystemData, saveData, host, label.isEmpty() ? null : label);
        if (environment != null && environment.node() instanceof li.cil.oc.api.network.Component component) component.setVisibility(Visibility.Network);
        return environment;
    }

    @Override
    public String slot(final ItemStack stack) {
        return Slot.Floppy;
    }

    @Override
    public int tier(final ItemStack stack) {
        return 0;
    }

    @Override
    public boolean doesSneakBypassUse(final ItemStack stack, final LevelReader level, final BlockPos pos, final Player player) {
        return true;
    }

    @Override
    public CompoundTag dataTag(final ItemStack stack) {
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

    private static CompoundTag rootData(final ItemStack stack) {
        if (stack == null) {
            return new CompoundTag();
        }
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? new CompoundTag() : customData.copyTag();
    }

    private static int dyeIndex(final String colorName) {
        return switch (colorName) {
            case "black" -> 0;
            case "red" -> 1;
            case "green" -> 2;
            case "brown" -> 3;
            case "blue" -> 4;
            case "purple" -> 5;
            case "cyan" -> 6;
            case "light_gray", "silver" -> 7;
            case "gray" -> 8;
            case "pink" -> 9;
            case "lime" -> 10;
            case "yellow" -> 11;
            case "light_blue" -> 12;
            case "magenta" -> 13;
            case "orange" -> 14;
            case "white" -> 15;
            default -> DEFAULT_FLOPPY_COLOR_INDEX;
        };
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
        return customData.copyTag().getCompound(FLOPPY_DATA_TAG);
    }

}
