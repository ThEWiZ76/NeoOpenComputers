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

    /**
     * Item-owned child tag for environment persistence, or null to use the default oc:data child.
     * Hosts load a copy of this state and pass an empty tag to save, then replace
     * this child with the produced state. Sibling item data is preserved.
     */
    CompoundTag dataTag(ItemStack stack);
}
