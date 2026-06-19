package li.cil.oc.api.driver;

import net.minecraft.world.item.ItemStack;

public interface EnvironmentProvider {
    Class<?> getEnvironment(ItemStack stack);
}
