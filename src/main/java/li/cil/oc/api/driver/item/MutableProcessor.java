package li.cil.oc.api.driver.item;

import li.cil.oc.api.machine.Architecture;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;

public interface MutableProcessor extends Processor {
    Collection<Class<? extends Architecture>> allArchitectures();

    void setArchitecture(ItemStack stack, Class<? extends Architecture> architecture);
}
