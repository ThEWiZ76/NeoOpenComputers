package li.cil.oc.client;

import li.cil.oc.common.menu.ServerRackMenu;
import li.cil.oc.common.network.RackControlPayload;
import li.cil.oc.common.network.ServerRackControlPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class ServerRackScreen extends AbstractContainerScreen<ServerRackMenu> {
    private static final int SERVER_SLOT_LEFT = 8;
    private static final int SERVER_SLOT_TOP = 18;
    private static final int SERVER_SLOT_SIZE = 16;
    private static final int SERVER_SLOT_STRIDE = 18;
    private static final int SERVER_SLOT_COLUMNS = 9;
    private static final int STATUS_CONTROL_X = 152;
    private static final int STATUS_CONTROL_Y = 62;
    private static final int STATUS_CONTROL_SIZE = 10;

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
        guiGraphics.drawString(font, statusLabel(menu.serverState()), left + 8, top + 62, 0xFFD8DEE9, false);
        drawStatusControl(guiGraphics, left + STATUS_CONTROL_X, top + STATUS_CONTROL_Y, menu.serverState());
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        final int slot = serverSlotAt(mouseX, mouseY, leftPos, topPos);
        if (slot >= 0) {
            guiGraphics.renderComponentTooltip(font, slotTooltip(menu.slotKind(slot), menu.slotTierLimit(slot)), mouseX, mouseY);
        } else if (mouseX >= leftPos + 8 && mouseX < leftPos + 168 && mouseY >= topPos + 60 && mouseY < topPos + 72) {
            guiGraphics.renderComponentTooltip(font, statusTooltip(menu.serverState(), menu.missingRequirements(), menu.componentCount(), menu.maxComponents()), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == 0 && statusControlAt((int) mouseX, (int) mouseY, leftPos, topPos)) {
            PacketDistributor.sendToServer(controlPayload(menu, RackControlPayload.TOGGLE));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
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

    public static Component slotTierLabel(final int tier) {
        if (tier == Integer.MAX_VALUE) {
            return Component.translatable("gui.neoopencomputers.server_rack.slot.any_tier");
        }
        if (tier < 0) {
            return Component.translatable("gui.neoopencomputers.server_rack.slot.unavailable");
        }
        return Component.translatable("gui.neoopencomputers.server_rack.slot.max_tier", tier);
    }

    public static List<Component> slotTooltip(final int kind, final int tier) {
        return List.of(slotLabel(kind), slotTierLabel(tier));
    }

    public static Component statusLabel(final int state) {
        return Component.translatable(switch (state) {
            case ServerRackMenu.STATE_READY -> "gui.neoopencomputers.server_rack.state.ready";
            case ServerRackMenu.STATE_RUNNING -> "gui.neoopencomputers.server_rack.state.running";
            case ServerRackMenu.STATE_INCOMPLETE -> "gui.neoopencomputers.server_rack.state.incomplete";
            default -> "gui.neoopencomputers.server_rack.state.empty";
        });
    }

    public static List<Component> statusTooltip(final int state, final int missingRequirements) {
        return statusTooltip(state, missingRequirements, 0, 0);
    }

    public static List<Component> statusTooltip(final int state, final int missingRequirements, final int componentCount, final int maxComponents) {
        final List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("gui.neoopencomputers.server_rack.status"));
        tooltip.add(statusLabel(state));
        if (maxComponents > 0) {
            tooltip.add(Component.translatable("gui.neoopencomputers.server_rack.components", componentCount, maxComponents));
        }
        if ((missingRequirements & ServerRackMenu.MISSING_CPU) != 0) {
            tooltip.add(Component.translatable("gui.neoopencomputers.rack.missing.cpu"));
        }
        if ((missingRequirements & ServerRackMenu.MISSING_MEMORY) != 0) {
            tooltip.add(Component.translatable("gui.neoopencomputers.rack.missing.memory"));
        }
        if ((missingRequirements & ServerRackMenu.MISSING_EEPROM) != 0) {
            tooltip.add(Component.translatable("gui.neoopencomputers.rack.missing.eeprom"));
        }
        return tooltip;
    }

    static ServerRackControlPayload controlPayload(final ServerRackMenu menu, final int action) {
        return new ServerRackControlPayload(menu.containerId, action);
    }

    static boolean statusControlAt(final int mouseX, final int mouseY, final int left, final int top) {
        final int x = mouseX - left;
        final int y = mouseY - top;
        return x >= STATUS_CONTROL_X && x < STATUS_CONTROL_X + STATUS_CONTROL_SIZE && y >= STATUS_CONTROL_Y && y < STATUS_CONTROL_Y + STATUS_CONTROL_SIZE;
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

    private static void drawStatusControl(final GuiGraphics guiGraphics, final int left, final int top, final int state) {
        final int color = state == ServerRackMenu.STATE_RUNNING ? 0xFF88C0D0 : state == ServerRackMenu.STATE_READY ? 0xFFA3BE8C : 0xFFD08770;
        guiGraphics.fill(left, top, left + STATUS_CONTROL_SIZE, top + STATUS_CONTROL_SIZE, 0xFF1F232B);
        guiGraphics.fill(left + 3, top + 2, left + 5, top + 8, color);
        guiGraphics.fill(left + 5, top + 3, left + 7, top + 7, color);
        guiGraphics.fill(left + 7, top + 4, left + 8, top + 6, color);
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
