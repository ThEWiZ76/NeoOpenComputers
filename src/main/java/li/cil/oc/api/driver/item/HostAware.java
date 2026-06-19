package li.cil.oc.api.driver.item;

import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.world.item.ItemStack;

public interface HostAware extends DriverItem {
    boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host);
}
