package li.cil.oc.common.recipe;

import li.cil.oc.api.API;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ModRecipeSerializers;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class LuaBiosRecipe extends CustomRecipe {
    public LuaBiosRecipe(final CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(final CraftingInput input, final Level level) {
        return input.ingredientCount() == 2 && hasItem(input, ModItems.EEPROM.get().asItem()) && hasItem(input, ModItems.MANUAL.get().asItem());
    }

    @Override
    public ItemStack assemble(final CraftingInput input, final HolderLookup.Provider registries) {
        if (!matches(input, null)) {
            return ItemStack.EMPTY;
        }
        final ItemInfo luaBios = API.items == null ? null : API.items.get("luabios");
        return luaBios == null ? ItemStack.EMPTY : luaBios.createItemStack(1);
    }

    @Override
    public boolean canCraftInDimensions(final int width, final int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.LUA_BIOS.get();
    }

    private static boolean hasItem(final CraftingInput input, final net.minecraft.world.item.Item item) {
        for (int i = 0; i < input.size(); i++) {
            if (input.getItem(i).is(item)) {
                return true;
            }
        }
        return false;
    }
}
