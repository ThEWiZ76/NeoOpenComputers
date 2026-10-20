package li.cil.oc.client.integration.jei;

import li.cil.oc.api.Manual;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;

final class DocumentationCategory implements IRecipeCategory<DocumentationCategory.Page> {
    record Page(ItemStack stack, List<String> lines, String path) {}
    private final RecipeType<Page> type;
    private final Component title;
    private final IDrawable icon;

    DocumentationCategory(RecipeType<Page> type, String title, IGuiHelper helper, ItemStack icon) {
        this.type = type; this.title = Component.translatable(title);
        this.icon = helper.createDrawableItemStack(icon);
    }

    @Override public RecipeType<Page> getRecipeType() { return type; }
    @Override public Component getTitle() { return title; }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 160; }
    @Override public int getHeight() { return 125; }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, Page page, IFocusGroup focuses) {
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(page.stack());
    }

    @Override public void draw(Page page, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        final var font = Minecraft.getInstance().font;
        for (int i = 0; i < page.lines().size(); i++) graphics.drawString(font, page.lines().get(i), 4, 4 + i * 10, 0x333333, false);
        if (page.path() != null) {
            final boolean hover = mouseX >= 30 && mouseX < 130 && mouseY >= 10 && mouseY < 30;
            graphics.fill(30, 10, 130, 30, hover ? 0xFF687788 : 0xFF485360);
            graphics.drawCenteredString(font, Component.translatable("jei.neoopencomputers.open_manual"), 80, 16, 0xFFFFFF);
        }
    }

    @Override public void createRecipeExtras(IRecipeExtrasBuilder builder, Page page, IFocusGroup focuses) {
        if (page.path() == null) return;
        builder.addInputHandler(new ManualLinkInput(() -> {
            final var minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return;
            minecraft.player.closeContainer();
            Manual.navigate(page.path()); Manual.openFor(minecraft.player);
        }));
    }

    static final class ManualLinkInput implements IJeiInputHandler {
        private final Runnable open;
        ManualLinkInput(Runnable open) { this.open = open; }
        @Override public ScreenRectangle getArea() { return new ScreenRectangle(30, 10, 100, 20); }
        @Override public boolean handleInput(double mouseX, double mouseY, IJeiUserInput input) {
            if (!input.getKey().equals(com.mojang.blaze3d.platform.InputConstants.Type.MOUSE.getOrCreate(GLFW.GLFW_MOUSE_BUTTON_LEFT))) return false;
            if (!input.isSimulate()) open.run();
            return true;
        }
    }
}
