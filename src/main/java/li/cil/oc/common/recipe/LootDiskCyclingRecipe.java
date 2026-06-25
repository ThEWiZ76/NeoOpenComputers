package li.cil.oc.common.recipe;

import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ModLootDisks;
import li.cil.oc.common.ModRecipeSerializers;
import li.cil.oc.common.WrenchTools;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.List;

public final class LootDiskCyclingRecipe extends CustomRecipe {
    public LootDiskCyclingRecipe(final CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(final CraftingInput input, final Level level) {
        return input.ingredientCount() == 2 && lootDisk(input) != null && wrench(input) != null;
    }

    @Override
    public ItemStack assemble(final CraftingInput input, final HolderLookup.Provider registries) {
        final ItemStack disk = lootDisk(input);
        if (disk == null) {
            return ItemStack.EMPTY;
        }
        final String factoryId = factoryId(disk);
        final List<ItemStack> disks = ModLootDisks.bundledStacksForCycling();
        if (disks.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int index = -1;
        for (int i = 0; i < disks.size(); i++) {
            if (factoryId(disks.get(i)).equals(factoryId)) {
                index = i;
                break;
            }
        }
        final int next = (index + 1) % disks.size();
        return disks.get(next).copy();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(final CraftingInput input) {
        final NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++) {
            final ItemStack stack = input.getItem(i);
            if (WrenchTools.isWrench(stack)) {
                remaining.set(i, stack.copy());
            } else if (stack.hasCraftingRemainingItem()) {
                remaining.set(i, stack.getCraftingRemainingItem());
            }
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(final int width, final int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.LOOT_DISK_CYCLING.get();
    }

    private static ItemStack lootDisk(final CraftingInput input) {
        return input.items().stream().filter(LootDiskCyclingRecipe::isLootDisk).findFirst().orElse(null);
    }

    private static ItemStack wrench(final CraftingInput input) {
        return input.items().stream().filter(WrenchTools::isWrench).findFirst().orElse(null);
    }

    private static boolean isLootDisk(final ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(ModItems.FLOPPY.get())) {
            return false;
        }
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }
        final CompoundTag tag = customData.copyTag();
        return tag.getBoolean(ItemRegistry.FLOPPY_RECIPE_CYCLING_TAG) && !tag.getString(ItemRegistry.FLOPPY_FACTORY_ID_TAG).isBlank();
    }

    private static String factoryId(final ItemStack stack) {
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? "" : customData.copyTag().getString(ItemRegistry.FLOPPY_FACTORY_ID_TAG);
    }
}
