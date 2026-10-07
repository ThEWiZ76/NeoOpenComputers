package li.cil.oc.common;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;

/** Resolves native dyes and the modern equivalent of upstream ore-dictionary dye colors. */
public final class DyeColors {
    public static DyeColor colorOf(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        if (stack.getItem() instanceof DyeItem dye) return dye.getDyeColor();
        // Include BLACK: NeoForge 21.1's DyeColor.getColor stops before its tag.
        for (final var color : DyeColor.values()) {
            if (stack.is(color.getTag())) return color;
        }
        return null;
    }

    private DyeColors() {
    }
}
