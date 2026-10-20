package li.cil.oc.client.integration.jei;

import li.cil.oc.common.ModItems;
import li.cil.oc.common.WrenchTools;
import li.cil.oc.common.recipe.LootDiskCyclingRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Static JEI recipe indexes cannot see ingredients received after startup. */
final class LootDiskRecipeLookup implements ISimpleRecipeManagerPlugin<RecipeHolder<CraftingRecipe>> {
    private final Supplier<Level> level;
    private final Supplier<? extends Collection<ItemStack>> ingredients;

    LootDiskRecipeLookup(Supplier<Level> level, Supplier<? extends Collection<ItemStack>> ingredients) {
        this.level = level; this.ingredients = ingredients;
    }

    @Override public boolean isHandledInput(ITypedIngredient<?> input) { return !getRecipesForInput(input).isEmpty(); }
    @Override public boolean isHandledOutput(ITypedIngredient<?> output) { return !getRecipesForOutput(output).isEmpty(); }

    @Override public List<RecipeHolder<CraftingRecipe>> getRecipesForInput(ITypedIngredient<?> input) {
        final var stack = input.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
        return recipes(stack, false);
    }

    @Override public List<RecipeHolder<CraftingRecipe>> getRecipesForOutput(ITypedIngredient<?> output) {
        final var stack = output.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
        return recipes(stack, true);
    }

    @Override public List<RecipeHolder<CraftingRecipe>> getAllRecipes() {
        return recipes(new ItemStack(ModItems.WRENCH.get()), false);
    }

    private List<RecipeHolder<CraftingRecipe>> recipes(ItemStack focus, boolean output) {
        final var world = level.get();
        if (world == null || focus.isEmpty()) return List.of();
        return world.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
            .filter(holder -> holder.value() instanceof LootDiskCyclingRecipe)
            .filter(holder -> {
                final var candidates = !output && !WrenchTools.isWrench(focus) ? List.of(focus) : ingredients.get();
                return candidates.stream().anyMatch(candidate -> {
                    final var input = CraftingInput.of(2, 1, List.of(candidate, new ItemStack(ModItems.WRENCH.get())));
                    if (!holder.value().matches(input, world)) return false;
                    final var result = holder.value().assemble(input, world.registryAccess());
                    return !result.isEmpty() && (!output || sameDisk(focus, result));
                });
            }).toList();
    }

    private static boolean sameDisk(ItemStack first, ItemStack second) {
        return first.is(second.getItem()) && Objects.equals(ItemSubtypes.FLOPPY.getSubtypeData(first, UidContext.Recipe),
            ItemSubtypes.FLOPPY.getSubtypeData(second, UidContext.Recipe));
    }
}
