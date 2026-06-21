package li.cil.oc.common.item;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.component.ChunkloaderUpgradeEnvironment;
import net.minecraft.world.item.ItemStack;

public class ChunkloaderUpgradeItem extends BasicUpgradeItem {
    public ChunkloaderUpgradeItem(final Properties properties) {
        super(properties, 2);
    }

    @Override
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        if (host != null && host.world() != null && host.world().isClientSide()) {
            return null;
        }
        return new ChunkloaderUpgradeEnvironment(host);
    }
}
