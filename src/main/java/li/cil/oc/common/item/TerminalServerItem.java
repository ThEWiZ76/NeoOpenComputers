package li.cil.oc.common.item;

import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.component.TerminalServerRackMountableEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class TerminalServerItem extends Item implements DriverItem, HostAware {
    public TerminalServerItem(final Properties properties) {
        super(properties);
    }

    @Override
    public boolean worksWith(final ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == this;
    }

    @Override
    public boolean worksWith(final ItemStack stack, final Class<? extends EnvironmentHost> host) {
        return worksWith(stack);
    }

    @Override
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        if (host instanceof Rack rack) {
            return new TerminalServerRackMountableEnvironment(host, findSlot(rack, stack));
        }
        return new TerminalServerRackMountableEnvironment();
    }

    @Override
    public String slot(final ItemStack stack) {
        return Slot.RackMountable;
    }

    @Override
    public int tier(final ItemStack stack) {
        return 0;
    }

    @Override
    public CompoundTag dataTag(final ItemStack stack) {
        return ItemDriverData.dataTag(stack);
    }

    private static int findSlot(final Rack rack, final ItemStack stack) {
        for (int slot = 0; slot < rack.getContainerSize(); slot++) {
            final ItemStack candidate = rack.getItem(slot);
            if (candidate == stack || ItemStack.matches(candidate, stack)) {
                return slot;
            }
        }
        return -1;
    }
}
