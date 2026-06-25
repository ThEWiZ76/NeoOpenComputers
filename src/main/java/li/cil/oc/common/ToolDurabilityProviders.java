package li.cil.oc.common;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

public final class ToolDurabilityProviders {
    private static final List<Method> PROVIDERS = new ArrayList<>();

    public static void add(final Method provider) {
        if (provider != null) {
            PROVIDERS.add(provider);
        }
    }

    public static OptionalDouble getDurability(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return OptionalDouble.empty();
        }
        for (final Method provider : PROVIDERS) {
            final double durability = invoke(provider, stack);
            if (!Double.isNaN(durability)) {
                return OptionalDouble.of(durability);
            }
        }
        if (stack.isDamageableItem() && stack.getMaxDamage() > 0) {
            return OptionalDouble.of(1D - (double) stack.getDamageValue() / (double) stack.getMaxDamage());
        }
        return OptionalDouble.empty();
    }

    private static double invoke(final Method provider, final ItemStack stack) {
        try {
            final Object result = provider.invoke(null, stack);
            return result instanceof Number number ? number.doubleValue() : Double.NaN;
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
            NeoOpenComputers.LOGGER.warn("Error invoking tool durability provider {}.", provider.getName(), e);
            return Double.NaN;
        }
    }

    private ToolDurabilityProviders() {
    }
}
