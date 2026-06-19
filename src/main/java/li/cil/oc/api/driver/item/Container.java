package li.cil.oc.api.driver.item;

import li.cil.oc.api.driver.DriverItem;
import net.minecraft.world.item.ItemStack;

public interface Container extends DriverItem {
    String providedSlot(ItemStack stack);

    int providedTier(ItemStack stack);
}
