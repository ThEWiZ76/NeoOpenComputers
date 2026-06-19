package li.cil.oc.api.driver;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public interface DriverItem {
    boolean worksWith(ItemStack stack);

    ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host);

    String slot(ItemStack stack);

    int tier(ItemStack stack);

    CompoundTag dataTag(ItemStack stack);
}
