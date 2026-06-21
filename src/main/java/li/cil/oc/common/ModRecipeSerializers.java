package li.cil.oc.common;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.recipe.LinkedCardRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, NeoOpenComputers.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<LinkedCardRecipe>> LINKED_CARD = SERIALIZERS.register(
        ModContentIds.LINKED_CARD,
        LinkedCardRecipe.Serializer::new);

    public static void register(final IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
    }

    private ModRecipeSerializers() {
    }
}
