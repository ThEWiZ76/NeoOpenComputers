package li.cil.oc.common;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class InkProviders {
    private static final List<Method> PROVIDERS = new ArrayList<>();

    public static void add(final Method provider) {
        if (provider != null) {
            PROVIDERS.add(provider);
        }
    }

    public static int inkValue(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        for (final Method provider : PROVIDERS) {
            final int value = invoke(provider, stack);
            if (value > 0) {
                return value;
            }
        }
        return 0;
    }

    private static int invoke(final Method provider, final ItemStack stack) {
        try {
            final Object result = provider.invoke(null, stack);
            return result instanceof Number number ? Math.max(0, number.intValue()) : 0;
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
            NeoOpenComputers.LOGGER.warn("Error invoking ink provider {}.", provider.getName(), e);
            return 0;
        }
    }

    private InkProviders() {
    }
}
