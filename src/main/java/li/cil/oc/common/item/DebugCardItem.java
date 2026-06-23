package li.cil.oc.common.item;

import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.component.DebugCardEnvironment;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class DebugCardItem extends Item implements DriverItem {
    public DebugCardItem(final Properties properties) {
        super(properties);
    }

    @Override
    public boolean worksWith(final ItemStack stack) {
        return stack != null && stack.getItem() == this;
    }

    @Override
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        if (host != null && host.world() != null && host.world().isClientSide) {
            return null;
        }
        return new DebugCardEnvironment(host, DebugCardEnvironment.loadAccess(dataTag(stack)));
    }

    @Override
    public String slot(final ItemStack stack) {
        return Slot.Card;
    }

    @Override
    public int tier(final ItemStack stack) {
        return 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public CompoundTag dataTag(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new CompoundTag();
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(new CompoundTag()));
            customData = stack.get(DataComponents.CUSTOM_DATA);
        }
        return customData.getUnsafe();
    }
}
