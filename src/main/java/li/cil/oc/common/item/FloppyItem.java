package li.cil.oc.common.item;

import li.cil.oc.api.API;
import li.cil.oc.api.FileSystem;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModLootDisks;
import li.cil.oc.common.ModSettings;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class FloppyItem extends Item implements DriverItem {
    private static final String DRIVER_DATA_TAG = "oc:data";
    private static final String FLOPPY_DATA_TAG = "oc:floppy";
    private static final String LEGACY_LOOT_PATH_TAG = "oc:lootPath";
    private static final String LEGACY_LOOT_FACTORY_TAG = "oc:lootFactory";
    private static final String LEGACY_LABEL_TAG = "oc:fs.label";

    public FloppyItem(final Properties properties) {
        super(properties);
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
        final CompoundTag rootData = rootData(stack);
        if (rootData.contains(LEGACY_LOOT_PATH_TAG)) {
            final li.cil.oc.api.fs.FileSystem fileSystem = ModLootDisks.bundledFileSystem(rootData.getString(LEGACY_LOOT_PATH_TAG));
            final String label = rootData.getString(LEGACY_LABEL_TAG);
            return FileSystem.asManagedEnvironment(fileSystem, label.isEmpty() ? null : label, host, null);
        }
        if (rootData.contains(LEGACY_LOOT_FACTORY_TAG)) {
            if (!(API.items instanceof ItemRegistry registry)) {
                return null;
            }
            return createReadOnlyEnvironment(registry.floppyFactory(rootData.getString(LEGACY_LOOT_FACTORY_TAG)), rootData, host);
        }
        if (rootData.contains(ItemRegistry.FLOPPY_FACTORY_ID_TAG)) {
            if (!(API.items instanceof ItemRegistry registry)) {
                return null;
            }
            return createReadOnlyEnvironment(registry.floppyFactory(stack), rootData, host);
        }

        return createWritableEnvironment(dataTag(stack), saved -> writeDataTag(stack, saved), rootData, host);
    }

    private static ManagedEnvironment createReadOnlyEnvironment(final Callable<li.cil.oc.api.fs.FileSystem> factory, final CompoundTag rootData, final EnvironmentHost host) {
        if (factory == null) {
            return null;
        }
        try {
            final li.cil.oc.api.fs.FileSystem fileSystem = factory.call();
            if (fileSystem == null) {
                return null;
            }
            final String label = readLabel(rootData);
            final ManagedEnvironment environment = FileSystem.asManagedEnvironment(fileSystem, label.isEmpty() ? null : label, host, null);
            if (environment != null && environment.node() instanceof li.cil.oc.api.network.Component component) {
                component.setVisibility(Visibility.Network);
            }
            return environment;
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
        final li.cil.oc.api.fs.FileSystem fileSystem = FileSystem.fromMemory(ModSettings.floppySize() * 1024L);
        if (fileSystem == null) {
            return null;
        }
        final String label = itemData == null ? "" : itemData.getString(ItemRegistry.FLOPPY_LABEL_TAG);
        final ManagedEnvironment environment = FileSystem.asManagedEnvironment(fileSystem, label.isEmpty() ? null : label, host, null);
        if (environment == null) {
            return null;
        }
        if (environment.node() instanceof li.cil.oc.api.network.Component component) {
            component.setVisibility(Visibility.Network);
        }
        if (fileSystemData != null && !fileSystemData.isEmpty()) {
            environment.load(fileSystemData);
        }
        return new StackBackedEnvironment(environment, saveData);
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

    private record StackBackedEnvironment(ManagedEnvironment delegate, Consumer<CompoundTag> saveData) implements ManagedEnvironment {
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
    }
}
