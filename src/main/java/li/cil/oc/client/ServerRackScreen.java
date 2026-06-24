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
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServerRackScreen extends AbstractContainerScreen<ServerRackMenu> {
    public static final ResourceLocation SERVER_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "textures/gui/server.png");
    public static final ResourceLocation POWER_BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "textures/gui/button_power.png");
    public static final ResourceLocation SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "textures/gui/slot.png");

    private static final int SERVER_SLOT_SIZE = 16;
    private static final int STATUS_CONTROL_X = 48;
    private static final int STATUS_CONTROL_Y = 33;
    private static final int STATUS_CONTROL_SIZE = 18;
    private static final int TRANSFER_HIGHLIGHT_COLOR = 0x80FFFFFF;

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
            drawSlot(
                guiGraphics,
                left + position.x() - 1,
                top + position.y() - 1,
                left + position.x(),
                top + position.y(),
                menu.slotKind(slot),
                menu.slotTierLimit(slot),
                menu.getSlot(slot).hasItem());
        }
        guiGraphics.drawString(font, statusLabel(menu.serverState()), left + 8, top + 62, 0xFFD8DEE9, false);
        if (statusControlVisible(menu)) {
            drawStatusControl(guiGraphics, left + STATUS_CONTROL_X, top + STATUS_CONTROL_Y, menu.serverState(), statusControlAt(mouseX, mouseY, left, top));
        }
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        if (shouldCloseForMissingRackServer(menu)) {
            onClose();
            return;
        }
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTransferHighlights(guiGraphics, mouseX, mouseY);
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

    public static ResourceLocation slotIconTexture(final int kind) {
        final String name = switch (kind) {
            case ServerRackMenu.SLOT_KIND_CARD -> "card";
            case ServerRackMenu.SLOT_KIND_CPU -> "cpu";
            case ServerRackMenu.SLOT_KIND_COMPONENT_BUS -> "component_bus";
            case ServerRackMenu.SLOT_KIND_MEMORY -> "memory";
            case ServerRackMenu.SLOT_KIND_HDD -> "hdd";
            case ServerRackMenu.SLOT_KIND_EEPROM -> "eeprom";
            default -> null;
        };
        return name == null ? null : iconTexture(name);
    }

    public static ResourceLocation tierIconTexture(final int tier) {
        if (tier == -1) {
            return iconTexture("na");
        }
        return tier >= 0 && tier <= 2 ? iconTexture("tier" + tier) : null;
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

    public static boolean shouldHighlightTransferTarget(
        final boolean carriedEmpty,
        final boolean currentPlayerInventory,
        final boolean currentHasStack,
        final boolean currentSelective,
        final boolean currentAcceptsHovered,
        final boolean hoveredPlayerInventory,
        final boolean hoveredHasStack,
        final boolean hoveredSelective,
        final boolean hoveredAcceptsCurrent) {
        if (!carriedEmpty || currentPlayerInventory == hoveredPlayerInventory) {
            return false;
        }
        return currentPlayerInventory
            ? currentHasStack && hoveredSelective && hoveredAcceptsCurrent
            : hoveredPlayerInventory && hoveredHasStack && currentSelective && currentAcceptsHovered;
    }

    public static boolean shouldHighlightSearchTarget(
        final boolean carriedEmpty,
        final boolean currentPlayerInventory,
        final boolean currentSelective,
        final boolean currentAcceptsSearch) {
        return carriedEmpty && !currentPlayerInventory && currentSelective && currentAcceptsSearch;
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

    static boolean shouldCloseForMissingRackServer(final ServerRackMenu menu) {
        return !menu.isItem() && !menu.serverPresent();
    }

    static boolean isPlayerInventorySlot(final int slotIndex) {
        return slotIndex >= ServerRackMenu.SERVER_SLOT_COUNT;
    }

    static boolean isSelectiveSlot(final ServerRackMenu menu, final int slotIndex) {
        return slotIndex >= 0
            && slotIndex < ServerRackMenu.SERVER_SLOT_COUNT
            && menu.slotKind(slotIndex) != ServerRackMenu.SLOT_KIND_NONE
            && menu.slotTierLimit(slotIndex) >= 0;
    }

    public static int powerButtonTextureX(final int state) {
        return state == ServerRackMenu.STATE_RUNNING ? STATUS_CONTROL_SIZE : 0;
    }

    public static int powerButtonTextureY(final boolean hovered) {
        return hovered ? STATUS_CONTROL_SIZE : 0;
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

    private static void drawSlot(
        final GuiGraphics guiGraphics,
        final int left,
        final int top,
        final int iconLeft,
        final int iconTop,
        final int kind,
        final int tier,
        final boolean occupied) {
        guiGraphics.blit(SLOT_TEXTURE, left, top, 0, 0, STATUS_CONTROL_SIZE, STATUS_CONTROL_SIZE);
        if (occupied) {
            return;
        }
        final ResourceLocation tierTexture = tierIconTexture(tier);
        if (tierTexture != null) {
            guiGraphics.blit(tierTexture, iconLeft, iconTop, 0, 0, SERVER_SLOT_SIZE, SERVER_SLOT_SIZE, SERVER_SLOT_SIZE, SERVER_SLOT_SIZE);
        }
        final ResourceLocation slotTexture = slotIconTexture(kind);
        if (slotTexture != null) {
            guiGraphics.blit(slotTexture, iconLeft, iconTop, 0, 0, SERVER_SLOT_SIZE, SERVER_SLOT_SIZE, SERVER_SLOT_SIZE, SERVER_SLOT_SIZE);
        }
    }

    private void renderTransferHighlights(final GuiGraphics guiGraphics, final int mouseX, final int mouseY) {
        if (!menu.getCarried().isEmpty()) {
            return;
        }
        final Optional<ItemStack> searchStack = hoveredSlot == null ? ItemSearch.hoveredStack(this, mouseX, mouseY) : Optional.empty();
        if (hoveredSlot == null && searchStack.isEmpty()) {
            return;
        }
        final int hoveredIndex = hoveredSlot == null ? -1 : menu.slots.indexOf(hoveredSlot);
        if (hoveredSlot != null && hoveredIndex < 0) {
            return;
        }
        final boolean hoveredPlayerInventory = hoveredSlot != null && isPlayerInventorySlot(hoveredIndex);
        final boolean hoveredSelective = hoveredSlot != null && isSelectiveSlot(menu, hoveredIndex);
        for (int slotIndex = 0; slotIndex < menu.slots.size(); slotIndex++) {
            final Slot slot = menu.slots.get(slotIndex);
            if (slot == hoveredSlot) {
                continue;
            }
            final boolean currentPlayerInventory = isPlayerInventorySlot(slotIndex);
            final boolean currentSelective = isSelectiveSlot(menu, slotIndex);
            final boolean highlight = hoveredSlot != null ? shouldHighlightTransferTarget(
                true,
                currentPlayerInventory,
                slot.hasItem(),
                currentSelective,
                hoveredSlot.hasItem() && slot.mayPlace(hoveredSlot.getItem()),
                hoveredPlayerInventory,
                hoveredSlot.hasItem(),
                hoveredSelective,
                isSelectiveSlot(menu, hoveredIndex) && slot.hasItem() && hoveredSlot.mayPlace(slot.getItem()))
                : shouldHighlightSearchTarget(true, currentPlayerInventory, currentSelective, slot.mayPlace(searchStack.orElseThrow()));
            if (highlight) {
                guiGraphics.fill(
                    leftPos + slot.x,
                    topPos + slot.y,
                    leftPos + slot.x + SERVER_SLOT_SIZE,
                    topPos + slot.y + SERVER_SLOT_SIZE,
                    TRANSFER_HIGHLIGHT_COLOR);
            }
        }
    }

    private static void drawStatusControl(final GuiGraphics guiGraphics, final int left, final int top, final int state, final boolean hovered) {
        guiGraphics.blit(
            POWER_BUTTON_TEXTURE,
            left,
            top,
            powerButtonTextureX(state),
            powerButtonTextureY(hovered),
            STATUS_CONTROL_SIZE,
            STATUS_CONTROL_SIZE);
    }

    private static ResourceLocation iconTexture(final String name) {
        return ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "textures/icons/" + name + ".png");
    }

}
