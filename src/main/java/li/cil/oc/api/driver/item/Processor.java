package li.cil.oc.api.driver.item;

import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.machine.Architecture;
import net.minecraft.world.item.ItemStack;

public interface Processor extends DriverItem {
    int supportedComponents(ItemStack stack);

    Class<? extends Architecture> architecture(ItemStack stack);
}
