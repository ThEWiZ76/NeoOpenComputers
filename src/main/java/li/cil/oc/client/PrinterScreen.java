package li.cil.oc.client;

import li.cil.oc.common.menu.PrinterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class PrinterScreen extends AbstractContainerScreen<PrinterMenu> {
    private static final int MATERIAL_BAR_X = 40;
    private static final int MATERIAL_BAR_Y = 21;
    private static final int INK_BAR_X = 40;
    private static final int INK_BAR_Y = 53;
    private static final int RESOURCE_BAR_WIDTH = 62;
    private static final int RESOURCE_BAR_HEIGHT = 12;
    private static final int PROGRESS_BAR_X = 105;
    private static final int PROGRESS_BAR_Y = 20;
    private static final int PROGRESS_BAR_SIZE = 46;

    public PrinterScreen(final PrinterMenu menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
        imageHeight = 166;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(final GuiGraphics guiGraphics, final float partialTick, final int mouseX, final int mouseY) {
        final int left = leftPos;
        final int top = topPos;
        guiGraphics.blit(DiskDriveScreen.BACKGROUND_TEXTURE, left, top, 0, 0, imageWidth, imageHeight);
        for (int slot = 0; slot < PrinterMenu.PRINTER_SLOT_COUNT; slot++) {
            drawSlot(guiGraphics, left + PrinterMenu.printerSlotX(slot) - 1, top + PrinterMenu.printerSlotY(slot) - 1);
        }
        drawBar(guiGraphics, left + MATERIAL_BAR_X, top + MATERIAL_BAR_Y, RESOURCE_BAR_WIDTH, RESOURCE_BAR_HEIGHT, fillWidth(menu.amountMaterial(), menu.materialCapacity()), 0xFF81A1C1);
        drawBar(guiGraphics, left + INK_BAR_X, top + INK_BAR_Y, RESOURCE_BAR_WIDTH, RESOURCE_BAR_HEIGHT, fillWidth(menu.amountInk(), menu.inkCapacity()), 0xFF2E3440);
        drawProgress(guiGraphics, left + PROGRESS_BAR_X, top + PROGRESS_BAR_Y, menu.progress());
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        if (inRegion(mouseX, mouseY, MATERIAL_BAR_X, MATERIAL_BAR_Y, RESOURCE_BAR_WIDTH, RESOURCE_BAR_HEIGHT)) {
            guiGraphics.renderComponentTooltip(font, materialTooltip(menu.amountMaterial(), menu.materialCapacity()), mouseX, mouseY);
        } else if (inRegion(mouseX, mouseY, INK_BAR_X, INK_BAR_Y, RESOURCE_BAR_WIDTH, RESOURCE_BAR_HEIGHT)) {
            guiGraphics.renderComponentTooltip(font, inkTooltip(menu.amountInk(), menu.inkCapacity()), mouseX, mouseY);
        } else if (inRegion(mouseX, mouseY, PROGRESS_BAR_X, PROGRESS_BAR_Y, PROGRESS_BAR_SIZE, PROGRESS_BAR_SIZE)) {
            guiGraphics.renderComponentTooltip(font, progressTooltip(menu.progress()), mouseX, mouseY);
        }
    }

    public static List<Component> materialTooltip(final int amount, final int capacity) {
        return List.of(Component.translatable("gui.neoopencomputers.printer.material"), Component.literal(amount + "/" + capacity));
    }

    public static List<Component> inkTooltip(final int amount, final int capacity) {
        return List.of(Component.translatable("gui.neoopencomputers.printer.ink"), Component.literal(amount + "/" + capacity));
    }

    public static List<Component> progressTooltip(final int progress) {
        return List.of(Component.translatable("gui.neoopencomputers.printer.progress"), Component.literal(progress + "%"));
    }

    private boolean inRegion(final int mouseX, final int mouseY, final int x, final int y, final int width, final int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width && mouseY >= topPos + y && mouseY < topPos + y + height;
    }

    private static int fillWidth(final int amount, final int capacity) {
        if (capacity <= 0) {
            return 0;
        }
        return Math.clamp((int) Math.round(RESOURCE_BAR_WIDTH * (amount / (double) capacity)), 0, RESOURCE_BAR_WIDTH);
    }

    private static void drawBar(final GuiGraphics guiGraphics, final int left, final int top, final int width, final int height, final int fill, final int color) {
        guiGraphics.fill(left - 1, top - 1, left + width + 1, top + height + 1, 0xFF1F232B);
        guiGraphics.fill(left, top, left + width, top + height, 0xFF4C566A);
        if (fill > 0) {
            guiGraphics.fill(left, top, left + fill, top + height, color);
        }
    }

    private static void drawProgress(final GuiGraphics guiGraphics, final int left, final int top, final int progress) {
        guiGraphics.fill(left - 1, top - 1, left + PROGRESS_BAR_SIZE + 1, top + PROGRESS_BAR_SIZE + 1, 0xFF1F232B);
        guiGraphics.fill(left, top, left + PROGRESS_BAR_SIZE, top + PROGRESS_BAR_SIZE, 0xFF3B4252);
        final int fill = Math.clamp((int) Math.round(PROGRESS_BAR_SIZE * (progress / 100D)), 0, PROGRESS_BAR_SIZE);
        if (fill > 0) {
            guiGraphics.fill(left, top + PROGRESS_BAR_SIZE - fill, left + PROGRESS_BAR_SIZE, top + PROGRESS_BAR_SIZE, 0xFF88C0D0);
        }
    }

    private static void drawSlot(final GuiGraphics guiGraphics, final int left, final int top) {
        guiGraphics.blit(
            DiskDriveScreen.SLOT_TEXTURE,
            left,
            top,
            0,
            0,
            DiskDriveScreen.slotTextureWidth(),
            DiskDriveScreen.slotTextureHeight(),
            DiskDriveScreen.slotTextureWidth(),
            DiskDriveScreen.slotTextureHeight());
    }
}
