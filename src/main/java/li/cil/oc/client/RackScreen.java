package li.cil.oc.client;

import li.cil.oc.common.menu.RackMenu;
import li.cil.oc.common.network.RackControlPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class RackScreen extends AbstractContainerScreen<RackMenu> {
    private static final int FIRST_SLOT_X = 53;
    private static final int SLOT_Y = 26;
    private static final int SLOT_SPACING = 18;
    private static final int CONTROL_Y = 50;
    private static final int CONTROL_SIZE = 10;

    public RackScreen(final RackMenu menu, final Inventory playerInventory, final Component title) {
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
        for (int slot = 0; slot < RackMenu.RACK_SLOT_COUNT; slot++) {
            drawSlot(guiGraphics, left + FIRST_SLOT_X - 1 + slot * SLOT_SPACING, top + SLOT_Y - 1);
            drawControl(guiGraphics, left + FIRST_SLOT_X + 3 + slot * SLOT_SPACING, top + CONTROL_Y, controlColor(menu.rackState(slot)));
        }
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        final int slot = controlSlotAt(mouseX, mouseY, leftPos, topPos);
        if (slot >= 0) {
            guiGraphics.renderComponentTooltip(font, controlTooltip(menu.rackState(slot)), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final int slot = controlSlotAt(mouseX, mouseY, leftPos, topPos);
        if (button == 0 && slot >= 0) {
            PacketDistributor.sendToServer(controlPayload(menu, slot, RackControlPayload.TOGGLE));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    static RackControlPayload controlPayload(final RackMenu menu, final int slot, final int action) {
        return new RackControlPayload(menu.containerId, slot, action);
    }

    static int controlSlotAt(final double mouseX, final double mouseY, final int left, final int top) {
        for (int slot = 0; slot < RackMenu.RACK_SLOT_COUNT; slot++) {
            final int x = left + FIRST_SLOT_X + 3 + slot * SLOT_SPACING;
            final int y = top + CONTROL_Y;
            if (mouseX >= x && mouseX < x + CONTROL_SIZE && mouseY >= y && mouseY < y + CONTROL_SIZE) {
                return slot;
            }
        }
        return -1;
    }

    static int controlColor(final int state) {
        return switch (state) {
            case RackMenu.STATE_READY -> 0xFFA3BE8C;
            case RackMenu.STATE_RUNNING -> 0xFF88C0D0;
            default -> 0xFF4C566A;
        };
    }

    static Component stateLabel(final int state) {
        return switch (state) {
            case RackMenu.STATE_READY -> Component.translatable("gui.neoopencomputers.rack.state.ready");
            case RackMenu.STATE_RUNNING -> Component.translatable("gui.neoopencomputers.rack.state.running");
            default -> Component.translatable("gui.neoopencomputers.rack.state.empty");
        };
    }

    static List<Component> controlTooltip(final int state) {
        return List.of(
            Component.translatable("gui.neoopencomputers.rack.control"),
            stateLabel(state));
    }

    private static void drawSlot(final GuiGraphics guiGraphics, final int left, final int top) {
        guiGraphics.fill(left - 1, top - 1, left + 17, top + 17, 0xFF1F232B);
        guiGraphics.fill(left, top, left + 16, top + 16, 0xFF4C566A);
    }

    private static void drawControl(final GuiGraphics guiGraphics, final int left, final int top, final int color) {
        guiGraphics.fill(left, top, left + CONTROL_SIZE, top + CONTROL_SIZE, 0xFF1F232B);
        guiGraphics.fill(left + 3, top + 2, left + 5, top + 8, color);
        guiGraphics.fill(left + 5, top + 3, left + 7, top + 7, color);
        guiGraphics.fill(left + 7, top + 4, left + 8, top + 6, color);
    }
}
