package li.cil.oc.common.item;

import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.internal.Tiered;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.component.ServerRackMountableEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ServerItem extends Item implements DriverItem, Tiered {
    private final int tier;

    public ServerItem(final Properties properties, final int tier) {
        super(properties.stacksTo(1));
        this.tier = Math.max(0, Math.min(2, tier));
    }

    @Override
    public int tier() {
        return tier;
    }

    @Override
    public boolean worksWith(final ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == this;
    }

    @Override
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        return new ServerRackMountableEnvironment(tier, false);
    }

    @Override
    public String slot(final ItemStack stack) {
        return Slot.RackMountable;
    }

    @Override
    public int tier(final ItemStack stack) {
        return tier;
    }

    @Override
    public CompoundTag dataTag(final ItemStack stack) {
        return new CompoundTag();
    }
}
