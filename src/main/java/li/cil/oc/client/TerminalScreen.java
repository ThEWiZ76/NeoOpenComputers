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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TerminalScreen extends AbstractContainerScreen<TerminalMenu> {
    private static final int DEFAULT_IMAGE_WIDTH = 248;
    private static final int DEFAULT_IMAGE_HEIGHT = 166;
    private static final int LINE_HEIGHT = 9;
    private static final int CELL_WIDTH = 6;
    private static final int TEXT_LEFT = 12;
    private static final int TEXT_TOP = 22;
    private static final int TEXT_RIGHT_MARGIN = 12;
    private static final int TEXT_BOTTOM_MARGIN = 12;
    private static final int CLIPBOARD_CHUNK_SIZE = 16 * 1024;
    private static final int CLIPBOARD_MAX_LENGTH = 64 * 1024;
    private final Set<Integer> pressedKeys = new HashSet<>();

    record TextRun(int column, String text, int color) {
    }

    public TerminalScreen(final TerminalMenu menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
        imageWidth = imageWidth(menu.snapshot());
        imageHeight = imageHeight(menu.snapshot());
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelY = 1000;
    }

    @Override
    protected void renderBg(final GuiGraphics guiGraphics, final float partialTick, final int mouseX, final int mouseY) {
        updateLayoutForSnapshot();
        final int left = leftPos;
        final int top = topPos;
        guiGraphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFF101820);
        guiGraphics.fill(left + 8, top + 18, left + imageWidth - 8, top + imageHeight - 8, 0xFF05080C);
        final Component status = statusLabel(menu.snapshot());
        if (status != null) {
            guiGraphics.drawString(font, status, left + TEXT_LEFT, top + TEXT_TOP, 0xFF6F7F8F, false);
            return;
        }
        final TerminalScreenSnapshot snapshot = menu.snapshot();
        renderCellBackgrounds(guiGraphics, snapshot, left, top);
        for (int row = 0; row < visibleRows(snapshot); row++) {
            for (final TextRun run : textRuns(snapshot, row)) {
                if (!run.text().isBlank()) {
                    guiGraphics.drawString(
                        font,
                        run.text(),
                        left + TEXT_LEFT + run.column() * CELL_WIDTH,
                        top + TEXT_TOP + row * LINE_HEIGHT,
                        run.color(),
                        false);
                }
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
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (!shouldForwardKeyboardInput(menu.snapshot())) {
            return false;
        }
        if (hasControlDown() && keyCode == GLFW.GLFW_KEY_V && minecraft != null) {
            sendClipboardInput(minecraft.keyboardHandler.getClipboard());
            return true;
        }
        if (shouldForwardKeyPress(pressedKeys.contains(keyCode), keyCode)) {
            sendKeyInput(true, (char) 0, keyCode);
        }
        pressedKeys.add(keyCode);
        return true;
    }

    @Override
    public boolean keyReleased(final int keyCode, final int scanCode, final int modifiers) {
        if (!shouldForwardKeyboardInput(menu.snapshot())) {
            return false;
        }
        if (shouldForwardKeyRelease(pressedKeys.remove(keyCode))) {
            sendKeyInput(false, (char) 0, keyCode);
        }
        return true;
    }

    @Override
    public boolean charTyped(final char codePoint, final int modifiers) {
        if (!shouldForwardKeyboardInput(menu.snapshot())) {
            return false;
        }
        sendKeyInput(true, codePoint, 0);
        sendKeyInput(false, codePoint, 0);
        return true;
    }

    @Override
    public void removed() {
        pressedKeys.clear();
        super.removed();
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
            for (int column = 0; column < snapshot.width(); column++) {
                if ((backgroundColor(snapshot, column, row) & 0x00FFFFFF) != 0) {
                    return true;
                }
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

    static boolean acceptsInput(final TerminalScreenSnapshot snapshot) {
        return snapshot != null && snapshot.width() > 0 && snapshot.height() > 0;
    }

    static boolean shouldForwardKeyboardInput(final boolean acceptsInput, final boolean searchInputFocused) {
        return acceptsInput && !searchInputFocused;
    }

    static boolean shouldForwardKeyboardInput(final TerminalScreenSnapshot snapshot) {
        return shouldForwardKeyboardInput(acceptsInput(snapshot), ItemSearch.isInputFocused());
    }

    static boolean shouldForwardKeyPress(final boolean alreadyPressed, final int keyCode) {
        return !alreadyPressed || !ignoreRepeat(keyCode);
    }

    static boolean shouldForwardKeyRelease(final boolean wasPressed) {
        return wasPressed;
    }

    private static boolean ignoreRepeat(final int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT_CONTROL,
                 GLFW.GLFW_KEY_RIGHT_CONTROL,
                 GLFW.GLFW_KEY_LEFT_ALT,
                 GLFW.GLFW_KEY_RIGHT_ALT,
                 GLFW.GLFW_KEY_LEFT_SHIFT,
                 GLFW.GLFW_KEY_RIGHT_SHIFT,
                 GLFW.GLFW_KEY_LEFT_SUPER,
                 GLFW.GLFW_KEY_RIGHT_SUPER -> true;
            default -> false;
        };
    }

    static int imageWidth(final TerminalScreenSnapshot snapshot) {
        if (!acceptsInput(snapshot)) {
            return DEFAULT_IMAGE_WIDTH;
        }
        return Math.max(DEFAULT_IMAGE_WIDTH, TEXT_LEFT + snapshot.width() * CELL_WIDTH + TEXT_RIGHT_MARGIN);
    }

    static int imageHeight(final TerminalScreenSnapshot snapshot) {
        if (!acceptsInput(snapshot)) {
            return DEFAULT_IMAGE_HEIGHT;
        }
        return Math.max(DEFAULT_IMAGE_HEIGHT, TEXT_TOP + snapshot.height() * LINE_HEIGHT + TEXT_BOTTOM_MARGIN);
    }

    static int visibleRows(final TerminalScreenSnapshot snapshot) {
        if (!acceptsInput(snapshot)) {
            return 0;
        }
        return Math.min(snapshot.height(), Math.max(0, (imageHeight(snapshot) - TEXT_TOP - TEXT_BOTTOM_MARGIN) / LINE_HEIGHT));
    }

    static int textColor(final TerminalScreenSnapshot snapshot, final int column, final int row) {
        return opaqueArgb(snapshot == null ? 0xFFFFFF : snapshot.foregroundColor(column, row));
    }

    static int backgroundColor(final TerminalScreenSnapshot snapshot, final int column, final int row) {
        return opaqueArgb(snapshot == null ? 0 : snapshot.backgroundColor(column, row));
    }

    static List<TextRun> textRuns(final TerminalScreenSnapshot snapshot, final int row) {
        if (!acceptsInput(snapshot) || row < 0 || row >= snapshot.height()) {
            return List.of();
        }
        final String line = snapshotLine(snapshot, row);
        final int width = Math.min(snapshot.width(), line.codePointCount(0, line.length()));
        if (width <= 0) {
            return List.of();
        }
        final List<TextRun> runs = new ArrayList<>();
        int runColumn = 0;
        int runColor = textColor(snapshot, 0, row);
        final StringBuilder runText = new StringBuilder();
        int offset = 0;
        for (int column = 0; column < width && offset < line.length(); column++) {
            final int codePoint = line.codePointAt(offset);
            final int color = textColor(snapshot, column, row);
            if (color != runColor && !runText.isEmpty()) {
                runs.add(new TextRun(runColumn, runText.toString(), runColor));
                runColumn = column;
                runColor = color;
                runText.setLength(0);
            }
            runText.appendCodePoint(codePoint);
            offset += Character.charCount(codePoint);
        }
        if (!runText.isEmpty()) {
            runs.add(new TextRun(runColumn, runText.toString(), runColor));
        }
        return runs;
    }

    static TerminalKeyPayload keyPayload(final TerminalMenu menu, final boolean pressed, final char character, final int keyCode) {
        return new TerminalKeyPayload(menu.containerId, pressed, character, openComputersKeyCode(keyCode));
    }

    static TerminalClipboardPayload clipboardPayload(final TerminalMenu menu, final String value) {
        return new TerminalClipboardPayload(menu.containerId, value);
    }

    static List<TerminalClipboardPayload> clipboardPayloads(final TerminalMenu menu, final String value) {
        if (value == null || value.isEmpty() || value.length() > CLIPBOARD_MAX_LENGTH) {
            return List.of();
        }
        final List<TerminalClipboardPayload> payloads = new ArrayList<>();
        for (int offset = 0; offset < value.length(); offset += CLIPBOARD_CHUNK_SIZE) {
            payloads.add(clipboardPayload(menu, value.substring(offset, Math.min(value.length(), offset + CLIPBOARD_CHUNK_SIZE))));
        }
        return payloads;
    }

    static TerminalMousePayload mousePayload(final TerminalMenu menu, final int kind, final double mouseX, final double mouseY, final int buttonOrDelta, final int left, final int top) {
        final double column = Math.floor((mouseX - left - TEXT_LEFT) / CELL_WIDTH);
        final double row = Math.floor((mouseY - top - TEXT_TOP) / LINE_HEIGHT);
        return new TerminalMousePayload(menu.containerId, kind, column, row, buttonOrDelta);
    }

    static TerminalMousePayload mousePayload(final TerminalMenu menu, final int kind, final double mouseX, final double mouseY, final int buttonOrDelta, final int left, final int top, final TerminalScreenSnapshot snapshot) {
        if (!acceptsInput(snapshot)) {
            return null;
        }
        final TerminalMousePayload payload = mousePayload(menu, kind, mouseX, mouseY, buttonOrDelta, left, top);
        if (payload.x() < 0 || payload.y() < 0 || payload.x() >= snapshot.width() || payload.y() >= snapshot.height()) {
            if (kind == TerminalMousePayload.MOUSE_UP) {
                return new TerminalMousePayload(menu.containerId, kind, -1.0D, -1.0D, buttonOrDelta);
            }
            return null;
        }
        return payload;
    }

    private void sendKeyInput(final boolean pressed, final char character, final int keyCode) {
        if (acceptsInput(menu.snapshot())) {
            PacketDistributor.sendToServer(keyPayload(menu, pressed, character, keyCode));
        }
    }

    private void sendClipboardInput(final String value) {
        if (acceptsInput(menu.snapshot())) {
            for (final TerminalClipboardPayload payload : clipboardPayloads(menu, value)) {
                PacketDistributor.sendToServer(payload);
            }
        }
    }

    private void sendMouseInput(final int kind, final double mouseX, final double mouseY, final int buttonOrDelta) {
        final TerminalMousePayload payload = mousePayload(menu, kind, mouseX, mouseY, buttonOrDelta, leftPos, topPos, menu.snapshot());
        if (payload != null) {
            PacketDistributor.sendToServer(payload);
        }
    }

    private void renderCellBackgrounds(final GuiGraphics guiGraphics, final TerminalScreenSnapshot snapshot, final int left, final int top) {
        for (int row = 0; row < visibleRows(snapshot); row++) {
            final int y = top + TEXT_TOP + row * LINE_HEIGHT;
            for (int column = 0; column < snapshot.width(); column++) {
                final int color = backgroundColor(snapshot, column, row);
                if ((color & 0x00FFFFFF) != 0) {
                    final int x = left + TEXT_LEFT + column * CELL_WIDTH;
                    guiGraphics.fill(x, y, x + CELL_WIDTH, y + LINE_HEIGHT, color);
                }
            }
        }
    }

    private void updateLayoutForSnapshot() {
        final int nextImageWidth = imageWidth(menu.snapshot());
        final int nextImageHeight = imageHeight(menu.snapshot());
        if (imageWidth != nextImageWidth || imageHeight != nextImageHeight) {
            imageWidth = nextImageWidth;
            imageHeight = nextImageHeight;
            leftPos = (width - imageWidth) / 2;
            topPos = (height - imageHeight) / 2;
        }
    }

    static int openComputersKeyCode(final int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_ENTER -> 0x1C;
            case GLFW.GLFW_KEY_KP_ENTER -> 0x9C;
            case GLFW.GLFW_KEY_BACKSPACE -> 0x0E;
            case GLFW.GLFW_KEY_TAB -> 0x0F;
            case GLFW.GLFW_KEY_DELETE -> 0xD3;
            case GLFW.GLFW_KEY_DOWN -> 0xD0;
            case GLFW.GLFW_KEY_LEFT -> 0xCB;
            case GLFW.GLFW_KEY_RIGHT -> 0xCD;
            case GLFW.GLFW_KEY_UP -> 0xC8;
            case GLFW.GLFW_KEY_HOME -> 0xC7;
            case GLFW.GLFW_KEY_END -> 0xCF;
            case GLFW.GLFW_KEY_PAGE_DOWN -> 0xD1;
            case GLFW.GLFW_KEY_PAGE_UP -> 0xC9;
            case GLFW.GLFW_KEY_LEFT_CONTROL -> 0x1D;
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> 0x9D;
            case GLFW.GLFW_KEY_LEFT_ALT -> 0x38;
            case GLFW.GLFW_KEY_RIGHT_ALT -> 0xB8;
            case GLFW.GLFW_KEY_LEFT_SHIFT -> 0x2A;
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> 0x36;
            case GLFW.GLFW_KEY_SPACE -> 0x39;
            case GLFW.GLFW_KEY_C -> 0x2E;
            case GLFW.GLFW_KEY_D -> 0x20;
            case GLFW.GLFW_KEY_Q -> 0x10;
            case GLFW.GLFW_KEY_W -> 0x11;
            default -> keyCode;
        };
    }

    private static int opaqueArgb(final int color) {
        return 0xFF000000 | (color & 0x00FFFFFF);
    }
}
