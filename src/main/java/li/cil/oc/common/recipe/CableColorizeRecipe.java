package li.cil.oc.common.recipe;

import li.cil.oc.common.ModItems;
import li.cil.oc.common.DyeColors;
import li.cil.oc.common.ModRecipeSerializers;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import java.util.ArrayList;

public final class CableColorizeRecipe extends CustomRecipe {
    public CableColorizeRecipe(final CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(final CraftingInput input, final Level level) {
        int cables = 0;
        int dyes = 0;
        for (final var stack : input.items()) {
            if (stack.isEmpty()) continue;
            if (stack.is(ModItems.CABLE.get())) cables++;
            else if (DyeColors.colorOf(stack) != null) dyes++;
            else return false;
        }
        return cables == 1 && dyes > 0;
    }

    @Override
    public ItemStack assemble(final CraftingInput input, final HolderLookup.Provider registries) {
        if (!matches(input, null)) return ItemStack.EMPTY;
        ItemStack cable = ItemStack.EMPTY;
        final var dyes = new ArrayList<DyeItem>();
        for (final var stack : input.items()) {
            if (stack.isEmpty()) continue;
            if (stack.is(ModItems.CABLE.get())) cable = stack;
            else dyes.add(DyeItem.byColor(DyeColors.colorOf(stack)));
        }
        // Vanilla uses the same brightness-preserving blend, including the previous item color.
        return DyedItemColor.applyDyes(cable, dyes);
    }

    @Override
    public boolean canCraftInDimensions(final int width, final int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.COLORIZE_CABLE.get();
    }
}
