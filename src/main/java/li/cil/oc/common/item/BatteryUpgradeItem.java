package li.cil.oc.common.item;

import li.cil.oc.api.driver.item.Chargeable;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.component.BatteryUpgradeEnvironment;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class BatteryUpgradeItem extends Item implements HostAware, Chargeable {
    private static final String DATA_TAG = "oc:battery";
    private static final String CHARGE_TAG = "charge";

    private final int tier;

    public BatteryUpgradeItem(final Properties properties, final int tier) {
        super(properties);
        this.tier = Math.max(0, Math.min(BatteryUpgradeEnvironment.CAPACITIES.length - 1, tier));
    }

    @Override
    public boolean worksWith(final ItemStack stack) {
        return stack != null && stack.getItem() == this;
    }

    @Override
    public boolean worksWith(final ItemStack stack, final Class<? extends EnvironmentHost> host) {
        return worksWith(stack);
    }

    @Override
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        if (ItemDriverData.isClientSide(host)) {
            return null;
        }
        return new BatteryUpgradeEnvironment(tier(stack));
    }

    @Override
    public String slot(final ItemStack stack) {
        return Slot.Upgrade;
    }

    @Override
    public int tier(final ItemStack stack) {
        return tier;
    }

    @Override
    public CompoundTag dataTag(final ItemStack stack) {
        return readData(stack);
    }

    @Override
    public boolean canCharge(final ItemStack stack) {
        return worksWith(stack);
    }

    @Override
    public double charge(final ItemStack stack, final double amount, final boolean simulate) {
        if (!canCharge(stack) || amount <= 0D) {
            return 0D;
        }
        final double stored = chargeStored(stack);
        final double accepted = Math.min(amount, Math.max(0D, maxCharge() - stored));
        if (!simulate && accepted > 0D) {
            writeCharge(stack, stored + accepted);
        }
        return accepted;
    }

    public double chargeStored(final ItemStack stack) {
        return Math.max(0D, Math.min(readData(stack).getDouble(CHARGE_TAG), maxCharge()));
    }

    public double maxCharge() {
        return BatteryUpgradeEnvironment.capacity(tier);
    }

    private static CompoundTag readData(final ItemStack stack) {
        if (stack == null) {
            return new CompoundTag();
        }
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return new CompoundTag();
        }
        return customData.copyTag().getCompound(DATA_TAG);
    }

    private static void writeCharge(final ItemStack stack, final double value) {
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        final CompoundTag root = customData == null ? new CompoundTag() : customData.copyTag();
        final CompoundTag data = root.getCompound(DATA_TAG);
        data.putDouble(CHARGE_TAG, value);
        root.put(DATA_TAG, data);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }
}
