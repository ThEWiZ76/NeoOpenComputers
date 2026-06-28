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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final Map<Integer, Character> pressedKeys = new HashMap<>();

    record TextRun(int column, String text, int color) {
    }

    record TextCell(int column, String text, int color) {
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
        guiGraphics.enableScissor(left + TEXT_LEFT, top + TEXT_TOP, left + imageWidth - TEXT_RIGHT_MARGIN, top + imageHeight - TEXT_BOTTOM_MARGIN);
        try {
            for (int row = 0; row < visibleRows(snapshot, imageHeight); row++) {
                for (final TextCell cell : textCells(snapshot, row, visibleColumns(snapshot, imageWidth))) {
                    if (!cell.text().isBlank()) {
                        guiGraphics.drawString(
                            font,
                            TerminalText.cell(cell.text()),
                            left + TEXT_LEFT + cell.column() * CELL_WIDTH + centeredCellOffset(font.width(cell.text())),
                            top + TEXT_TOP + row * LINE_HEIGHT,
                            cell.color(),
                            false);
                    }
                }
            }
        } finally {
            guiGraphics.disableScissor();
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
        if (shouldHandleBeforeScreenShortcuts(acceptsInput(menu.snapshot()), ItemSearch.isInputFocused(), keyCode)) {
            handleTerminalKeyPressed(keyCode);
            return true;
        }
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (!shouldForwardKeyboardInput(menu.snapshot())) {
            return false;
        }
        handleTerminalKeyPressed(keyCode);
        return true;
    }

    private void handleTerminalKeyPressed(final int keyCode) {
        if (hasControlDown() && keyCode == GLFW.GLFW_KEY_V && minecraft != null) {
            sendClipboardInput(minecraft.keyboardHandler.getClipboard());
            return;
        }
        if (shouldForwardKeyPress(pressedKeys.containsKey(keyCode), keyCode)) {
            sendKeyInput(true, (char) 0, keyCode);
        }
        pressedKeys.putIfAbsent(keyCode, (char) 0);
    }

    @Override
    public boolean keyReleased(final int keyCode, final int scanCode, final int modifiers) {
        if (!shouldForwardKeyboardInput(menu.snapshot())) {
            return false;
        }
        final Character character = pressedKeys.remove(keyCode);
        if (shouldForwardKeyRelease(character != null)) {
            sendKeyInput(false, character == null ? (char) 0 : character, keyCode);
        }
        return true;
    }

    @Override
    public boolean charTyped(final char codePoint, final int modifiers) {
        if (!shouldForwardKeyboardInput(menu.snapshot())) {
            return false;
        }
        final int keyCode = keyCodeForCharacter(codePoint, 0);
        pressedKeys.put(keyCode, codePoint);
        sendKeyInput(true, codePoint, keyCode);
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
        return acceptsInput;
    }

    static boolean shouldForwardKeyboardInput(final TerminalScreenSnapshot snapshot) {
        return shouldForwardKeyboardInput(acceptsInput(snapshot), ItemSearch.isInputFocused());
    }

    static boolean shouldHandleBeforeScreenShortcuts(final boolean acceptsInput, final boolean searchInputFocused, final int keyCode) {
        return shouldForwardKeyboardInput(acceptsInput, searchInputFocused) && keyCode != GLFW.GLFW_KEY_ESCAPE;
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

    static int imageWidth(final TerminalScreenSnapshot snapshot, final int availableWidth) {
        return Math.min(imageWidth(snapshot), Math.max(DEFAULT_IMAGE_WIDTH, availableWidth - 18));
    }

    static int imageHeight(final TerminalScreenSnapshot snapshot) {
        if (!acceptsInput(snapshot)) {
            return DEFAULT_IMAGE_HEIGHT;
        }
        return Math.max(DEFAULT_IMAGE_HEIGHT, TEXT_TOP + snapshot.height() * LINE_HEIGHT + TEXT_BOTTOM_MARGIN);
    }

    static int imageHeight(final TerminalScreenSnapshot snapshot, final int availableHeight) {
        return Math.min(imageHeight(snapshot), Math.max(DEFAULT_IMAGE_HEIGHT, availableHeight - 18));
    }

    static int visibleRows(final TerminalScreenSnapshot snapshot) {
        if (!acceptsInput(snapshot)) {
            return 0;
        }
        return visibleRows(snapshot, imageHeight(snapshot));
    }

    static int visibleRows(final TerminalScreenSnapshot snapshot, final int imageHeight) {
        if (!acceptsInput(snapshot)) {
            return 0;
        }
        return Math.min(snapshot.height(), Math.max(0, (imageHeight - TEXT_TOP - TEXT_BOTTOM_MARGIN) / LINE_HEIGHT));
    }

    static int visibleColumns(final TerminalScreenSnapshot snapshot, final int imageWidth) {
        if (!acceptsInput(snapshot)) {
            return 0;
        }
        return Math.min(snapshot.width(), Math.max(0, (imageWidth - TEXT_LEFT - TEXT_RIGHT_MARGIN) / CELL_WIDTH));
    }

    static int textColor(final TerminalScreenSnapshot snapshot, final int column, final int row) {
        return opaqueArgb(snapshot == null ? 0xFFFFFF : snapshot.foregroundColor(column, row));
    }

    static int backgroundColor(final TerminalScreenSnapshot snapshot, final int column, final int row) {
        return opaqueArgb(snapshot == null ? 0 : snapshot.backgroundColor(column, row));
    }

    static List<TextRun> textRuns(final TerminalScreenSnapshot snapshot, final int row) {
        return textRuns(snapshot, row, snapshot == null ? 0 : snapshot.width());
    }

    static List<TextRun> textRuns(final TerminalScreenSnapshot snapshot, final int row, final int visibleColumns) {
        if (!acceptsInput(snapshot) || row < 0 || row >= snapshot.height()) {
            return List.of();
        }
        final String line = snapshotLine(snapshot, row);
        final int width = Math.min(Math.min(snapshot.width(), visibleColumns), line.codePointCount(0, line.length()));
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

    static List<TextCell> textCells(final TerminalScreenSnapshot snapshot, final int row, final int visibleColumns) {
        if (!acceptsInput(snapshot) || row < 0 || row >= snapshot.height()) {
            return List.of();
        }
        final String line = snapshotLine(snapshot, row);
        final int width = Math.min(Math.min(snapshot.width(), visibleColumns), line.codePointCount(0, line.length()));
        if (width <= 0) {
            return List.of();
        }
        final List<TextCell> cells = new ArrayList<>(width);
        int offset = 0;
        for (int column = 0; column < width && offset < line.length(); column++) {
            final int codePoint = line.codePointAt(offset);
            cells.add(new TextCell(column, new String(Character.toChars(codePoint)), textColor(snapshot, column, row)));
            offset += Character.charCount(codePoint);
        }
        return cells;
    }

    static int centeredCellOffset(final int glyphWidth) {
        return 0;
    }

    static TerminalKeyPayload keyPayload(final TerminalMenu menu, final boolean pressed, final char character, final int keyCode) {
        return new TerminalKeyPayload(menu.containerId, pressed, character, openComputersKeyCode(keyCodeForCharacter(character, keyCode)));
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
        final int rows = visibleRows(snapshot, imageHeight);
        final int columns = visibleColumns(snapshot, imageWidth);
        for (int row = 0; row < rows; row++) {
            final int y = top + TEXT_TOP + row * LINE_HEIGHT;
            for (int column = 0; column < columns; column++) {
                final int color = backgroundColor(snapshot, column, row);
                if ((color & 0x00FFFFFF) != 0) {
                    final int x = left + TEXT_LEFT + column * CELL_WIDTH;
                    guiGraphics.fill(x, y, x + CELL_WIDTH, y + LINE_HEIGHT, color);
                }
            }
        }
    }

    private void updateLayoutForSnapshot() {
        final int nextImageWidth = imageWidth(menu.snapshot(), width);
        final int nextImageHeight = imageHeight(menu.snapshot(), height);
        if (imageWidth != nextImageWidth || imageHeight != nextImageHeight) {
            imageWidth = nextImageWidth;
            imageHeight = nextImageHeight;
            leftPos = (width - imageWidth) / 2;
            topPos = (height - imageHeight) / 2;
        }
    }

    static int openComputersKeyCode(final int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE -> 0x01;
            case GLFW.GLFW_KEY_1 -> 0x02;
            case GLFW.GLFW_KEY_2 -> 0x03;
            case GLFW.GLFW_KEY_3 -> 0x04;
            case GLFW.GLFW_KEY_4 -> 0x05;
            case GLFW.GLFW_KEY_5 -> 0x06;
            case GLFW.GLFW_KEY_6 -> 0x07;
            case GLFW.GLFW_KEY_7 -> 0x08;
            case GLFW.GLFW_KEY_8 -> 0x09;
            case GLFW.GLFW_KEY_9 -> 0x0A;
            case GLFW.GLFW_KEY_0 -> 0x0B;
            case GLFW.GLFW_KEY_MINUS -> 0x0C;
            case GLFW.GLFW_KEY_EQUAL -> 0x0D;
            case GLFW.GLFW_KEY_ENTER -> 0x1C;
            case GLFW.GLFW_KEY_KP_ENTER -> 0x9C;
            case GLFW.GLFW_KEY_BACKSPACE -> 0x0E;
            case GLFW.GLFW_KEY_TAB -> 0x0F;
            case GLFW.GLFW_KEY_A -> 0x1E;
            case GLFW.GLFW_KEY_B -> 0x30;
            case GLFW.GLFW_KEY_C -> 0x2E;
            case GLFW.GLFW_KEY_D -> 0x20;
            case GLFW.GLFW_KEY_E -> 0x12;
            case GLFW.GLFW_KEY_F -> 0x21;
            case GLFW.GLFW_KEY_G -> 0x22;
            case GLFW.GLFW_KEY_H -> 0x23;
            case GLFW.GLFW_KEY_I -> 0x17;
            case GLFW.GLFW_KEY_J -> 0x24;
            case GLFW.GLFW_KEY_K -> 0x25;
            case GLFW.GLFW_KEY_L -> 0x26;
            case GLFW.GLFW_KEY_M -> 0x32;
            case GLFW.GLFW_KEY_N -> 0x31;
            case GLFW.GLFW_KEY_O -> 0x18;
            case GLFW.GLFW_KEY_P -> 0x19;
            case GLFW.GLFW_KEY_Q -> 0x10;
            case GLFW.GLFW_KEY_R -> 0x13;
            case GLFW.GLFW_KEY_S -> 0x1F;
            case GLFW.GLFW_KEY_T -> 0x14;
            case GLFW.GLFW_KEY_U -> 0x16;
            case GLFW.GLFW_KEY_V -> 0x2F;
            case GLFW.GLFW_KEY_W -> 0x11;
            case GLFW.GLFW_KEY_X -> 0x2D;
            case GLFW.GLFW_KEY_Y -> 0x15;
            case GLFW.GLFW_KEY_Z -> 0x2C;
            case GLFW.GLFW_KEY_LEFT_BRACKET -> 0x1A;
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> 0x1B;
            case GLFW.GLFW_KEY_SEMICOLON -> 0x27;
            case GLFW.GLFW_KEY_APOSTROPHE -> 0x28;
            case GLFW.GLFW_KEY_GRAVE_ACCENT -> 0x29;
            case GLFW.GLFW_KEY_BACKSLASH -> 0x2B;
            case GLFW.GLFW_KEY_COMMA -> 0x33;
            case GLFW.GLFW_KEY_PERIOD -> 0x34;
            case GLFW.GLFW_KEY_SLASH -> 0x35;
            case GLFW.GLFW_KEY_DELETE -> 0xD3;
            case GLFW.GLFW_KEY_INSERT -> 0xD2;
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
            case GLFW.GLFW_KEY_CAPS_LOCK -> 0x3A;
            case GLFW.GLFW_KEY_F1 -> 0x3B;
            case GLFW.GLFW_KEY_F2 -> 0x3C;
            case GLFW.GLFW_KEY_F3 -> 0x3D;
            case GLFW.GLFW_KEY_F4 -> 0x3E;
            case GLFW.GLFW_KEY_F5 -> 0x3F;
            case GLFW.GLFW_KEY_F6 -> 0x40;
            case GLFW.GLFW_KEY_F7 -> 0x41;
            case GLFW.GLFW_KEY_F8 -> 0x42;
            case GLFW.GLFW_KEY_F9 -> 0x43;
            case GLFW.GLFW_KEY_F10 -> 0x44;
            case GLFW.GLFW_KEY_NUM_LOCK -> 0x45;
            case GLFW.GLFW_KEY_SCROLL_LOCK -> 0x46;
            case GLFW.GLFW_KEY_F11 -> 0x57;
            case GLFW.GLFW_KEY_F12 -> 0x58;
            case GLFW.GLFW_KEY_KP_7 -> 0x47;
            case GLFW.GLFW_KEY_KP_8 -> 0x48;
            case GLFW.GLFW_KEY_KP_9 -> 0x49;
            case GLFW.GLFW_KEY_KP_SUBTRACT -> 0x4A;
            case GLFW.GLFW_KEY_KP_4 -> 0x4B;
            case GLFW.GLFW_KEY_KP_5 -> 0x4C;
            case GLFW.GLFW_KEY_KP_6 -> 0x4D;
            case GLFW.GLFW_KEY_KP_ADD -> 0x4E;
            case GLFW.GLFW_KEY_KP_1 -> 0x4F;
            case GLFW.GLFW_KEY_KP_2 -> 0x50;
            case GLFW.GLFW_KEY_KP_3 -> 0x51;
            case GLFW.GLFW_KEY_KP_0 -> 0x52;
            case GLFW.GLFW_KEY_KP_DECIMAL -> 0x53;
            case GLFW.GLFW_KEY_KP_MULTIPLY -> 0x37;
            case GLFW.GLFW_KEY_KP_DIVIDE -> 0xB5;
            default -> keyCode;
        };
    }

    static int keyCodeForCharacter(final char character, final int fallbackKeyCode) {
        if (fallbackKeyCode != 0 || character == 0) {
            return fallbackKeyCode;
        }
        if (character >= 'a' && character <= 'z') {
            return GLFW.GLFW_KEY_A + (character - 'a');
        }
        if (character >= 'A' && character <= 'Z') {
            return GLFW.GLFW_KEY_A + (character - 'A');
        }
        if (character >= '1' && character <= '9') {
            return GLFW.GLFW_KEY_1 + (character - '1');
        }
        return switch (character) {
            case '0', ')' -> GLFW.GLFW_KEY_0;
            case '!' -> GLFW.GLFW_KEY_1;
            case '@' -> GLFW.GLFW_KEY_2;
            case '#' -> GLFW.GLFW_KEY_3;
            case '$' -> GLFW.GLFW_KEY_4;
            case '%' -> GLFW.GLFW_KEY_5;
            case '^' -> GLFW.GLFW_KEY_6;
            case '&' -> GLFW.GLFW_KEY_7;
            case '*' -> GLFW.GLFW_KEY_8;
            case '(' -> GLFW.GLFW_KEY_9;
            case '-', '_' -> GLFW.GLFW_KEY_MINUS;
            case '=', '+' -> GLFW.GLFW_KEY_EQUAL;
            case '[', '{' -> GLFW.GLFW_KEY_LEFT_BRACKET;
            case ']', '}' -> GLFW.GLFW_KEY_RIGHT_BRACKET;
            case ';', ':' -> GLFW.GLFW_KEY_SEMICOLON;
            case '\'', '"' -> GLFW.GLFW_KEY_APOSTROPHE;
            case '`', '~' -> GLFW.GLFW_KEY_GRAVE_ACCENT;
            case '\\', '|' -> GLFW.GLFW_KEY_BACKSLASH;
            case ',', '<' -> GLFW.GLFW_KEY_COMMA;
            case '.', '>' -> GLFW.GLFW_KEY_PERIOD;
            case '/', '?' -> GLFW.GLFW_KEY_SLASH;
            case ' ' -> GLFW.GLFW_KEY_SPACE;
            default -> fallbackKeyCode;
        };
    }

    private static int opaqueArgb(final int color) {
        return 0xFF000000 | (color & 0x00FFFFFF);
    }
}
