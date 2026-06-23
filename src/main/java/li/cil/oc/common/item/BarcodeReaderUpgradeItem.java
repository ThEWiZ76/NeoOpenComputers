package li.cil.oc.common.item;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.component.BarcodeReaderUpgradeEnvironment;
import net.minecraft.world.item.ItemStack;

public class BarcodeReaderUpgradeItem extends BasicUpgradeItem {
    public BarcodeReaderUpgradeItem(final Properties properties) {
        super(properties, 1);
    }

    @Override
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        return new BarcodeReaderUpgradeEnvironment(host);
    }
}
