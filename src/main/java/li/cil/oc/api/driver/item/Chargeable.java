package li.cil.oc.api.driver.item;

import net.minecraft.world.item.ItemStack;

public interface Chargeable {
    boolean canCharge(ItemStack stack);

    double charge(ItemStack stack, double amount, boolean simulate);
}
