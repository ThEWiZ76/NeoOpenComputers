package li.cil.oc.common.template;

import li.cil.oc.common.ModSettings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Ordinary crafting recovery; explicit disassembler templates remain the host's first choice. */
public final class DisassemblerIngredients {
    private static final Map<RecipeManager, Index> INDICES = new WeakHashMap<>();
    private static final Comparator<ItemStack> ALTERNATIVES = Comparator
        .comparing((ItemStack stack) -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
        .thenComparingInt(ItemStack::getDamageValue);

    public static ItemStack[] recover(final Level level, final ItemStack stack) {
        if (level == null || !DisassemblerTemplates.isAllowed(stack)
            || !ModSettings.disassembleAllTheThings() && li.cil.oc.api.Items.get(stack) == null) return new ItemStack[0];
        return forTemplate(level, stack);
    }

    /** Selected templates may accept foreign items even when ordinary foreign recovery is disabled. */
    public static ItemStack[] forTemplate(final Level level, final ItemStack stack) {
        if (level == null || !DisassemblerTemplates.isAllowed(stack)) return new ItemStack[0];
        final var blacklist = new HashSet<>(ModSettings.disassemblerInputBlacklist());
        for (var holder : index(level).byItem.getOrDefault(stack.getItem(), List.of())) {
            final var recipe = holder.value();
            final var result = recipe.getResultItem(level.registryAccess());
            if (result.isEmpty() || result.getDamageValue() != stack.getDamageValue()) continue;
            final int count = result.getCount();
            final var inputs = new ArrayList<ItemStack>();
            boolean blocked = false;
            for (Ingredient ingredient : recipe.getIngredients()) {
                final var selected = Arrays.stream(ingredient.getItems()).filter(item -> !item.isEmpty()).min(ALTERNATIVES).orElse(ItemStack.EMPTY);
                // Upstream filters fractional inputs before merging, and never returns fluid buckets.
                if (selected.isEmpty() || selected.getCount() / count == 0 || selected.getItem() instanceof BucketItem) continue;
                final var name = selected.getItem() instanceof BlockItem block
                    ? BuiltInRegistries.BLOCK.getKey(block.getBlock()) : BuiltInRegistries.ITEM.getKey(selected.getItem());
                if (blacklist.contains(name.toString())) { blocked = true; break; }
                inputs.add(selected.copy());
            }
            if (blocked) continue;
            if (inputs.stream().anyMatch(input -> input.is(stack.getItem()) && input.getDamageValue() == stack.getDamageValue())) return new ItemStack[0];
            final var merged = new ArrayList<ItemStack>();
            for (var input : inputs) {
                // Modern data components can identify different ingredient variants of the same item.
                final var existing = merged.stream().filter(item -> ItemStack.isSameItemSameComponents(item, input)).findFirst();
                if (existing.isPresent()) existing.get().grow(input.getCount());
                else merged.add(input);
            }
            final var outputs = new ArrayList<ItemStack>();
            for (var input : merged) {
                final int amount = input.getCount() / count;
                for (int i = 0; i < amount; i++) outputs.add(input.copyWithCount(1));
            }
            return outputs.toArray(ItemStack[]::new);
        }
        return new ItemStack[0];
    }

    private static synchronized Index index(final Level level) {
        final var manager = level.getRecipeManager();
        // RecipeManager replaces its immutable map on reload/replaceRecipes, including same-ID edits.
        final var source = manager.getRecipes();
        final var cached = INDICES.get(manager);
        if (cached != null && cached.source == source) return cached;
        final var byItem = new HashMap<Item, List<RecipeHolder<?>>>();
        for (var holder : source) {
            if (!(holder.value() instanceof ShapedRecipe || holder.value() instanceof ShapelessRecipe)) continue;
            if (holder.value().getType() != RecipeType.CRAFTING || holder.value().isSpecial()) continue;
            final var result = holder.value().getResultItem(level.registryAccess());
            if (!result.isEmpty()) byItem.computeIfAbsent(result.getItem(), ignored -> new ArrayList<>()).add(holder);
        }
        byItem.values().forEach(recipes -> recipes.sort(Comparator.comparing(recipe -> recipe.id().toString())));
        final var index = new Index(source, byItem);
        INDICES.put(manager, index);
        return index;
    }

    private record Index(Collection<RecipeHolder<?>> source, Map<Item, List<RecipeHolder<?>>> byItem) { }

    private DisassemblerIngredients() { }
}
