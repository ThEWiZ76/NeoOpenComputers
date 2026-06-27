package li.cil.oc.client;

import li.cil.oc.common.blockentity.WaypointBlockEntity;
import li.cil.oc.common.menu.WaypointMenu;
import li.cil.oc.common.network.WaypointLabelPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class WaypointScreen extends AbstractContainerScreen<WaypointMenu> {
    private EditBox labelBox;

    public WaypointScreen(final WaypointMenu menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
        imageHeight = 92;
        inventoryLabelY = imageHeight + 1000;
    }

    @Override
    protected void init() {
        super.init();
        labelBox = addRenderableWidget(new EditBox(
            font,
            leftPos + 12,
            topPos + 31,
            imageWidth - 24,
            20,
            Component.translatable("gui.neoopencomputers.waypoint.label")));
        labelBox.setMaxLength(WaypointBlockEntity.MAX_LABEL_LENGTH);
        labelBox.setValue(menu.label());
        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                button -> submitAndClose())
            .bounds(leftPos + imageWidth - 72, topPos + imageHeight - 28, 60, 20)
            .build());
        setInitialFocus(labelBox);
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            submitAndClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderBg(final GuiGraphics guiGraphics, final float partialTick, final int mouseX, final int mouseY) {
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF2E3440);
        guiGraphics.fill(leftPos + 7, topPos + 7, leftPos + imageWidth - 7, topPos + imageHeight - 7, 0xFF3B4252);
        guiGraphics.drawString(font, Component.translatable("gui.neoopencomputers.waypoint.label"), leftPos + 12, topPos + 19, 0xFFD8DEE9, false);
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void submitAndClose() {
        if (labelBox != null) {
            PacketDistributor.sendToServer(new WaypointLabelPayload(menu.containerId, labelBox.getValue()));
        }
        onClose();
    }
}
