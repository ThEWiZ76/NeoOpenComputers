package li.cil.oc.common;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class ItemCharges {
    private static final List<Provider> PROVIDERS = new ArrayList<>();

    public static void add(final Method canCharge, final Method charge) {
        if (canCharge != null && charge != null) {
            PROVIDERS.add(new Provider(canCharge, charge));
        }
    }

    public static boolean canCharge(final ItemStack stack) {
        return stack != null && !stack.isEmpty() && PROVIDERS.stream().anyMatch(provider -> provider.canCharge(stack));
    }

    public static double charge(final ItemStack stack, final double amount) {
        if (stack == null || stack.isEmpty()) {
            return amount;
        }
        return PROVIDERS.stream()
            .filter(provider -> provider.canCharge(stack))
            .findFirst()
            .map(provider -> provider.charge(stack, amount))
            .orElse(amount);
    }

    private record Provider(Method canCharge, Method charge) {
        private boolean canCharge(final ItemStack stack) {
            try {
                final Object result = canCharge.invoke(null, stack);
                return result instanceof Boolean value && value;
            } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
                NeoOpenComputers.LOGGER.warn("Error invoking item charge check {}.", canCharge.getName(), e);
                return false;
            }
        }

        private double charge(final ItemStack stack, final double amount) {
            try {
                final Object result = charge.invoke(null, stack, amount, Boolean.FALSE);
                return result instanceof Number number ? number.doubleValue() : amount;
            } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
                NeoOpenComputers.LOGGER.warn("Error invoking item charge provider {}.", charge.getName(), e);
                return amount;
            }
        }
    }

    private ItemCharges() {
    }
}
