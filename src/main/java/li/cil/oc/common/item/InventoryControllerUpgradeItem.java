package li.cil.oc.common.item;

import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.internal.Adapter;
import li.cil.oc.api.internal.Drone;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.component.InventoryControllerEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class InventoryControllerUpgradeItem extends Item implements HostAware {
    public InventoryControllerUpgradeItem(final Properties properties) {
        super(properties);
    }

    @Override
    public boolean worksWith(final ItemStack stack) {
        return stack != null && stack.getItem() == this;
    }

    @Override
    public boolean worksWith(final ItemStack stack, final Class<? extends EnvironmentHost> host) {
        return worksWith(stack) && (Adapter.class.isAssignableFrom(host) || Drone.class.isAssignableFrom(host) || Robot.class.isAssignableFrom(host));
    }

    @Override
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        if (ItemDriverData.isClientSide(host)) {
            return null;
        }
        if (host instanceof Robot robot) {
            return new InventoryControllerEnvironment.RobotInventoryControllerEnvironment(robot);
        }
        if (host instanceof Drone drone) {
            return new InventoryControllerEnvironment.AgentInventoryControllerEnvironment(drone);
        }
        if (host instanceof Adapter) {
            return new InventoryControllerEnvironment(host);
        }
        return null;
    }

    @Override
    public String slot(final ItemStack stack) {
        return Slot.Upgrade;
    }

    @Override
    public int tier(final ItemStack stack) {
        return 1;
    }

    @Override
    public CompoundTag dataTag(final ItemStack stack) {
        return ItemDriverData.dataTag(stack);
    }
}
