package li.cil.oc.common;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

public final class WrenchTools {
    private static final Set<Method> USAGES = new LinkedHashSet<>();
    private static final Set<Method> CHECKS = new LinkedHashSet<>();

    public static void addUsage(final Method method) {
        if (method != null) {
            USAGES.add(method);
        }
    }

    public static void addCheck(final Method method) {
        if (method != null) {
            CHECKS.add(method);
        }
    }

    public static boolean isWrench(final ItemStack stack) {
        return stack != null && !stack.isEmpty() && CHECKS.stream().anyMatch(method -> invokeBoolean(method, stack));
    }

    public static boolean holdsApplicableWrench(final Player player, final BlockPos pos) {
        if (player == null || player.getMainHandItem().isEmpty()) {
            return false;
        }
        return USAGES.stream().anyMatch(method -> invokeBoolean(method, player, pos, Boolean.FALSE));
    }

    public static void wrenchUsed(final Player player, final BlockPos pos) {
        if (player == null || player.getMainHandItem().isEmpty()) {
            return;
        }
        USAGES.forEach(method -> invokeBoolean(method, player, pos, Boolean.TRUE));
    }

    private static boolean invokeBoolean(final Method method, final Object... args) {
        try {
            final Object result = method.invoke(null, args);
            return result instanceof Boolean value && value;
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
            NeoOpenComputers.LOGGER.warn("Error invoking wrench callback {}.", method.getName(), e);
            return false;
        }
    }

    private WrenchTools() {
    }
}
