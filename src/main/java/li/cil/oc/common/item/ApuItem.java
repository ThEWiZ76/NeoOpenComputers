package li.cil.oc.common.item;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.component.ApuEnvironment;
import net.minecraft.world.item.ItemStack;

public final class ApuItem extends CpuItem {
    private final int gpuTier;

    public ApuItem(final Properties properties, final int tier) {
        super(properties, Math.max(0, Math.min(2, tier + 1)));
        this.gpuTier = Math.max(0, Math.min(1, tier));
    }

    @Override
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        return new ApuEnvironment(gpuTier);
    }
}
