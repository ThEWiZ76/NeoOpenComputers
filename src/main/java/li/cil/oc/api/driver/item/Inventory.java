package li.cil.oc.api.driver.item;

import li.cil.oc.api.driver.DriverItem;
import net.minecraft.world.item.ItemStack;

public interface Inventory extends DriverItem {
    int inventoryCapacity(ItemStack stack);
}
