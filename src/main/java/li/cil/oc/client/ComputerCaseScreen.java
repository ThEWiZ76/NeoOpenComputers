package li.cil.oc.client;

import li.cil.oc.common.menu.ComputerCaseMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ComputerCaseScreen extends AbstractContainerScreen<ComputerCaseMenu> {
    public ComputerCaseScreen(final ComputerCaseMenu menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(final GuiGraphics guiGraphics, final float partialTick, final int mouseX, final int mouseY) {
        final int left = leftPos;
        final int top = topPos;
        guiGraphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFF2E3440);
        guiGraphics.fill(left + 7, top + 16, left + 169, top + 76, 0xFF3B4252);
        drawSlot(guiGraphics, left + 79, top + 16);
        drawSlot(guiGraphics, left + 61, top + 43);
        drawSlot(guiGraphics, left + 97, top + 43);
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private static void drawSlot(final GuiGraphics guiGraphics, final int left, final int top) {
        guiGraphics.fill(left - 1, top - 1, left + 17, top + 17, 0xFF1F232B);
        guiGraphics.fill(left, top, left + 16, top + 16, 0xFF4C566A);
    }
}
