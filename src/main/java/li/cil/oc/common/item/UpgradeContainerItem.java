package li.cil.oc.common.item;

import li.cil.oc.api.driver.item.Container;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class UpgradeContainerItem extends Item implements Container {
    private final int tier;

    public UpgradeContainerItem(final Properties properties, final int tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public boolean worksWith(final ItemStack stack) {
        return stack != null && stack.getItem() == this;
    }

    @Override
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        return null;
    }

    @Override
    public String slot(final ItemStack stack) {
        return Slot.Container;
    }

    @Override
    public int tier(final ItemStack stack) {
        return tier;
    }

    @Override
    public CompoundTag dataTag(final ItemStack stack) {
        return new CompoundTag();
    }

    @Override
    public String providedSlot(final ItemStack stack) {
        return Slot.Upgrade;
    }

    @Override
    public int providedTier(final ItemStack stack) {
        return tier(stack);
    }
}
