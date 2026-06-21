package li.cil.oc.client;

import li.cil.oc.common.menu.RelayMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class RelayScreen extends AbstractContainerScreen<RelayMenu> {
    private static final int STATUS_X = 8;
    private static final int STATUS_Y = 6;
    private static final int STATUS_WIDTH = 120;
    private static final int STATUS_HEIGHT = 10;

    public RelayScreen(final RelayMenu menu, final Inventory playerInventory, final Component title) {
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
        drawSlot(guiGraphics, left + 52, top + 25);
        drawSlot(guiGraphics, left + 70, top + 25);
        drawSlot(guiGraphics, left + 88, top + 25);
        drawSlot(guiGraphics, left + 106, top + 25);
    }

    @Override
    protected void renderLabels(final GuiGraphics guiGraphics, final int mouseX, final int mouseY) {
        guiGraphics.drawString(font, statusLabel(menu.relayMode()), STATUS_X, STATUS_Y, 0xE5E9F0, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xE5E9F0, false);
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        if (isHovering(STATUS_X, STATUS_Y, STATUS_WIDTH, STATUS_HEIGHT, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, statusTooltip(menu.relayMode(), menu.relayDelay(), menu.relayQueueSize(), menu.relayMaxQueueSize()), mouseX, mouseY);
        }
    }

    public static Component statusLabel(final int mode) {
        return switch (mode) {
            case RelayMenu.MODE_LINKED -> Component.translatable("gui.neoopencomputers.relay.mode.linked");
            case RelayMenu.MODE_WIRELESS -> Component.translatable("gui.neoopencomputers.relay.mode.wireless");
            default -> Component.translatable("gui.neoopencomputers.relay.mode.wired");
        };
    }

    public static List<Component> statusTooltip(final int mode, final int delay, final int queueSize, final int maxQueueSize) {
        final List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("gui.neoopencomputers.relay.status"));
        tooltip.add(statusLabel(mode));
        if (delay > 0) {
            tooltip.add(Component.translatable("gui.neoopencomputers.relay.delay", delay));
        }
        if (maxQueueSize > 0) {
            tooltip.add(Component.translatable("gui.neoopencomputers.relay.queue", queueSize, maxQueueSize));
        }
        return tooltip;
    }

    private static void drawSlot(final GuiGraphics guiGraphics, final int left, final int top) {
        guiGraphics.fill(left - 1, top - 1, left + 17, top + 17, 0xFF1F232B);
        guiGraphics.fill(left, top, left + 16, top + 16, 0xFF4C566A);
    }
}
