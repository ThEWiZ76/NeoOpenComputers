package li.cil.oc.client;

import li.cil.oc.common.component.TerminalScreenSnapshot;
import li.cil.oc.common.menu.TerminalMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class TerminalScreen extends AbstractContainerScreen<TerminalMenu> {
    private static final int LINE_HEIGHT = 9;
    private static final int TEXT_COLOR = 0xFFB8F4C8;

    public TerminalScreen(final TerminalMenu menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
        imageWidth = 248;
        imageHeight = 166;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelY = 1000;
    }

    @Override
    protected void renderBg(final GuiGraphics guiGraphics, final float partialTick, final int mouseX, final int mouseY) {
        final int left = leftPos;
        final int top = topPos;
        guiGraphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFF101820);
        guiGraphics.fill(left + 8, top + 18, left + imageWidth - 8, top + imageHeight - 8, 0xFF05080C);
        for (int row = 0; row < Math.min(menu.snapshot().height(), 15); row++) {
            final String line = snapshotLine(menu.snapshot(), row);
            if (!line.isBlank()) {
                guiGraphics.drawString(font, line, left + 12, top + 22 + row * LINE_HEIGHT, TEXT_COLOR, false);
            }
        }
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    static String snapshotLine(final TerminalScreenSnapshot snapshot, final int row) {
        return snapshot == null ? "" : snapshot.line(row);
    }
}
