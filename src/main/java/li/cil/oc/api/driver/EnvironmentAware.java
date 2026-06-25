package li.cil.oc.api.driver;

import li.cil.oc.api.network.Environment;
import net.minecraft.world.item.ItemStack;

/**
 * @deprecated Use an {@link EnvironmentProvider} instead.
 */
@Deprecated
public interface EnvironmentAware {
    Class<? extends Environment> providedEnvironment(ItemStack stack);
}
