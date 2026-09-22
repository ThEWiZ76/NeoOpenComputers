package li.cil.oc.common.item;

import li.cil.oc.api.internal.Drone;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.internal.Tablet;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.component.TractorBeamUpgradeEnvironment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

public class TractorBeamUpgradeItem extends BasicUpgradeItem {
    public TractorBeamUpgradeItem(final Properties properties) {
        super(properties, 2);
    }

    @Override
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        if (ItemDriverData.isClientSide(host)) {
            return null;
        }
        final TractorBeamUpgradeEnvironment environment;
        if (host instanceof Robot || host instanceof Drone) {
            environment = new TractorBeamUpgradeEnvironment((li.cil.oc.api.internal.Agent) host) {
                @Override public void save(final CompoundTag data) {
                    super.save(data);
                    ItemDriverData.writeDataTag(stack, data);
                }
            };
        } else if (host instanceof Tablet tablet) {
            environment = new TractorBeamUpgradeEnvironment(tablet) {
                @Override public void save(final CompoundTag data) {
                    super.save(data);
                    ItemDriverData.writeDataTag(stack, data);
                }
            };
        } else {
            return null;
        }
        environment.load(dataTag(stack));
        return environment;
    }
}
