package li.cil.oc.api.internal;

import net.minecraft.world.item.ItemStack;

public interface Database {
    int size();

    ItemStack getStackInSlot(int slot);

    void setStackInSlot(int slot, ItemStack stack);

    int findStackWithHash(String hash);
}
