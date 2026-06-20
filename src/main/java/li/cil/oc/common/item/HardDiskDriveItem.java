package li.cil.oc.common.item;

import li.cil.oc.api.FileSystem;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

public class HardDiskDriveItem extends Item implements DriverItem {
    private static final long[] CAPACITIES = {
        1024L * 1024L,
        2048L * 1024L,
        4096L * 1024L
    };
    private static final String HDD_DATA_TAG = "oc:hdd";
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
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        return createEnvironment(tier(stack), dataTag(stack), saved -> writeDataTag(stack, saved), host);
    }

    static ManagedEnvironment createEnvironment(final CompoundTag data, final Consumer<CompoundTag> saveData, final EnvironmentHost host) {
        return createEnvironment(0, data, saveData, host);
    }

    static ManagedEnvironment createEnvironment(final int tier, final CompoundTag data, final Consumer<CompoundTag> saveData, final EnvironmentHost host) {
        final int clampedTier = Math.max(0, Math.min(2, tier));
        final li.cil.oc.api.fs.FileSystem fileSystem = FileSystem.fromMemory(CAPACITIES[clampedTier]);
        if (fileSystem == null) {
            return null;
        }
        final ManagedEnvironment environment = FileSystem.asManagedEnvironment(fileSystem, "hdd", host, null);
        if (environment == null) {
            return null;
        }
        if (data != null && !data.isEmpty()) {
            environment.load(data);
        }
        return new StackBackedEnvironment(environment, saveData);
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
        if (stack == null) {
            return new CompoundTag();
        }
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return new CompoundTag();
        }
        return customData.copyTag().getCompound(HDD_DATA_TAG);
    }

    private static void writeDataTag(final ItemStack stack, final CompoundTag data) {
        if (stack == null) {
            return;
        }
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        final CompoundTag root = customData == null ? new CompoundTag() : customData.copyTag();
        root.put(HDD_DATA_TAG, data.copy());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
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
