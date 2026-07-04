package li.cil.oc.client;

import li.cil.oc.common.menu.RobotMenu;
import li.cil.oc.common.network.RackControlPayload;
import li.cil.oc.common.network.RobotControlPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class RobotScreen extends AbstractContainerScreen<RobotMenu> {
    public static final ResourceLocation BACKGROUND_TEXTURE = ComputerCaseScreen.BACKGROUND_TEXTURE;
    public static final ResourceLocation ROBOT_TEXTURE = ComputerCaseScreen.COMPUTER_TEXTURE;

    private static final int SLOT_SIZE = 16;
    private static final int TITLE_TEXT_X = 8;
    private static final int TITLE_TEXT_Y = 6;
    private static final int INVENTORY_TEXT_X = 8;
    private static final int INVENTORY_TEXT_Y = 72;
    private static final int STATUS_CONTROL_X = 142;
    private static final int STATUS_CONTROL_Y = 33;
    private static final int STATUS_CONTROL_SIZE = 18;
    private static final Component SCREEN_TITLE = Component.translatable("gui.neoopencomputers.robot.title");

    public RobotScreen(final RobotMenu menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
        imageHeight = 166;
        titleLabelX = TITLE_TEXT_X;
        titleLabelY = TITLE_TEXT_Y;
        inventoryLabelX = INVENTORY_TEXT_X;
        inventoryLabelY = INVENTORY_TEXT_Y;
    }

    @Override
    protected void renderBg(final GuiGraphics guiGraphics, final float partialTick, final int mouseX, final int mouseY) {
        final int left = leftPos;
        final int top = topPos;
        guiGraphics.blit(BACKGROUND_TEXTURE, left, top, 0, 0, imageWidth, imageHeight);
        guiGraphics.blit(ROBOT_TEXTURE, left, top, 0, 0, imageWidth, imageHeight);
        final int tier = menu.robotTier();
        for (int slot = 0; slot < RobotMenu.robotSlotCountForTier(tier); slot++) {
            ComputerCaseScreen.drawSlot(
                guiGraphics,
                left + RobotMenu.robotSlotX(tier, slot) - 1,
                top + RobotMenu.robotSlotY(tier, slot) - 1,
                left + RobotMenu.robotSlotX(tier, slot),
                top + RobotMenu.robotSlotY(tier, slot),
                RobotMenu.robotSlotKind(tier, slot),
                RobotMenu.robotSlotTierLimit(tier, slot),
                menu.getSlot(slot).hasItem());
        }
        drawStatusControl(guiGraphics, left + STATUS_CONTROL_X, top + STATUS_CONTROL_Y, menu.robotState(), statusControlAt(mouseX, mouseY, left, top));
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
        final int slot = robotSlotAt(mouseX, mouseY, leftPos, topPos, menu.robotTier());
        if (ComputerCaseScreen.shouldRenderSlotOverlayTooltip(slot >= 0, slot >= 0 && menu.getSlot(slot).hasItem())) {
            guiGraphics.renderComponentTooltip(font, ComputerCaseScreen.slotTooltip(
                RobotMenu.robotSlotKind(menu.robotTier(), slot),
                RobotMenu.robotSlotTierLimit(menu.robotTier(), slot),
                menu.getSlot(slot).hasItem()), mouseX, mouseY);
        } else if (statusControlAt(mouseX, mouseY, leftPos, topPos)) {
            guiGraphics.renderComponentTooltip(font, ComputerCaseScreen.statusControlTooltip(menu.robotState()), mouseX, mouseY);
        } else if (mouseX >= leftPos + 8 && mouseX < leftPos + 168 && mouseY >= topPos + 60 && mouseY < topPos + 72) {
            guiGraphics.renderComponentTooltip(font, ComputerCaseScreen.statusTooltip(menu.robotState(), menu.missingRequirements(), menu.componentCount(), menu.maxComponents()), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == 0 && statusControlAt((int) mouseX, (int) mouseY, leftPos, topPos)) {
            PacketDistributor.sendToServer(controlPayload(menu, statusControlAction(menu.robotState())));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public static Component screenTitle() {
        return SCREEN_TITLE;
    }

    public static int robotSlotAt(final int mouseX, final int mouseY, final int left, final int top, final int tier) {
        for (int slot = 0; slot < RobotMenu.robotSlotCountForTier(tier); slot++) {
            final int x = left + RobotMenu.robotSlotX(tier, slot);
            final int y = top + RobotMenu.robotSlotY(tier, slot);
            if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                return slot;
            }
        }
        return -1;
    }

    static RobotControlPayload controlPayload(final RobotMenu menu, final int action) {
        return new RobotControlPayload(menu.containerId, action);
    }

    static int statusControlAction(final int state) {
        return state == RobotMenu.STATE_RUNNING ? RackControlPayload.STOP : RackControlPayload.START;
    }

    static boolean statusControlAt(final int mouseX, final int mouseY, final int left, final int top) {
        final int x = mouseX - left;
        final int y = mouseY - top;
        return x >= STATUS_CONTROL_X && x < STATUS_CONTROL_X + STATUS_CONTROL_SIZE && y >= STATUS_CONTROL_Y && y < STATUS_CONTROL_Y + STATUS_CONTROL_SIZE;
    }

    private static void drawStatusControl(final GuiGraphics guiGraphics, final int left, final int top, final int state, final boolean hovered) {
        guiGraphics.blit(
            ComputerCaseScreen.POWER_BUTTON_TEXTURE,
            left,
            top,
            ComputerCaseScreen.powerButtonTextureX(state),
            ComputerCaseScreen.powerButtonTextureY(hovered),
            STATUS_CONTROL_SIZE,
            STATUS_CONTROL_SIZE,
            ComputerCaseScreen.powerButtonTextureWidth(),
            ComputerCaseScreen.powerButtonTextureHeight());
    }
}
