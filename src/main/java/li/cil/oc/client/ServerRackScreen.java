package li.cil.oc.client;

import li.cil.oc.common.menu.ServerRackMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class ServerRackScreen extends AbstractContainerScreen<ServerRackMenu> {
    private static final int SERVER_SLOT_LEFT = 8;
    private static final int SERVER_SLOT_TOP = 18;
    private static final int SERVER_SLOT_SIZE = 16;
    private static final int SERVER_SLOT_STRIDE = 18;
    private static final int SERVER_SLOT_COLUMNS = 9;

    public ServerRackScreen(final ServerRackMenu menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(final GuiGraphics guiGraphics, final float partialTick, final int mouseX, final int mouseY) {
        final int left = leftPos;
        final int top = topPos;
        guiGraphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFF2E3440);
        guiGraphics.fill(left + 7, top + 16, left + 169, top + 58, 0xFF3B4252);
        for (int slot = 0; slot < ServerRackMenu.SERVER_SLOT_COUNT; slot++) {
            drawSlot(guiGraphics, left + 7 + (slot % SERVER_SLOT_COLUMNS) * SERVER_SLOT_STRIDE, top + 17 + (slot / SERVER_SLOT_COLUMNS) * SERVER_SLOT_STRIDE, menu.slotKind(slot));
            final String label = slotAbbreviation(menu.slotKind(slot));
            if (!label.isEmpty()) {
                guiGraphics.drawString(font, label, left + SERVER_SLOT_LEFT + (slot % SERVER_SLOT_COLUMNS) * SERVER_SLOT_STRIDE + 2, top + SERVER_SLOT_TOP + (slot / SERVER_SLOT_COLUMNS) * SERVER_SLOT_STRIDE + 4, 0xFFD8DEE9, false);
            }
        }
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        final int slot = serverSlotAt(mouseX, mouseY, leftPos, topPos);
        if (slot >= 0) {
            guiGraphics.renderComponentTooltip(font, slotTooltip(menu.slotKind(slot)), mouseX, mouseY);
        }
    }

    public static Component slotLabel(final int kind) {
        return Component.translatable(switch (kind) {
            case ServerRackMenu.SLOT_KIND_CARD -> "gui.neoopencomputers.server_rack.slot.card";
            case ServerRackMenu.SLOT_KIND_CPU -> "gui.neoopencomputers.server_rack.slot.cpu";
            case ServerRackMenu.SLOT_KIND_COMPONENT_BUS -> "gui.neoopencomputers.server_rack.slot.component_bus";
            case ServerRackMenu.SLOT_KIND_MEMORY -> "gui.neoopencomputers.server_rack.slot.memory";
            case ServerRackMenu.SLOT_KIND_HDD -> "gui.neoopencomputers.server_rack.slot.hdd";
            case ServerRackMenu.SLOT_KIND_EEPROM -> "gui.neoopencomputers.server_rack.slot.eeprom";
            default -> "gui.neoopencomputers.server_rack.slot.empty";
        });
    }

    public static List<Component> slotTooltip(final int kind) {
        return List.of(slotLabel(kind));
    }

    public static int serverSlotAt(final int mouseX, final int mouseY, final int left, final int top) {
        final int x = mouseX - left - SERVER_SLOT_LEFT;
        final int y = mouseY - top - SERVER_SLOT_TOP;
        if (x < 0 || y < 0 || x % SERVER_SLOT_STRIDE >= SERVER_SLOT_SIZE || y % SERVER_SLOT_STRIDE >= SERVER_SLOT_SIZE) {
            return -1;
        }
        final int column = x / SERVER_SLOT_STRIDE;
        final int row = y / SERVER_SLOT_STRIDE;
        if (column >= SERVER_SLOT_COLUMNS) {
            return -1;
        }
        final int slot = row * SERVER_SLOT_COLUMNS + column;
        return slot < ServerRackMenu.SERVER_SLOT_COUNT ? slot : -1;
    }

    private static void drawSlot(final GuiGraphics guiGraphics, final int left, final int top, final int kind) {
        guiGraphics.fill(left - 1, top - 1, left + 17, top + 17, 0xFF1F232B);
        guiGraphics.fill(left, top, left + 16, top + 16, kind == ServerRackMenu.SLOT_KIND_NONE ? 0xFF2E3440 : 0xFF4C566A);
    }

    private static String slotAbbreviation(final int kind) {
        return switch (kind) {
            case ServerRackMenu.SLOT_KIND_CARD -> "C";
            case ServerRackMenu.SLOT_KIND_CPU -> "CPU";
            case ServerRackMenu.SLOT_KIND_COMPONENT_BUS -> "B";
            case ServerRackMenu.SLOT_KIND_MEMORY -> "M";
            case ServerRackMenu.SLOT_KIND_HDD -> "D";
            case ServerRackMenu.SLOT_KIND_EEPROM -> "E";
            default -> "";
        };
    }
}
