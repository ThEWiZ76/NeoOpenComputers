package li.cil.oc.client.integration.jei;

import com.mojang.datafixers.util.Pair;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.recipe.LootDiskCyclingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

final class LootDiskCyclingExtension implements ICraftingCategoryExtension<LootDiskCyclingRecipe> {
    private final Supplier<? extends Collection<ItemStack>> ingredients;
    private final Supplier<Level> level;

    LootDiskCyclingExtension(Supplier<? extends Collection<ItemStack>> ingredients, Supplier<Level> level) {
        this.ingredients = ingredients; this.level = level;
    }

    @Override public boolean isHandled(RecipeHolder<LootDiskCyclingRecipe> recipe) {
        final var world = level.get();
        return world != null && !pairs(recipe, world).isEmpty();
    }

    @Override public void setRecipe(RecipeHolder<LootDiskCyclingRecipe> recipe, IRecipeLayoutBuilder builder, ICraftingGridHelper grid, IFocusGroup focuses) {
        final var world = level.get();
        if (world == null) return;
        final var pairs = pairs(recipe, world);
        if (pairs.isEmpty()) return;
        final var inputs = pairs.stream().map(Pair::getFirst).toList();
        final var outputs = pairs.stream().map(Pair::getSecond).toList();
        final var slots = grid.createAndSetInputs(builder, List.of(inputs, List.of(new ItemStack(ModItems.WRENCH.get()))), 0, 0);
        final var output = grid.createAndSetOutputs(builder, outputs);
        builder.createFocusLink(slots.getFirst(), output);
    }

    private List<Pair<ItemStack, ItemStack>> pairs(RecipeHolder<LootDiskCyclingRecipe> holder, Level world) {
        final var pairs = new ArrayList<Pair<ItemStack, ItemStack>>();
        for (final var candidate : ingredients.get()) {
            final var input = CraftingInput.of(2, 1, List.of(candidate, new ItemStack(ModItems.WRENCH.get())));
            if (!holder.value().matches(input, world)) continue;
            final var output = holder.value().assemble(input, world.registryAccess());
            if (!output.isEmpty()) pairs.add(Pair.of(candidate.copy(), output));
        }
        return List.copyOf(pairs);
    }
}
