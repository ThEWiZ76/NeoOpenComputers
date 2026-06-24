package li.cil.oc.client;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.menu.ServerRackMenu;
import li.cil.oc.common.network.RackControlPayload;
import li.cil.oc.common.network.ServerRackControlPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class ServerRackScreen extends AbstractContainerScreen<ServerRackMenu> {
    public static final ResourceLocation SERVER_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "textures/gui/server.png");

    private static final int SERVER_SLOT_SIZE = 16;
    private static final int STATUS_CONTROL_X = 48;
    private static final int STATUS_CONTROL_Y = 33;
    private static final int STATUS_CONTROL_SIZE = 18;

    public ServerRackScreen(final ServerRackMenu menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(final GuiGraphics guiGraphics, final float partialTick, final int mouseX, final int mouseY) {
        final int left = leftPos;
        final int top = topPos;
        guiGraphics.blit(SERVER_TEXTURE, left, top, 0, 0, imageWidth, imageHeight);
        final int tier = serverTier(menu);
        for (int slot = 0; slot < ServerRackMenu.SERVER_SLOT_COUNT; slot++) {
            final ServerRackMenu.ServerSlotPosition position = slotPositionForTier(tier, slot);
            if (position == null) {
                continue;
            }
            drawSlot(guiGraphics, left + position.x() - 1, top + position.y() - 1, menu.slotKind(slot));
            final String label = slotAbbreviation(menu.slotKind(slot));
            if (!label.isEmpty()) {
                guiGraphics.drawString(font, label, left + position.x() + 2, top + position.y() + 4, 0xFFD8DEE9, false);
            }
        }
        guiGraphics.drawString(font, statusLabel(menu.serverState()), left + 8, top + 62, 0xFFD8DEE9, false);
        if (statusControlVisible(menu)) {
            drawStatusControl(guiGraphics, left + STATUS_CONTROL_X, top + STATUS_CONTROL_Y, menu.serverState());
        }
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        final int slot = serverSlotAt(mouseX, mouseY, leftPos, topPos, serverTier(menu));
        if (slot >= 0) {
            guiGraphics.renderComponentTooltip(font, slotTooltip(menu.slotKind(slot), menu.slotTierLimit(slot), menu.getSlot(slot).hasItem()), mouseX, mouseY);
        } else if (statusControlVisible(menu) && statusControlAt(mouseX, mouseY, leftPos, topPos)) {
            guiGraphics.renderComponentTooltip(font, statusControlTooltip(menu.serverState()), mouseX, mouseY);
        } else if (mouseX >= leftPos + 8 && mouseX < leftPos + 168 && mouseY >= topPos + 60 && mouseY < topPos + 72) {
            guiGraphics.renderComponentTooltip(font, statusTooltip(menu.serverState(), menu.missingRequirements(), menu.componentCount(), menu.maxComponents()), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == 0 && statusControlVisible(menu) && statusControlAt((int) mouseX, (int) mouseY, leftPos, topPos)) {
            PacketDistributor.sendToServer(controlPayload(menu, RackControlPayload.TOGGLE));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void slotClicked(final Slot slot, final int slotId, final int mouseButton, final ClickType clickType) {
        if (menu.isLockedSlot(slotId)) {
            return;
        }
        super.slotClicked(slot, slotId, mouseButton, clickType);
    }

    @Override
    protected boolean checkHotbarKeyPressed(final int keyCode, final int scanCode) {
        return false;
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

    public static List<Component> slotTooltip(final int kind, final int tier, final boolean occupied) {
        return List.of(
            slotLabel(kind),
            slotTierLabel(tier),
            Component.translatable(occupied ? "gui.neoopencomputers.server_rack.slot.installed" : "gui.neoopencomputers.server_rack.slot.empty_state"));
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

    public static List<Component> statusControlTooltip(final int state) {
        return List.of(Component.translatable(state == ServerRackMenu.STATE_RUNNING
            ? "gui.neoopencomputers.server_rack.power.turn_off"
            : "gui.neoopencomputers.server_rack.power.turn_on"));
    }

    static ServerRackControlPayload controlPayload(final ServerRackMenu menu, final int action) {
        return new ServerRackControlPayload(menu.containerId, action);
    }

    static boolean statusControlAt(final int mouseX, final int mouseY, final int left, final int top) {
        final int x = mouseX - left;
        final int y = mouseY - top;
        return x >= STATUS_CONTROL_X && x < STATUS_CONTROL_X + STATUS_CONTROL_SIZE && y >= STATUS_CONTROL_Y && y < STATUS_CONTROL_Y + STATUS_CONTROL_SIZE;
    }

    static boolean statusControlVisible(final ServerRackMenu menu) {
        return !menu.isItem();
    }

    public static int serverSlotAt(final int mouseX, final int mouseY, final int left, final int top) {
        return serverSlotAt(mouseX, mouseY, left, top, 2);
    }

    public static int serverSlotAt(final int mouseX, final int mouseY, final int left, final int top, final int tier) {
        for (int slot = 0; slot < ServerRackMenu.SERVER_SLOT_COUNT; slot++) {
            final ServerRackMenu.ServerSlotPosition position = slotPositionForTier(tier, slot);
            if (position == null) {
                continue;
            }
            final int x = left + position.x();
            final int y = top + position.y();
            if (mouseX >= x && mouseX < x + SERVER_SLOT_SIZE && mouseY >= y && mouseY < y + SERVER_SLOT_SIZE) {
                return slot;
            }
        }
        return -1;
    }

    public static ServerRackMenu.ServerSlotPosition slotPositionForTier(final int tier, final int slot) {
        return ServerRackMenu.slotPositionForTier(tier, slot);
    }

    static int serverTier(final ServerRackMenu menu) {
        if (menu.slotTierLimit(16) >= 0 || menu.slotKind(16) != ServerRackMenu.SLOT_KIND_NONE) {
            return 2;
        }
        if (menu.slotTierLimit(12) >= 0 || menu.slotKind(12) != ServerRackMenu.SLOT_KIND_NONE) {
            return 1;
        }
        return 0;
    }

    private static void drawSlot(final GuiGraphics guiGraphics, final int left, final int top, final int kind) {
        guiGraphics.fill(left - 1, top - 1, left + 17, top + 17, 0xFF1F232B);
        guiGraphics.fill(left, top, left + 16, top + 16, kind == ServerRackMenu.SLOT_KIND_NONE ? 0xFF2E3440 : 0xFF4C566A);
    }

    private static void drawStatusControl(final GuiGraphics guiGraphics, final int left, final int top, final int state) {
        final int color = state == ServerRackMenu.STATE_RUNNING ? 0xFF88C0D0 : state == ServerRackMenu.STATE_READY ? 0xFFA3BE8C : 0xFFD08770;
        guiGraphics.fill(left, top, left + STATUS_CONTROL_SIZE, top + STATUS_CONTROL_SIZE, 0xFF1F232B);
        guiGraphics.fill(left + 6, top + 4, left + 9, top + 14, color);
        guiGraphics.fill(left + 9, top + 5, left + 12, top + 13, color);
        guiGraphics.fill(left + 12, top + 7, left + 14, top + 11, color);
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
