package li.cil.oc.client;

import li.cil.oc.common.component.TerminalScreenSnapshot;
import li.cil.oc.common.menu.TerminalMenu;
import li.cil.oc.common.network.TerminalClipboardPayload;
import li.cil.oc.common.network.TerminalKeyPayload;
import li.cil.oc.common.network.TerminalMousePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public class TerminalScreen extends AbstractContainerScreen<TerminalMenu> {
    private static final int LINE_HEIGHT = 9;
    private static final int CELL_WIDTH = 6;
    private static final int TEXT_LEFT = 12;
    private static final int TEXT_TOP = 22;
    private static final int TEXT_COLOR = 0xFFB8F4C8;

    public TerminalScreen(final TerminalMenu menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
        imageWidth = 248;
        imageHeight = 166;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelY = 1000;
    }

    @Override
    protected void renderBg(final GuiGraphics guiGraphics, final float partialTick, final int mouseX, final int mouseY) {
        final int left = leftPos;
        final int top = topPos;
        guiGraphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFF101820);
        guiGraphics.fill(left + 8, top + 18, left + imageWidth - 8, top + imageHeight - 8, 0xFF05080C);
        final Component status = statusLabel(menu.snapshot());
        if (status != null) {
            guiGraphics.drawString(font, status, left + TEXT_LEFT, top + TEXT_TOP, 0xFF6F7F8F, false);
            return;
        }
        for (int row = 0; row < Math.min(menu.snapshot().height(), 15); row++) {
            final String line = snapshotLine(menu.snapshot(), row);
            if (!line.isBlank()) {
                guiGraphics.drawString(font, line, left + TEXT_LEFT, top + TEXT_TOP + row * LINE_HEIGHT, TEXT_COLOR, false);
            }
        }
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if (hasControlDown() && keyCode == GLFW.GLFW_KEY_V && minecraft != null) {
            sendClipboardInput(minecraft.keyboardHandler.getClipboard());
            return true;
        }
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        sendKeyInput(true, (char) 0, keyCode);
        return true;
    }

    @Override
    public boolean keyReleased(final int keyCode, final int scanCode, final int modifiers) {
        sendKeyInput(false, (char) 0, keyCode);
        return true;
    }

    @Override
    public boolean charTyped(final char codePoint, final int modifiers) {
        sendKeyInput(true, codePoint, 0);
        sendKeyInput(false, codePoint, 0);
        return true;
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        sendMouseInput(TerminalMousePayload.MOUSE_DOWN, mouseX, mouseY, button);
        return true;
    }

    @Override
    public boolean mouseDragged(final double mouseX, final double mouseY, final int button, final double dragX, final double dragY) {
        sendMouseInput(TerminalMousePayload.MOUSE_DRAG, mouseX, mouseY, button);
        return true;
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        sendMouseInput(TerminalMousePayload.MOUSE_UP, mouseX, mouseY, button);
        return true;
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
        sendMouseInput(TerminalMousePayload.MOUSE_SCROLL, mouseX, mouseY, (int) Math.signum(scrollY));
        return true;
    }

    static String snapshotLine(final TerminalScreenSnapshot snapshot, final int row) {
        return snapshot == null ? "" : snapshot.line(row);
    }

    static boolean hasVisibleText(final TerminalScreenSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        for (int row = 0; row < snapshot.height(); row++) {
            if (!snapshotLine(snapshot, row).isBlank()) {
                return true;
            }
        }
        return false;
    }

    static Component statusLabel(final TerminalScreenSnapshot snapshot) {
        if (snapshot == null || snapshot.width() <= 0 || snapshot.height() <= 0) {
            return Component.translatable("gui.neoopencomputers.terminal.no_screen_data");
        }
        return hasVisibleText(snapshot) ? null : Component.translatable("gui.neoopencomputers.terminal.blank_screen");
    }

    static TerminalKeyPayload keyPayload(final TerminalMenu menu, final boolean pressed, final char character, final int keyCode) {
        return new TerminalKeyPayload(menu.containerId, pressed, character, keyCode);
    }

    static TerminalClipboardPayload clipboardPayload(final TerminalMenu menu, final String value) {
        return new TerminalClipboardPayload(menu.containerId, value);
    }

    static TerminalMousePayload mousePayload(final TerminalMenu menu, final int kind, final double mouseX, final double mouseY, final int buttonOrDelta, final int left, final int top) {
        final double column = 1 + Math.floor((mouseX - left - TEXT_LEFT) / CELL_WIDTH);
        final double row = 1 + Math.floor((mouseY - top - TEXT_TOP) / LINE_HEIGHT);
        return new TerminalMousePayload(menu.containerId, kind, column, row, buttonOrDelta);
    }

    static TerminalMousePayload mousePayload(final TerminalMenu menu, final int kind, final double mouseX, final double mouseY, final int buttonOrDelta, final int left, final int top, final TerminalScreenSnapshot snapshot) {
        if (snapshot == null || snapshot.width() <= 0 || snapshot.height() <= 0) {
            return null;
        }
        final TerminalMousePayload payload = mousePayload(menu, kind, mouseX, mouseY, buttonOrDelta, left, top);
        if (payload.x() < 1 || payload.y() < 1 || payload.x() > snapshot.width() || payload.y() > snapshot.height()) {
            return null;
        }
        return payload;
    }

    private void sendKeyInput(final boolean pressed, final char character, final int keyCode) {
        PacketDistributor.sendToServer(keyPayload(menu, pressed, character, keyCode));
    }

    private void sendClipboardInput(final String value) {
        if (value != null && !value.isEmpty()) {
            PacketDistributor.sendToServer(clipboardPayload(menu, value));
        }
    }

    private void sendMouseInput(final int kind, final double mouseX, final double mouseY, final int buttonOrDelta) {
        final TerminalMousePayload payload = mousePayload(menu, kind, mouseX, mouseY, buttonOrDelta, leftPos, topPos, menu.snapshot());
        if (payload != null) {
            PacketDistributor.sendToServer(payload);
        }
    }
}
