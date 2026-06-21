package li.cil.oc.common.item;

import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.component.TractorBeamUpgradeEnvironment;
import net.minecraft.world.item.ItemStack;

public class TractorBeamUpgradeItem extends BasicUpgradeItem {
    public TractorBeamUpgradeItem(final Properties properties) {
        super(properties, 2);
    }

    @Override
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        return host instanceof Agent agent ? new TractorBeamUpgradeEnvironment(agent) : null;
    }
}
