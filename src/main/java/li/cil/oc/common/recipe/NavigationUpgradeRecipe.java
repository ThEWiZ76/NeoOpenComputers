package li.cil.oc.common.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import li.cil.oc.common.ModRecipeSerializers;
import li.cil.oc.common.item.NavigationUpgradeItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

public final class NavigationUpgradeRecipe extends ShapedRecipe {
    private final ItemStack result;
    private final boolean showNotification;

    public NavigationUpgradeRecipe(
        final String group,
        final CraftingBookCategory category,
        final ShapedRecipePattern pattern,
        final ItemStack result,
        final boolean showNotification) {
        super(group, category, pattern, result, showNotification);
        this.result = result;
        this.showNotification = showNotification;
    }

    @Override
    public ItemStack assemble(final CraftingInput input, final HolderLookup.Provider registries) {
        final ItemStack stack = super.assemble(input, registries);
        for (int i = 0; i < input.size(); i++) {
            final ItemStack ingredient = input.getItem(i);
            if (!ingredient.isEmpty() && ingredient.has(DataComponents.MAP_ID)) {
                NavigationUpgradeItem.copyMapData(stack, ingredient, null);
                break;
            }
        }
        return stack;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.NAVIGATION_UPGRADE.get();
    }

    public static final class Serializer implements RecipeSerializer<NavigationUpgradeRecipe> {
        private static final MapCodec<NavigationUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(NavigationUpgradeRecipe::getGroup),
                    CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(NavigationUpgradeRecipe::category),
                    ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.pattern),
                    ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
                    Codec.BOOL.optionalFieldOf("show_notification", Boolean.TRUE).forGetter(recipe -> recipe.showNotification)
                )
                .apply(instance, NavigationUpgradeRecipe::new)
        );
        private static final StreamCodec<RegistryFriendlyByteBuf, NavigationUpgradeRecipe> STREAM_CODEC = StreamCodec.of(
            NavigationUpgradeRecipe.Serializer::toNetwork,
            NavigationUpgradeRecipe.Serializer::fromNetwork);

        @Override
        public MapCodec<NavigationUpgradeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, NavigationUpgradeRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static NavigationUpgradeRecipe fromNetwork(final RegistryFriendlyByteBuf buffer) {
            final String group = buffer.readUtf();
            final CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
            final ShapedRecipePattern pattern = ShapedRecipePattern.STREAM_CODEC.decode(buffer);
            final ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
            final boolean showNotification = buffer.readBoolean();
            return new NavigationUpgradeRecipe(group, category, pattern, result, showNotification);
        }

        private static void toNetwork(final RegistryFriendlyByteBuf buffer, final NavigationUpgradeRecipe recipe) {
            buffer.writeUtf(recipe.getGroup());
            buffer.writeEnum(recipe.category());
            ShapedRecipePattern.STREAM_CODEC.encode(buffer, recipe.pattern);
            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
            buffer.writeBoolean(recipe.showNotification);
        }
    }
}
