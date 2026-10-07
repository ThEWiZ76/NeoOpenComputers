package li.cil.oc.common.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class DecolorizeRecipe extends CustomRecipe {
    private final Item target;
    private final RecipeSerializer<?> serializer;

    public DecolorizeRecipe(final CraftingBookCategory category, final Item target, final RecipeSerializer<?> serializer) {
        super(category);
        this.target = target;
        this.serializer = serializer;
    }

    @Override
    public boolean matches(final CraftingInput input, final Level level) {
        if (input.ingredientCount() != 2) return false;
        boolean cable = false;
        boolean water = false;
        for (final var stack : input.items()) {
            if (stack.isEmpty()) continue;
            if (stack.is(target)) cable = true;
            else if (stack.is(Items.WATER_BUCKET)) water = true;
            else return false;
        }
        return cable && water;
    }

    @Override
    public ItemStack assemble(final CraftingInput input, final HolderLookup.Provider registries) {
        if (!matches(input, null)) return ItemStack.EMPTY;
        for (final var stack : input.items()) {
            if (stack.is(target)) {
                final var result = stack.copyWithCount(1);
                result.remove(DataComponents.DYED_COLOR);
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(final int width, final int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return serializer;
    }
}
