package li.cil.oc.common.integration;

import li.cil.oc.common.DriverRegistry;
import li.cil.oc.common.WrenchTools;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/** Optional Railcraft worldspike and crowbar interoperability. */
public final class RailcraftIntegration {
    private RailcraftIntegration() { }

    private static boolean loaded() { return ModList.get() != null && ModList.get().isLoaded("railcraft"); }

    public static void register(DriverRegistry registry) {
        if (!loaded()) return;
        Loaded.register(registry);
        try {
            WrenchTools.addCheck(RailcraftIntegration.class.getMethod("isWrench", ItemStack.class));
            WrenchTools.addUsage(RailcraftIntegration.class.getMethod("useWrench", Player.class, BlockPos.class, boolean.class));
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Missing Railcraft wrench callbacks", exception);
        }
    }

    public static boolean isWrench(ItemStack stack) {
        return loaded() && stack != null && !stack.isEmpty() && Loaded.isWrench(stack);
    }

    public static boolean useWrench(Player player, BlockPos pos, boolean changeDurability) {
        return loaded() && player != null && pos != null && Loaded.useWrench(player, pos, changeDurability);
    }

    private static final class Loaded {
        static void register(DriverRegistry registry) { registry.add(new li.cil.oc.common.integration.railcraft.RailcraftBlockDriver()); }
        static boolean isWrench(ItemStack stack) { return stack.getItem() instanceof mods.railcraft.api.item.Crowbar; }
        static boolean useWrench(Player player, BlockPos pos, boolean changeDurability) {
            final var stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof mods.railcraft.api.item.Crowbar crowbar)) return false;
            if (!changeDurability) return crowbar.canWhack(player, InteractionHand.MAIN_HAND, stack, pos);
            if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) return false;
            crowbar.onWhack(serverPlayer, InteractionHand.MAIN_HAND, stack, pos);
            return true;
        }
    }
}
