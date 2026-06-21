package li.cil.oc.client;

import li.cil.oc.common.menu.RaidMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class RaidScreen extends AbstractContainerScreen<RaidMenu> {
    private static final int STATUS_X = 8;
    private static final int STATUS_Y = 6;
    private static final int STATUS_WIDTH = 120;
    private static final int STATUS_HEIGHT = 10;

    public RaidScreen(final RaidMenu menu, final Inventory playerInventory, final Component title) {
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
        drawSlot(guiGraphics, left + 61, top + 34);
        drawSlot(guiGraphics, left + 79, top + 34);
        drawSlot(guiGraphics, left + 97, top + 34);
    }

    @Override
    protected void renderLabels(final GuiGraphics guiGraphics, final int mouseX, final int mouseY) {
        guiGraphics.drawString(font, statusLabel(menu.raidState()), STATUS_X, STATUS_Y, 0xE5E9F0, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xE5E9F0, false);
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        if (isHovering(STATUS_X, STATUS_Y, STATUS_WIDTH, STATUS_HEIGHT, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, statusTooltip(menu.raidState(), menu.raidCapacity()), mouseX, mouseY);
        }
    }

    public static Component statusLabel(final int state) {
        return switch (state) {
            case RaidMenu.STATE_READY -> Component.translatable("gui.neoopencomputers.raid.state.ready");
            case RaidMenu.STATE_INCOMPLETE -> Component.translatable("gui.neoopencomputers.raid.state.incomplete");
            default -> Component.translatable("gui.neoopencomputers.raid.state.empty");
        };
    }

    public static List<Component> statusTooltip(final int state, final int capacity) {
        final List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("gui.neoopencomputers.raid.status"));
        tooltip.add(statusLabel(state));
        if (capacity > 0) {
            tooltip.add(Component.translatable("gui.neoopencomputers.raid.capacity", capacityLabel(capacity)));
        }
        return tooltip;
    }

    private static String capacityLabel(final int capacity) {
        if (capacity % (1024 * 1024) == 0) {
            return (capacity / (1024 * 1024)) + " MiB";
        }
        if (capacity % 1024 == 0) {
            return (capacity / 1024) + " KiB";
        }
        return capacity + " B";
    }

    private static void drawSlot(final GuiGraphics guiGraphics, final int left, final int top) {
        guiGraphics.fill(left - 1, top - 1, left + 17, top + 17, 0xFF1F232B);
        guiGraphics.fill(left, top, left + 16, top + 16, 0xFF4C566A);
    }
}
