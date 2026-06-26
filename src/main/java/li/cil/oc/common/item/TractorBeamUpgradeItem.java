package li.cil.oc.common.item;

import li.cil.oc.api.internal.Drone;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.internal.Tablet;
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
        if (ItemDriverData.isClientSide(host)) {
            return null;
        }
        if (host instanceof Robot robot) {
            return new TractorBeamUpgradeEnvironment(robot);
        }
        if (host instanceof Drone drone) {
            return new TractorBeamUpgradeEnvironment(drone);
        }
        if (host instanceof Tablet tablet) {
            return new TractorBeamUpgradeEnvironment(tablet);
        }
        return null;
    }
}
