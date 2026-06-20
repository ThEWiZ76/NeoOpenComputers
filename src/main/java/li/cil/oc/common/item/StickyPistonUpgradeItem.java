package li.cil.oc.common.item;

import li.cil.oc.api.internal.Rotatable;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.component.StickyPistonUpgradeEnvironment;
import net.minecraft.world.item.ItemStack;

public class StickyPistonUpgradeItem extends PistonUpgradeItem {
    public StickyPistonUpgradeItem(final Properties properties) {
        super(properties);
    }

    @Override
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        return host instanceof Rotatable rotatable ? new StickyPistonUpgradeEnvironment(host, rotatable) : null;
    }

    @Override
    public int tier(final ItemStack stack) {
        return 1;
    }
}
