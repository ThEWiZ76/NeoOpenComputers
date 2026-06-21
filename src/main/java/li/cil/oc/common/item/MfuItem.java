package li.cil.oc.common.item;

import li.cil.oc.api.internal.Adapter;
import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.world.item.ItemStack;

public class MfuItem extends BasicUpgradeItem {
    public MfuItem(final Properties properties) {
        super(properties, 2);
    }

    @Override
    public boolean worksWith(final ItemStack stack, final Class<? extends EnvironmentHost> host) {
        return worksWith(stack) && Adapter.class.isAssignableFrom(host);
    }
}
