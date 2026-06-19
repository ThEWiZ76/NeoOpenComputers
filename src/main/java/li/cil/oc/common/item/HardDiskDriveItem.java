package li.cil.oc.common.item;

import li.cil.oc.api.FileSystem;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class HardDiskDriveItem extends Item implements DriverItem {
    private static final long TIER_ONE_CAPACITY = 1024L * 1024L;

    public HardDiskDriveItem(final Properties properties) {
        super(properties);
    }

    @Override
    public boolean worksWith(final ItemStack stack) {
        return stack != null && stack.getItem() == this;
    }

    @Override
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        final li.cil.oc.api.fs.FileSystem fileSystem = FileSystem.fromMemory(TIER_ONE_CAPACITY);
        if (fileSystem == null) {
            return null;
        }
        return FileSystem.asManagedEnvironment(fileSystem, "hdd", host, null);
    }

    @Override
    public String slot(final ItemStack stack) {
        return Slot.HDD;
    }

    @Override
    public int tier(final ItemStack stack) {
        return 0;
    }

    @Override
    public CompoundTag dataTag(final ItemStack stack) {
        return new CompoundTag();
    }
}
