package li.cil.oc.client;

import li.cil.oc.common.menu.MicrocontrollerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class MicrocontrollerScreen extends AbstractContainerScreen<MicrocontrollerMenu> {
    public static final ResourceLocation BACKGROUND_TEXTURE = ComputerCaseScreen.BACKGROUND_TEXTURE;
    public static final ResourceLocation MICROCONTROLLER_TEXTURE = ComputerCaseScreen.COMPUTER_TEXTURE;

    private static final int SLOT_SIZE = 16;
    private static final int TITLE_TEXT_X = 8;
    private static final int TITLE_TEXT_Y = 6;
    private static final int INVENTORY_TEXT_X = 8;
    private static final int INVENTORY_TEXT_Y = 72;
    private static final Component SCREEN_TITLE = Component.translatable("gui.neoopencomputers.microcontroller.title");

    public MicrocontrollerScreen(final MicrocontrollerMenu menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
        imageHeight = 166;
        titleLabelX = TITLE_TEXT_X;
        titleLabelY = TITLE_TEXT_Y;
        inventoryLabelX = INVENTORY_TEXT_X;
        inventoryLabelY = INVENTORY_TEXT_Y;
    }

    @Override
    protected void renderBg(final GuiGraphics guiGraphics, final float partialTick, final int mouseX, final int mouseY) {
        final int left = leftPos;
        final int top = topPos;
        guiGraphics.blit(BACKGROUND_TEXTURE, left, top, 0, 0, imageWidth, imageHeight);
        guiGraphics.blit(MICROCONTROLLER_TEXTURE, left, top, 0, 0, imageWidth, imageHeight);
        final int tier = menu.microcontrollerTier();
        for (int slot = 0; slot < MicrocontrollerMenu.microcontrollerSlotCountForTier(tier); slot++) {
            ComputerCaseScreen.drawSlot(
                guiGraphics,
                left + MicrocontrollerMenu.microcontrollerSlotX(tier, slot) - 1,
                top + MicrocontrollerMenu.microcontrollerSlotY(tier, slot) - 1,
                left + MicrocontrollerMenu.microcontrollerSlotX(tier, slot),
                top + MicrocontrollerMenu.microcontrollerSlotY(tier, slot),
                MicrocontrollerMenu.microcontrollerSlotKind(tier, slot),
                MicrocontrollerMenu.microcontrollerSlotTierLimit(tier, slot),
                menu.getSlot(slot).hasItem());
        }
    }

    @Override
    protected void renderLabels(final GuiGraphics guiGraphics, final int mouseX, final int mouseY) {
        guiGraphics.drawString(font, screenTitle(), titleLabelX, titleLabelY, 0xFF404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF404040, false);
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        final int slot = microcontrollerSlotAt(mouseX, mouseY, leftPos, topPos, menu.microcontrollerTier());
        if (ComputerCaseScreen.shouldRenderSlotOverlayTooltip(slot >= 0, slot >= 0 && menu.getSlot(slot).hasItem())) {
            guiGraphics.renderComponentTooltip(font, ComputerCaseScreen.slotTooltip(
                MicrocontrollerMenu.microcontrollerSlotKind(menu.microcontrollerTier(), slot),
                MicrocontrollerMenu.microcontrollerSlotTierLimit(menu.microcontrollerTier(), slot),
                menu.getSlot(slot).hasItem()), mouseX, mouseY);
        }
    }

    public static Component screenTitle() {
        return SCREEN_TITLE;
    }

    public static int microcontrollerSlotAt(final int mouseX, final int mouseY, final int left, final int top, final int tier) {
        for (int slot = 0; slot < MicrocontrollerMenu.microcontrollerSlotCountForTier(tier); slot++) {
            final int x = left + MicrocontrollerMenu.microcontrollerSlotX(tier, slot);
            final int y = top + MicrocontrollerMenu.microcontrollerSlotY(tier, slot);
            if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                return slot;
            }
        }
        return -1;
    }
}
