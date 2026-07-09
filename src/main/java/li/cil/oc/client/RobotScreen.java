package li.cil.oc.client;

import li.cil.oc.NeoOpenComputers;
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
    public static final ResourceLocation ROBOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "textures/gui/robot.png");
    public static final ResourceLocation ROBOT_NO_SCREEN_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "textures/gui/robot_noscreen.png");

    private static final int SLOT_SIZE = 16;
    private static final int TITLE_TEXT_X = 8;
    private static final int TITLE_TEXT_Y = 6;
    private static final int SCREEN_X = 8;
    private static final int SCREEN_Y = 18;
    private static final int SCREEN_WIDTH = 240;
    private static final int SCREEN_HEIGHT = 130;
    private static final int STATUS_CONTROL_X = 5;
    private static final int STATUS_CONTROL_Y = 153;
    private static final int STATUS_CONTROL_SIZE = 18;
    private static final int POWER_BAR_X = 26;
    private static final int POWER_BAR_Y = 156;
    private static final int POWER_BAR_WIDTH = 140;
    private static final int POWER_BAR_HEIGHT = 12;
    private static final int IMAGE_WIDTH = 256;
    private static final int IMAGE_HEIGHT_WITH_SCREEN = 256;
    private static final Component SCREEN_TITLE = Component.translatable("gui.neoopencomputers.robot.title");

    public RobotScreen(final RobotMenu menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
        imageWidth = IMAGE_WIDTH;
        imageHeight = IMAGE_HEIGHT_WITH_SCREEN;
        titleLabelX = TITLE_TEXT_X;
        titleLabelY = TITLE_TEXT_Y;
    }

    @Override
    protected void renderBg(final GuiGraphics guiGraphics, final float partialTick, final int mouseX, final int mouseY) {
        final int left = leftPos;
        final int top = topPos;
        guiGraphics.blit(ROBOT_TEXTURE, left, top, 0, 0, imageWidth, imageHeight);
        drawScreenPanel(guiGraphics, left + SCREEN_X, top + SCREEN_Y, menu.hasScreen());
        drawPowerBar(guiGraphics, left + POWER_BAR_X, top + POWER_BAR_Y, menu.energy(), menu.maxEnergy());
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
        } else if (powerBarAt(mouseX, mouseY, leftPos, topPos)) {
            guiGraphics.renderComponentTooltip(font, powerTooltip(menu.energy(), menu.maxEnergy()), mouseX, mouseY);
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

    public static int robotImageWidth() {
        return IMAGE_WIDTH;
    }

    public static int robotImageHeightWithScreen() {
        return IMAGE_HEIGHT_WITH_SCREEN;
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

    private static boolean powerBarAt(final int mouseX, final int mouseY, final int left, final int top) {
        final int x = mouseX - left;
        final int y = mouseY - top;
        return x >= POWER_BAR_X && x < POWER_BAR_X + POWER_BAR_WIDTH && y >= POWER_BAR_Y && y < POWER_BAR_Y + POWER_BAR_HEIGHT;
    }

    private static java.util.List<Component> powerTooltip(final int energy, final int maxEnergy) {
        final int percent = maxEnergy <= 0 ? 0 : (int) Math.min(100L, Math.max(0L, (long) energy * 100L / maxEnergy));
        return java.util.List.of(Component.literal("Power: " + percent + "% (" + energy + "/" + maxEnergy + ")"));
    }

    private static void drawPowerBar(final GuiGraphics guiGraphics, final int left, final int top, final int energy, final int maxEnergy) {
        if (maxEnergy <= 0 || energy <= 0) {
            return;
        }
        final int width = (int) Math.max(1L, Math.min(POWER_BAR_WIDTH, (long) energy * POWER_BAR_WIDTH / maxEnergy));
        guiGraphics.fill(left, top, left + width, top + POWER_BAR_HEIGHT, 0xFF62C864);
        guiGraphics.fill(left, top, left + width, top + 2, 0xFF9EEAA0);
    }

    private static void drawScreenPanel(final GuiGraphics guiGraphics, final int left, final int top, final boolean hasScreen) {
        if (!hasScreen) {
            return;
        }
        guiGraphics.fill(left, top, left + SCREEN_WIDTH, top + SCREEN_HEIGHT, 0xFF000000);
        guiGraphics.fill(left, top, left + SCREEN_WIDTH, top + 1, 0xFF303030);
        guiGraphics.fill(left, top + SCREEN_HEIGHT - 1, left + SCREEN_WIDTH, top + SCREEN_HEIGHT, 0xFF303030);
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
