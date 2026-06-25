package li.cil.oc.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;

public final class ModWrenches {
    private static boolean registered;

    public static void registerDefaults() {
        if (registered) {
            return;
        }
        registered = true;
        WrenchTools.addCheck(method("isWrench", ItemStack.class));
        WrenchTools.addUsage(method("useWrench", Player.class, BlockPos.class, boolean.class));
    }

    public static boolean isWrench(final ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof li.cil.oc.api.internal.Wrench;
    }

    public static boolean useWrench(final Player player, final BlockPos pos, final boolean changeDurability) {
        if (player == null || pos == null) {
            return false;
        }
        final ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof li.cil.oc.api.internal.Wrench wrench)) {
            return false;
        }
        return wrench.useWrenchOnBlock(player, player.level(), pos, !changeDurability);
    }

    private static Method method(final String name, final Class<?>... parameterTypes) {
        try {
            return ModWrenches.class.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Missing default wrench callback " + name, e);
        }
    }

    private ModWrenches() {
    }
}
