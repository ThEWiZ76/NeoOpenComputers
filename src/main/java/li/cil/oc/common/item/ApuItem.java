package li.cil.oc.common.item;

import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.component.ApuEnvironment;
import net.minecraft.world.item.ItemStack;

public final class ApuItem extends CpuItem implements HostAware {
    private final int gpuTier;

    public ApuItem(final Properties properties, final int tier) {
        super(properties, Math.max(0, Math.min(2, tier + 1)));
        this.gpuTier = Math.max(0, Math.min(1, tier));
    }

    @Override
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        if (ItemDriverData.isClientSide(host)) {
            return null;
        }
        return new ApuEnvironment(gpuTier, dataTag(stack), saved -> ItemDriverData.writeDataTag(stack, saved));
    }

    @Override
    public boolean worksWith(final ItemStack stack, final Class<? extends EnvironmentHost> host) {
        return worksWith(stack);
    }
}
