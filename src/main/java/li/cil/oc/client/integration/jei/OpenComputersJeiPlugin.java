package li.cil.oc.client.integration.jei;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Manual;
import li.cil.oc.client.ItemSearch;
import li.cil.oc.common.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JEI discovers this class only when the optional client mod is installed. */
@JeiPlugin
public final class OpenComputersJeiPlugin implements IModPlugin {
    static final RecipeType<DocumentationCategory.Page> MANUAL = RecipeType.create(NeoOpenComputers.MODID, "manual", DocumentationCategory.Page.class);
    static final RecipeType<DocumentationCategory.Page> CALLBACKS = RecipeType.create(NeoOpenComputers.MODID, "api", DocumentationCategory.Page.class);
    private AutoCloseable stackFocusing;
    private AutoCloseable focusedInput;

    @Override public ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "jei"); }

    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        final var helper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
            new DocumentationCategory(MANUAL, "jei.neoopencomputers.manual", helper, new ItemStack(ModItems.MANUAL.get())),
            new DocumentationCategory(CALLBACKS, "jei.neoopencomputers.api", helper, new ItemStack(ModItems.TABLET.get())));
    }

    @Override public void registerRecipes(IRecipeRegistration registration) {
        final List<DocumentationCategory.Page> manual = new ArrayList<>();
        final List<DocumentationCategory.Page> callbacks = new ArrayList<>();
        final var font = Minecraft.getInstance().font;
        for (final ItemStack stack : registration.getIngredientManager().getAllIngredients(VanillaTypes.ITEM_STACK)) {
            final String path = Manual.pathFor(stack);
            if (path != null) manual.add(new DocumentationCategory.Page(stack.copy(), List.of(), path));
            final var environments = Driver.environmentsFor(stack);
            if (environments == null) continue;
            final var entries = CallbackDocumentation.discover(environments);
            for (final var lines : CallbackDocumentation.pages(entries, (text, width) -> font.getSplitter().splitLines(text, width, net.minecraft.network.chat.Style.EMPTY)
                .stream().map(net.minecraft.network.chat.FormattedText::getString).toList())) {
                callbacks.add(new DocumentationCategory.Page(stack.copy(), lines, null));
            }
        }
        registration.addRecipes(MANUAL, manual);
        registration.addRecipes(CALLBACKS, callbacks);
    }

    @Override public void onRuntimeAvailable(IJeiRuntime runtime) {
        onRuntimeUnavailable();
        stackFocusing = ItemSearch.registerStackFocusing((screen, x, y) -> Optional.ofNullable(runtime.getIngredientListOverlay().getIngredientUnderMouse(VanillaTypes.ITEM_STACK)));
        focusedInput = ItemSearch.registerFocusedInput(() -> runtime.getIngredientListOverlay().hasKeyboardFocus());
    }

    @Override public void onRuntimeUnavailable() {
        close(stackFocusing); close(focusedInput);
        stackFocusing = null; focusedInput = null;
    }

    private static void close(AutoCloseable subscription) {
        if (subscription == null) return;
        try { subscription.close(); }
        catch (Exception failure) { NeoOpenComputers.LOGGER.warn("Cannot detach JEI input bridge", failure); }
    }
}
