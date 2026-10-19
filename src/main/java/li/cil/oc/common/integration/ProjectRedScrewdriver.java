package li.cil.oc.common.integration;

import li.cil.oc.common.WrenchTools;
import mrtjp.projectred.api.IScrewdriver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/** Recognizes ProjectRed's screwdriver API through the existing OC wrench registry. */
public final class ProjectRedScrewdriver {
    private static boolean registered;

    public static void register() {
        if (registered || !ModList.get().isLoaded("projectred_core")) return;
        try {
            WrenchTools.addCheck(ProjectRedScrewdriver.class.getMethod("isWrench", ItemStack.class));
            WrenchTools.addUsage(ProjectRedScrewdriver.class.getMethod("useWrench", Player.class, BlockPos.class, boolean.class));
            registered = true;
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Missing ProjectRed screwdriver callbacks", exception);
        }
    }

    public static boolean isWrench(final ItemStack stack) {
        return registered && stack != null && !stack.isEmpty() && Loaded.isWrench(stack);
    }

    public static boolean useWrench(final Player player, final BlockPos pos, final boolean changeDurability) {
        return registered && player != null && pos != null && Loaded.useWrench(player, changeDurability);
    }

    private static final class Loaded {
        static boolean isWrench(ItemStack stack) { return stack.getItem() instanceof IScrewdriver; }

        static boolean useWrench(Player player, boolean changeDurability) {
            if (!(player.getMainHandItem().getItem() instanceof IScrewdriver screwdriver)
                || !screwdriver.canUse(player, InteractionHand.MAIN_HAND)) return false;
            if (changeDurability) screwdriver.damageScrewdriver(player, InteractionHand.MAIN_HAND);
            return true;
        }
    }

    private ProjectRedScrewdriver() {}
}
