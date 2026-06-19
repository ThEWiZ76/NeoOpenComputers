package li.cil.oc.common.item;

import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class CpuItem extends Item implements Processor {
    public CpuItem(final Properties properties) {
        super(properties);
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
        return Slot.CPU;
    }

    @Override
    public int tier(final ItemStack stack) {
        return 0;
    }

    @Override
    public CompoundTag dataTag(final ItemStack stack) {
        return new CompoundTag();
    }

    @Override
    public int supportedComponents(final ItemStack stack) {
        return 8;
    }

    @Override
    public Class<? extends Architecture> architecture(final ItemStack stack) {
        return li.cil.oc.api.Machine.LuaArchitecture;
    }
}
