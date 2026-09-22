package li.cil.oc.common.item;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.component.LeashUpgradeEnvironment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

public class LeashUpgradeItem extends BasicUpgradeItem {
    public LeashUpgradeItem(final Properties properties) {
        super(properties, 0);
    }

    @Override
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        if (ItemDriverData.isClientSide(host)) {
            return null;
        }
        final var environment = new LeashUpgradeEnvironment(host) {
            @Override public void save(final CompoundTag data) {
                super.save(data);
                ItemDriverData.writeDataTag(stack, data);
            }
        };
        environment.load(dataTag(stack));
        return environment;
    }
}
