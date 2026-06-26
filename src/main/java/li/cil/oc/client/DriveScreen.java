package li.cil.oc.client;

import li.cil.oc.common.menu.DriveMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class DriveScreen extends AbstractContainerScreen<DriveMenu> {
    private static final int BUTTON_WIDTH = 74;
    private static final int BUTTON_HEIGHT = 18;

    private Button managedButton;
    private Button unmanagedButton;
    private Button lockButton;

    public DriveScreen(final DriveMenu menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
        imageHeight = 120;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        managedButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.neoopencomputers.drive.managed"),
                button -> sendButton(DriveMenu.BUTTON_MANAGED))
            .bounds(leftPos + 11, topPos + 11, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());
        unmanagedButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.neoopencomputers.drive.unmanaged"),
                button -> sendButton(DriveMenu.BUTTON_UNMANAGED))
            .bounds(leftPos + 91, topPos + 11, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());
        lockButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.neoopencomputers.drive.lock"),
                button -> sendButton(DriveMenu.BUTTON_LOCK))
            .bounds(leftPos + 11, topPos + imageHeight - 42, 52, BUTTON_HEIGHT)
            .build());
        updateButtonState();
    }

    @Override
    protected void renderBg(final GuiGraphics guiGraphics, final float partialTick, final int mouseX, final int mouseY) {
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF2E3440);
        guiGraphics.fill(leftPos + 7, topPos + 7, leftPos + imageWidth - 7, topPos + imageHeight - 7, 0xFF3B4252);
        guiGraphics.drawWordWrap(font, Component.translatable("gui.neoopencomputers.drive.warning"), leftPos + 11, topPos + 37, imageWidth - 22, 0xFFD8DEE9);
        guiGraphics.drawWordWrap(font, Component.translatable("gui.neoopencomputers.drive.lock_warning"), leftPos + 69, topPos + imageHeight - 48, imageWidth - 80, 0xFFD8DEE9);
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        updateButtonState();
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    public static Component modeLabel(final boolean unmanaged) {
        return Component.translatable(unmanaged
            ? "gui.neoopencomputers.drive.unmanaged"
            : "gui.neoopencomputers.drive.managed");
    }

    private void sendButton(final int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private void updateButtonState() {
        if (managedButton != null) {
            managedButton.active = menu.isUnmanaged();
        }
        if (unmanagedButton != null) {
            unmanagedButton.active = !menu.isUnmanaged();
        }
        if (lockButton != null) {
            lockButton.active = !menu.isLocked();
        }
    }
}
