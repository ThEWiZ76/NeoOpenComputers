package li.cil.oc.common.item;

import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.ModSettings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ComponentBusItem extends Item implements Processor {
    private final int tier;

    public ComponentBusItem(final Properties properties, final int tier) {
        super(properties);
        this.tier = Math.max(0, Math.min(2, tier));
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
        return Slot.ComponentBus;
    }

    @Override
    public int tier(final ItemStack stack) {
        return tier;
    }

    @Override
    public CompoundTag dataTag(final ItemStack stack) {
        return ItemDriverData.dataTag(stack);
    }

    @Override
    public int supportedComponents(final ItemStack stack) {
        return ModSettings.cpuComponentCount(tier);
    }

    @Override
    public Class<? extends Architecture> architecture(final ItemStack stack) {
        return null;
    }
}
