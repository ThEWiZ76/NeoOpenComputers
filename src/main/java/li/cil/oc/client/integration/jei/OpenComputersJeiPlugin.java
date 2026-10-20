package li.cil.oc.client.integration.jei;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Manual;
import li.cil.oc.client.ItemSearch;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.network.LootDiskClientState;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** JEI discovers this class only when the optional client mod is installed. */
@JeiPlugin
public final class OpenComputersJeiPlugin implements IModPlugin {
    static final RecipeType<DocumentationCategory.Page> MANUAL = RecipeType.create(NeoOpenComputers.MODID, "manual", DocumentationCategory.Page.class);
    // JEI sorts default categories by UID; keep the manual before its callback pages.
    static final RecipeType<DocumentationCategory.Page> CALLBACKS = RecipeType.create(NeoOpenComputers.MODID, "manual_callbacks", DocumentationCategory.Page.class);
    private AutoCloseable stackFocusing;
    private AutoCloseable focusedInput;
    private AutoCloseable runtimeDisks;
    private final Set<String> initiallyDocumentedFactories = new HashSet<>();

    @Override public ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "jei"); }

    @Override public void registerItemSubtypes(ISubtypeRegistration registration) {
        for (final var item : List.of(ModItems.ROBOT.get(), ModItems.MICROCONTROLLER_TIER1.get(), ModItems.MICROCONTROLLER_TIER2.get(),
            ModItems.MICROCONTROLLER_CREATIVE.get(), ModItems.DRONE.get(), ModItems.TABLET.get())) {
            registration.registerSubtypeInterpreter(item, ItemSubtypes.COMPONENTS);
        }
        registration.registerSubtypeInterpreter(ModItems.FLOPPY.get(), ItemSubtypes.FLOPPY);
    }

    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        final var helper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
            new DocumentationCategory(MANUAL, "jei.neoopencomputers.manual", helper, new ItemStack(ModItems.MANUAL.get())),
            new DocumentationCategory(CALLBACKS, "jei.neoopencomputers.api", helper, new ItemStack(ModItems.TABLET.get())));
    }

    @Override public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getCraftingCategory().addExtension(li.cil.oc.common.recipe.LootDiskCyclingRecipe.class,
            new LootDiskCyclingExtension(() -> registration.getJeiHelpers().getIngredientManager().getAllIngredients(VanillaTypes.ITEM_STACK),
                () -> ClientWorld.level()));
    }

    private static final class ClientWorld {
        static net.minecraft.world.level.Level level() { return Minecraft.getInstance().level; }
    }

    @Override public void registerAdvanced(IAdvancedRegistration registration) {
        registration.addTypedRecipeManagerPlugin(mezz.jei.api.constants.RecipeTypes.CRAFTING,
            new LootDiskRecipeLookup(ClientWorld::level,
                () -> registration.getJeiHelpers().getIngredientManager().getAllIngredients(VanillaTypes.ITEM_STACK)));
    }

    @Override public void registerRecipes(IRecipeRegistration registration) {
        initiallyDocumentedFactories.clear();
        final var stacks = registration.getIngredientManager().getAllIngredients(VanillaTypes.ITEM_STACK);
        for (final var stack : stacks) {
            final var factory = LootDiskClientState.factoryId(stack);
            if (factory != null) initiallyDocumentedFactories.add(factory);
        }
        final var pages = documentation(stacks);
        registration.addRecipes(MANUAL, pages.manual());
        registration.addRecipes(CALLBACKS, pages.callbacks());
    }

    record Documentation(List<DocumentationCategory.Page> manual, List<DocumentationCategory.Page> callbacks) {}

    private static Documentation documentation(Collection<ItemStack> stacks) {
        final var font = Minecraft.getInstance().font;
        return documentation(stacks, (text, width) -> font.getSplitter().splitLines(text, width, net.minecraft.network.chat.Style.EMPTY)
            .stream().map(net.minecraft.network.chat.FormattedText::getString).toList());
    }

    static Documentation documentation(Collection<ItemStack> stacks, java.util.function.BiFunction<String, Integer, List<String>> wrap) {
        final List<DocumentationCategory.Page> manual = new ArrayList<>();
        final List<DocumentationCategory.Page> callbacks = new ArrayList<>();
        for (final ItemStack stack : stacks) {
            final String path = Manual.pathFor(stack);
            if (path != null) manual.add(new DocumentationCategory.Page(stack.copy(), List.of(), path));
            final var environments = Driver.environmentsFor(stack);
            if (environments == null) continue;
            final var entries = CallbackDocumentation.discover(environments);
            for (final var lines : CallbackDocumentation.pages(entries, wrap)) {
                callbacks.add(new DocumentationCategory.Page(stack.copy(), lines, null));
            }
        }
        return new Documentation(List.copyOf(manual), List.copyOf(callbacks));
    }

    @Override public void onRuntimeAvailable(IJeiRuntime runtime) {
        onRuntimeUnavailable();
        stackFocusing = ItemSearch.registerStackFocusing((screen, x, y) -> Optional.ofNullable(runtime.getIngredientListOverlay().getIngredientUnderMouse(VanillaTypes.ITEM_STACK)));
        focusedInput = ItemSearch.registerFocusedInput(() -> runtime.getIngredientListOverlay().hasKeyboardFocus());
        final var ingredients = runtime.getIngredientManager();
        final var documented = new HashSet<>(initiallyDocumentedFactories);
        runtimeDisks = subscribeRuntimeDisks(() -> ingredients.getAllIngredients(VanillaTypes.ITEM_STACK),
            stacks -> {
                ingredients.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, stacks);
                final var newDisks = stacks.stream().filter(stack -> documented.add(LootDiskClientState.factoryId(stack))).toList();
                if (!newDisks.isEmpty()) {
                    final var pages = documentation(newDisks);
                    final var recipes = runtime.getRecipeManager();
                    if (!pages.manual().isEmpty()) recipes.addRecipes(MANUAL, pages.manual());
                    if (!pages.callbacks().isEmpty()) recipes.addRecipes(CALLBACKS, pages.callbacks());
                }
            });
    }

    @Override public void onRuntimeUnavailable() {
        close(stackFocusing); close(focusedInput); close(runtimeDisks);
        stackFocusing = null; focusedInput = null; runtimeDisks = null;
    }

    static AutoCloseable subscribeRuntimeDisks(Supplier<? extends Collection<ItemStack>> existing, Consumer<List<ItemStack>> add) {
        return LootDiskClientState.subscribe(() -> {
            final var current = existing.get();
            final var missing = LootDiskClientState.stacks().stream()
                .filter(stack -> current.stream().noneMatch(candidate -> ItemStack.isSameItemSameComponents(candidate, stack)))
                .toList();
            if (!missing.isEmpty()) add.accept(missing);
        });
    }

    private static void close(AutoCloseable subscription) {
        if (subscription == null) return;
        try { subscription.close(); }
        catch (Exception failure) { NeoOpenComputers.LOGGER.warn("Cannot detach JEI runtime bridge", failure); }
    }
}
