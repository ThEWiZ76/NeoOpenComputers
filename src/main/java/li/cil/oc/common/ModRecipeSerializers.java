package li.cil.oc.common;

import com.mojang.serialization.MapCodec;
import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.recipe.LinkedCardRecipe;
import li.cil.oc.common.recipe.ColorizeRecipe;
import li.cil.oc.common.recipe.DecolorizeRecipe;
import li.cil.oc.common.recipe.LootDiskCyclingRecipe;
import li.cil.oc.common.recipe.LootRecraftingCondition;
import li.cil.oc.common.recipe.LuaBiosRecipe;
import li.cil.oc.common.recipe.NavigationUpgradeRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModRecipeSerializers {
    private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
        DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, NeoOpenComputers.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, NeoOpenComputers.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ColorizeRecipe>> COLORIZE_CABLE = SERIALIZERS.register(
        "colorize_cable", () -> new SimpleCraftingRecipeSerializer<>(category -> new ColorizeRecipe(category, ModItems.CABLE.get(), ModRecipeSerializers.COLORIZE_CABLE.get())));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DecolorizeRecipe>> DECOLORIZE_CABLE = SERIALIZERS.register(
        "decolorize_cable", () -> new SimpleCraftingRecipeSerializer<>(category -> new DecolorizeRecipe(category, ModItems.CABLE.get(), ModRecipeSerializers.DECOLORIZE_CABLE.get())));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ColorizeRecipe>> COLORIZE_HOVER_BOOTS = SERIALIZERS.register(
        "colorize_hover_boots", () -> new SimpleCraftingRecipeSerializer<>(category -> new ColorizeRecipe(category, ModItems.HOVER_BOOTS.get(), ModRecipeSerializers.COLORIZE_HOVER_BOOTS.get())));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DecolorizeRecipe>> DECOLORIZE_HOVER_BOOTS = SERIALIZERS.register(
        "decolorize_hover_boots", () -> new SimpleCraftingRecipeSerializer<>(category -> new DecolorizeRecipe(category, ModItems.HOVER_BOOTS.get(), ModRecipeSerializers.DECOLORIZE_HOVER_BOOTS.get())));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<LinkedCardRecipe>> LINKED_CARD = SERIALIZERS.register(
        ModContentIds.LINKED_CARD,
        LinkedCardRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<LootDiskCyclingRecipe>> LOOT_DISK_CYCLING = SERIALIZERS.register(
        "loot_disk_cycling",
        () -> new SimpleCraftingRecipeSerializer<>(LootDiskCyclingRecipe::new));

    static {
        CONDITIONS.register("loot_recrafting", () -> LootRecraftingCondition.CODEC);
    }

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<LuaBiosRecipe>> LUA_BIOS = SERIALIZERS.register(
        "lua_bios",
        () -> new SimpleCraftingRecipeSerializer<>(LuaBiosRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<NavigationUpgradeRecipe>> NAVIGATION_UPGRADE = SERIALIZERS.register(
        ModContentIds.NAVIGATION_UPGRADE,
        NavigationUpgradeRecipe.Serializer::new);

    public static void register(final IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
        CONDITIONS.register(modEventBus);
    }

    private ModRecipeSerializers() {
    }
}
