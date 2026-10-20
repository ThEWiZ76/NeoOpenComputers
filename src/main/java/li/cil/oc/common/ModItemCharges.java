package li.cil.oc.common;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Chargeable;
import net.minecraft.world.item.ItemStack;

public final class ModItemCharges {
    private static boolean registered;

    public static void registerDefaults() {
        if (registered) {
            return;
        }
        registered = true;
        try {
            ItemCharges.add(
                ModItemCharges.class.getMethod("canCharge", ItemStack.class),
                ModItemCharges.class.getMethod("charge", ItemStack.class, double.class, boolean.class));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Missing default item charge callbacks", e);
        }
    }

    public static boolean canCharge(final ItemStack stack) {
        if (chargeable(stack) != null) return true;
        final var storage = forgeEnergy(stack);
        return storage != null && storage.canReceive();
    }

    public static double charge(final ItemStack stack, final double amount, final boolean simulate) {
        final Chargeable chargeable = chargeable(stack);
        if (chargeable == null) {
            final var storage = forgeEnergy(stack);
            if (storage == null || !storage.canReceive() || !(amount > 0)) return amount;
            final int requested = ModSettings.toForgeEnergy(amount);
            if (requested <= 0) return amount;
            return amount - ModSettings.fromForgeEnergy(storage.receiveEnergy(requested, simulate));
        }
        final double accepted = chargeable.charge(stack, amount, simulate);
        return amount - accepted;
    }

    private static net.neoforged.neoforge.energy.IEnergyStorage forgeEnergy(final ItemStack stack) {
        return stack == null || stack.isEmpty() ? null : stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
    }

    private static Chargeable chargeable(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() instanceof Chargeable chargeable && chargeable.canCharge(stack)) {
            return chargeable;
        }
        final DriverItem driver = Driver.driverFor(stack);
        if (driver instanceof Chargeable chargeable && chargeable.canCharge(stack)) {
            return chargeable;
        }
        return null;
    }

    private ModItemCharges() {
    }
}
