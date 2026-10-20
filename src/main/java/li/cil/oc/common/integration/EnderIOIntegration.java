package li.cil.oc.common.integration;

import li.cil.oc.common.WrenchTools;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/** Published Ender IO wrench types remain behind the installed-mod boundary. */
public final class EnderIOIntegration {
    private EnderIOIntegration() { }

    private static boolean loaded() { return ModList.get() != null && ModList.get().isLoaded("enderio"); }

    public static void register() {
        if (!loaded()) return;
        try {
            WrenchTools.addCheck(EnderIOIntegration.class.getMethod("isWrench", ItemStack.class));
            WrenchTools.addUsage(EnderIOIntegration.class.getMethod("useWrench", Player.class, BlockPos.class, boolean.class));
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Missing Ender IO wrench callbacks", exception);
        }
    }

    public static boolean isWrench(ItemStack stack) {
        return loaded() && stack != null && !stack.isEmpty() && Loaded.isWrench(stack);
    }

    public static boolean useWrench(Player player, BlockPos pos, boolean changeDurability) {
        // The modern Yeta Wrench has no legacy canUse/used hook or durability to consume.
        return player != null && pos != null && isWrench(player.getMainHandItem());
    }

    private static final class Loaded {
        static boolean isWrench(ItemStack stack) { return stack.getItem() instanceof com.enderio.enderio.content.tools.YetaWrenchItem; }
    }
}
