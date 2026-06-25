package li.cil.oc.common;

import li.cil.oc.api.driver.item.Chargeable;
import li.cil.oc.common.item.BatteryUpgradeItem;
import li.cil.oc.common.item.TabletItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class ChargeableItemEnergyStorage implements IEnergyStorage {
    private final ItemStack stack;
    private final Chargeable chargeable;

    public ChargeableItemEnergyStorage(final ItemStack stack, final Chargeable chargeable) {
        this.stack = stack;
        this.chargeable = chargeable;
    }

    public static IEnergyStorage create(final ItemStack stack, final Void context) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() instanceof Chargeable chargeable && chargeable.canCharge(stack)) {
            return new ChargeableItemEnergyStorage(stack, chargeable);
        }
        return null;
    }

    @Override
    public int receiveEnergy(final int maxReceive, final boolean simulate) {
        if (maxReceive <= 0 || !canReceive()) {
            return 0;
        }
        return ModSettings.toForgeEnergy(chargeable.charge(stack, ModSettings.fromForgeEnergy(maxReceive), simulate));
    }

    @Override
    public int extractEnergy(final int maxExtract, final boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return ModSettings.toForgeEnergy(chargeStored());
    }

    @Override
    public int getMaxEnergyStored() {
        return ModSettings.toForgeEnergy(maxCharge());
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return chargeable.canCharge(stack);
    }

    private double chargeStored() {
        if (stack.getItem() instanceof BatteryUpgradeItem battery) {
            return battery.chargeStored(stack);
        }
        if (stack.getItem() instanceof TabletItem tablet) {
            return tablet.getCharge(stack);
        }
        return 0D;
    }

    private double maxCharge() {
        if (stack.getItem() instanceof BatteryUpgradeItem battery) {
            return battery.maxCharge();
        }
        if (stack.getItem() instanceof TabletItem tablet) {
            return tablet.maxCharge(stack);
        }
        return 0D;
    }
}
