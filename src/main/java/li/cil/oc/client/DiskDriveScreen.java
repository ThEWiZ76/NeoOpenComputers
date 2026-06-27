package li.cil.oc.client;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.menu.DiskDriveMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class DiskDriveScreen extends AbstractContainerScreen<DiskDriveMenu> {
    public static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "textures/gui/background.png");
    public static final ResourceLocation SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "textures/gui/slot.png");

    private static final int SLOT_TEXTURE_SIZE = 18;

    public DiskDriveScreen(final DiskDriveMenu menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(final GuiGraphics guiGraphics, final float partialTick, final int mouseX, final int mouseY) {
        final int left = leftPos;
        final int top = topPos;
        guiGraphics.blit(BACKGROUND_TEXTURE, left, top, 0, 0, imageWidth, imageHeight);
        drawSlot(guiGraphics, left + 79, top + 34);
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        if (mouseX >= leftPos + 8 && mouseX < leftPos + 168 && mouseY >= topPos + 60 && mouseY < topPos + 72) {
            guiGraphics.renderComponentTooltip(font, statusTooltip(menu.mediaState()), mouseX, mouseY);
        }
    }

    public static Component statusLabel(final int state) {
        return Component.translatable(state == DiskDriveMenu.STATE_LOADED
            ? "gui.neoopencomputers.disk_drive.state.loaded"
            : "gui.neoopencomputers.disk_drive.state.empty");
    }

    public static List<Component> statusTooltip(final int state) {
        return List.of(Component.translatable("gui.neoopencomputers.disk_drive.status"), statusLabel(state));
    }

    public static int slotTextureWidth() {
        return SLOT_TEXTURE_SIZE;
    }

    public static int slotTextureHeight() {
        return SLOT_TEXTURE_SIZE;
    }

    private static void drawSlot(final GuiGraphics guiGraphics, final int left, final int top) {
        guiGraphics.blit(SLOT_TEXTURE, left, top, 0, 0, SLOT_TEXTURE_SIZE, SLOT_TEXTURE_SIZE, slotTextureWidth(), slotTextureHeight());
    }
}
