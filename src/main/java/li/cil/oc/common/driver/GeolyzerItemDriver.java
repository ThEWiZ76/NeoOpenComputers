package li.cil.oc.common.driver;

import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.component.GeolyzerEnvironment;
import li.cil.oc.common.item.ItemDriverData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class GeolyzerItemDriver implements HostAware {
    @Override
    public boolean worksWith(final ItemStack stack) {
        return stack != null && stack.getItem() == ModItems.GEOLYZER.get();
    }

    @Override
    public boolean worksWith(final ItemStack stack, final Class<? extends EnvironmentHost> host) {
        return worksWith(stack);
    }

    @Override
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        if (host != null && host.world() != null && host.world().isClientSide) {
            return null;
        }
        return new GeolyzerEnvironment(host);
    }

    @Override
    public String slot(final ItemStack stack) {
        return Slot.Upgrade;
    }

    @Override
    public int tier(final ItemStack stack) {
        return 0;
    }

    @Override
    public CompoundTag dataTag(final ItemStack stack) {
        return ItemDriverData.dataTag(stack);
    }
}
