package li.cil.oc.common.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.common.ModRecipeSerializers;
import li.cil.oc.common.item.LinkedCardItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

import java.util.UUID;

public final class LinkedCardRecipe extends ShapedRecipe {
    private final ItemStack result;
    private final boolean showNotification;

    public LinkedCardRecipe(
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
        final DriverItem driver = Driver.driverFor(stack);
        if (driver != null) {
            driver.dataTag(stack).putString(LinkedCardItem.TUNNEL_TAG, UUID.randomUUID().toString());
        }
        return stack;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.LINKED_CARD.get();
    }

    public static final class Serializer implements RecipeSerializer<LinkedCardRecipe> {
        private static final MapCodec<LinkedCardRecipe> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(LinkedCardRecipe::getGroup),
                    CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(LinkedCardRecipe::category),
                    ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.pattern),
                    ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
                    Codec.BOOL.optionalFieldOf("show_notification", Boolean.TRUE).forGetter(recipe -> recipe.showNotification)
                )
                .apply(instance, LinkedCardRecipe::new)
        );
        private static final StreamCodec<RegistryFriendlyByteBuf, LinkedCardRecipe> STREAM_CODEC = StreamCodec.of(
            LinkedCardRecipe.Serializer::toNetwork,
            LinkedCardRecipe.Serializer::fromNetwork);

        @Override
        public MapCodec<LinkedCardRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, LinkedCardRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static LinkedCardRecipe fromNetwork(final RegistryFriendlyByteBuf buffer) {
            final String group = buffer.readUtf();
            final CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
            final ShapedRecipePattern pattern = ShapedRecipePattern.STREAM_CODEC.decode(buffer);
            final ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
            final boolean showNotification = buffer.readBoolean();
            return new LinkedCardRecipe(group, category, pattern, result, showNotification);
        }

        private static void toNetwork(final RegistryFriendlyByteBuf buffer, final LinkedCardRecipe recipe) {
            buffer.writeUtf(recipe.getGroup());
            buffer.writeEnum(recipe.category());
            ShapedRecipePattern.STREAM_CODEC.encode(buffer, recipe.pattern);
            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
            buffer.writeBoolean(recipe.showNotification);
        }
    }
}
