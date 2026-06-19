package li.cil.oc.api.driver.item;

import li.cil.oc.api.driver.DriverItem;
import net.minecraft.world.item.ItemStack;

public interface Memory extends DriverItem {
    double amount(ItemStack stack);
}
