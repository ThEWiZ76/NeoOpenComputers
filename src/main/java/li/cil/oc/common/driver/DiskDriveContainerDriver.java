package li.cil.oc.common.driver;

import li.cil.oc.api.driver.item.Container;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class DiskDriveContainerDriver implements Container {
    @Override
    public boolean worksWith(final ItemStack stack) {
        return stack != null && stack.getItem() == ModItems.DISK_DRIVE.get();
    }

    @Override
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        return null;
    }

    @Override
    public String slot(final ItemStack stack) {
        return Slot.Container;
    }

    @Override
    public int tier(final ItemStack stack) {
        return 0;
    }

    @Override
    public CompoundTag dataTag(final ItemStack stack) {
        return new CompoundTag();
    }

    @Override
    public String providedSlot(final ItemStack stack) {
        return Slot.Floppy;
    }

    @Override
    public int providedTier(final ItemStack stack) {
        return Integer.MAX_VALUE;
    }
}
