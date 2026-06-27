package li.cil.oc.client;

import li.cil.oc.common.menu.AdapterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class AdapterScreen extends AbstractContainerScreen<AdapterMenu> {
    public AdapterScreen(final AdapterMenu menu, final Inventory playerInventory, final Component title) {
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
        drawSlot(guiGraphics, left + AdapterMenu.adapterSlotX() - 1, top + AdapterMenu.adapterSlotY() - 1);
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
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
