package li.cil.oc.client;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.common.menu.ComputerCaseMenu;
import li.cil.oc.common.network.ComputerCaseControlPayload;
import li.cil.oc.common.network.RackControlPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class ComputerCaseScreen extends AbstractContainerScreen<ComputerCaseMenu> {
    public static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "textures/gui/background.png");
    public static final ResourceLocation COMPUTER_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "textures/gui/computer.png");
    public static final ResourceLocation POWER_BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "textures/gui/button_power.png");
    public static final ResourceLocation SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "textures/gui/slot.png");

    private static final int SLOT_SIZE = 16;
    private static final int SLOT_TEXTURE_SIZE = 18;
    private static final int POWER_BUTTON_TEXTURE_SIZE = 36;
    private static final int STATUS_CONTROL_X = 70;
    private static final int STATUS_CONTROL_Y = 33;
    private static final int STATUS_CONTROL_SIZE = 18;
    private static final Component SCREEN_TITLE = Component.translatable("gui.neoopencomputers.computer_case.title");

    public ComputerCaseScreen(final ComputerCaseMenu menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(final GuiGraphics guiGraphics, final float partialTick, final int mouseX, final int mouseY) {
        final int left = leftPos;
        final int top = topPos;
        guiGraphics.blit(BACKGROUND_TEXTURE, left, top, 0, 0, imageWidth, imageHeight);
        guiGraphics.blit(COMPUTER_TEXTURE, left, top, 0, 0, imageWidth, imageHeight);
        final int tier = menu.computerTier();
        for (int slot = 0; slot < ComputerCaseMenu.computerSlotCountForTier(tier); slot++) {
            drawSlot(
                guiGraphics,
                left + ComputerCaseMenu.computerSlotX(tier, slot) - 1,
                top + ComputerCaseMenu.computerSlotY(tier, slot) - 1,
                left + ComputerCaseMenu.computerSlotX(tier, slot),
                top + ComputerCaseMenu.computerSlotY(tier, slot),
                ComputerCaseMenu.computerSlotKind(tier, slot),
                ComputerCaseMenu.computerSlotTierLimit(tier, slot),
                menu.getSlot(slot).hasItem());
        }
        drawStatusControl(guiGraphics, left + STATUS_CONTROL_X, top + STATUS_CONTROL_Y, menu.computerState(), statusControlAt(mouseX, mouseY, left, top));
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
        final int slot = computerSlotAt(mouseX, mouseY, leftPos, topPos, menu.computerTier());
        if (slot >= 0) {
            guiGraphics.renderComponentTooltip(font, slotTooltip(
                ComputerCaseMenu.computerSlotKind(menu.computerTier(), slot),
                ComputerCaseMenu.computerSlotTierLimit(menu.computerTier(), slot),
                menu.getSlot(slot).hasItem()), mouseX, mouseY);
        } else if (statusControlAt(mouseX, mouseY, leftPos, topPos)) {
            guiGraphics.renderComponentTooltip(font, statusControlTooltip(menu.computerState()), mouseX, mouseY);
        } else if (mouseX >= leftPos + 8 && mouseX < leftPos + 168 && mouseY >= topPos + 60 && mouseY < topPos + 72) {
            guiGraphics.renderComponentTooltip(font, statusTooltip(menu.computerState(), menu.missingRequirements(), menu.componentCount(), menu.maxComponents()), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == 0 && statusControlAt((int) mouseX, (int) mouseY, leftPos, topPos)) {
            PacketDistributor.sendToServer(controlPayload(menu, statusControlAction(menu.computerState())));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public static Component statusLabel(final int state) {
        return Component.translatable(switch (state) {
            case ComputerCaseMenu.STATE_READY -> "gui.neoopencomputers.computer_case.state.ready";
            case ComputerCaseMenu.STATE_RUNNING -> "gui.neoopencomputers.computer_case.state.running";
            case ComputerCaseMenu.STATE_INCOMPLETE -> "gui.neoopencomputers.computer_case.state.incomplete";
            default -> "gui.neoopencomputers.computer_case.state.empty";
        });
    }

    public static Component screenTitle() {
        return SCREEN_TITLE;
    }

    public static List<Component> statusTooltip(final int state, final int missingRequirements, final int componentCount, final int maxComponents) {
        final List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("gui.neoopencomputers.computer_case.status"));
        tooltip.add(statusLabel(state));
        if (maxComponents > 0) {
            tooltip.add(Component.translatable("gui.neoopencomputers.computer_case.components", componentCount, maxComponents));
        }
        if ((missingRequirements & ComputerCaseMenu.MISSING_CPU) != 0) {
            tooltip.add(Component.translatable("gui.neoopencomputers.rack.missing.cpu"));
        }
        if ((missingRequirements & ComputerCaseMenu.MISSING_MEMORY) != 0) {
            tooltip.add(Component.translatable("gui.neoopencomputers.rack.missing.memory"));
        }
        if ((missingRequirements & ComputerCaseMenu.MISSING_EEPROM) != 0) {
            tooltip.add(Component.translatable("gui.neoopencomputers.rack.missing.eeprom"));
        }
        return tooltip;
    }

    public static List<Component> slotTooltip(final String kind, final int tier, final boolean occupied) {
        return List.of(
            Component.literal(kind),
            tier == Integer.MAX_VALUE
                ? Component.translatable("gui.neoopencomputers.server_rack.slot.any_tier")
                : Component.translatable("gui.neoopencomputers.server_rack.slot.max_tier", tier),
            Component.translatable(occupied ? "gui.neoopencomputers.server_rack.slot.installed" : "gui.neoopencomputers.server_rack.slot.empty_state"));
    }

    public static List<Component> statusControlTooltip(final int state) {
        return List.of(Component.translatable(state == ComputerCaseMenu.STATE_RUNNING
            ? "gui.neoopencomputers.computer_case.power.turn_off"
            : "gui.neoopencomputers.computer_case.power.turn_on"));
    }

    static ComputerCaseControlPayload controlPayload(final ComputerCaseMenu menu, final int action) {
        return new ComputerCaseControlPayload(menu.containerId, action);
    }

    static int statusControlAction(final int state) {
        return state == ComputerCaseMenu.STATE_RUNNING ? RackControlPayload.STOP : RackControlPayload.START;
    }

    static boolean statusControlAt(final int mouseX, final int mouseY, final int left, final int top) {
        final int x = mouseX - left;
        final int y = mouseY - top;
        return x >= STATUS_CONTROL_X && x < STATUS_CONTROL_X + STATUS_CONTROL_SIZE && y >= STATUS_CONTROL_Y && y < STATUS_CONTROL_Y + STATUS_CONTROL_SIZE;
    }

    public static int computerSlotAt(final int mouseX, final int mouseY, final int left, final int top, final int tier) {
        for (int slot = 0; slot < ComputerCaseMenu.computerSlotCountForTier(tier); slot++) {
            final int x = left + ComputerCaseMenu.computerSlotX(tier, slot);
            final int y = top + ComputerCaseMenu.computerSlotY(tier, slot);
            if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                return slot;
            }
        }
        return -1;
    }

    public static int powerButtonTextureX(final int state) {
        return state == ComputerCaseMenu.STATE_RUNNING ? STATUS_CONTROL_SIZE : 0;
    }

    public static int powerButtonTextureY(final boolean hovered) {
        return hovered ? STATUS_CONTROL_SIZE : 0;
    }

    public static int powerButtonTextureWidth() {
        return POWER_BUTTON_TEXTURE_SIZE;
    }

    public static int powerButtonTextureHeight() {
        return POWER_BUTTON_TEXTURE_SIZE;
    }

    public static int slotTextureWidth() {
        return SLOT_TEXTURE_SIZE;
    }

    public static int slotTextureHeight() {
        return SLOT_TEXTURE_SIZE;
    }

    private static void drawSlot(
        final GuiGraphics guiGraphics,
        final int left,
        final int top,
        final int iconLeft,
        final int iconTop,
        final String kind,
        final int tier,
        final boolean occupied) {
        guiGraphics.blit(
            SLOT_TEXTURE,
            left,
            top,
            0,
            0,
            STATUS_CONTROL_SIZE,
            STATUS_CONTROL_SIZE,
            slotTextureWidth(),
            slotTextureHeight());
        if (occupied) {
            return;
        }
        final ResourceLocation tierTexture = tierIconTexture(tier);
        if (tierTexture != null) {
            guiGraphics.blit(tierTexture, iconLeft, iconTop, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
        }
        final ResourceLocation slotTexture = slotIconTexture(kind);
        if (slotTexture != null) {
            guiGraphics.blit(slotTexture, iconLeft, iconTop, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
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
            STATUS_CONTROL_SIZE,
            powerButtonTextureWidth(),
            powerButtonTextureHeight());
    }

    private static ResourceLocation slotIconTexture(final String kind) {
        return switch (kind) {
            case Slot.Card -> iconTexture("card");
            case Slot.CPU -> iconTexture("cpu");
            case Slot.Memory -> iconTexture("memory");
            case Slot.HDD -> iconTexture("hdd");
            case Slot.Floppy -> iconTexture("floppy");
            case "eeprom" -> iconTexture("eeprom");
            default -> null;
        };
    }

    private static ResourceLocation tierIconTexture(final int tier) {
        if (tier == -1) {
            return iconTexture("na");
        }
        return tier >= 0 && tier <= 2 ? iconTexture("tier" + tier) : null;
    }

    private static ResourceLocation iconTexture(final String name) {
        return ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "textures/icons/" + name + ".png");
    }
}
