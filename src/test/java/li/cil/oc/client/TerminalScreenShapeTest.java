package li.cil.oc.client;

import li.cil.oc.common.component.TerminalScreenSnapshot;
import li.cil.oc.common.menu.TerminalMenu;
import li.cil.oc.common.network.TerminalClipboardPayload;
import li.cil.oc.common.network.TerminalKeyPayload;
import li.cil.oc.common.network.TerminalMousePayload;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TerminalScreenShapeTest {
    @Test
    void terminalScreenHasMenuConstructor() throws NoSuchMethodException {
        final Constructor<TerminalScreen> constructor = TerminalScreen.class.getConstructor(
            TerminalMenu.class,
            Inventory.class,
            Component.class);

        assertTrue(AbstractContainerScreen.class.isAssignableFrom(TerminalScreen.class));
        assertArrayEquals(new Class<?>[]{TerminalMenu.class, Inventory.class, Component.class}, constructor.getParameterTypes());
    }

    @Test
    void terminalScreenHasSnapshotLineHelper() throws NoSuchMethodException {
        final Method method = TerminalScreen.class.getDeclaredMethod("snapshotLine", TerminalScreenSnapshot.class, int.class);

        assertEquals(String.class, method.getReturnType());
    }

    @Test
    void terminalScreenExposesSnapshotStatusHelpers() throws NoSuchMethodException {
        final Method hasVisibleText = TerminalScreen.class.getDeclaredMethod("hasVisibleText", TerminalScreenSnapshot.class);
        final Method statusLabel = TerminalScreen.class.getDeclaredMethod("statusLabel", TerminalScreenSnapshot.class);
        final Method acceptsInput = TerminalScreen.class.getDeclaredMethod("acceptsInput", TerminalScreenSnapshot.class);
        final Method imageWidth = TerminalScreen.class.getDeclaredMethod("imageWidth", TerminalScreenSnapshot.class);
        final Method imageHeight = TerminalScreen.class.getDeclaredMethod("imageHeight", TerminalScreenSnapshot.class);
        final Method visibleRows = TerminalScreen.class.getDeclaredMethod("visibleRows", TerminalScreenSnapshot.class);

        assertEquals(boolean.class, hasVisibleText.getReturnType());
        assertEquals(Component.class, statusLabel.getReturnType());
        assertEquals(boolean.class, acceptsInput.getReturnType());
        assertEquals(int.class, imageWidth.getReturnType());
        assertEquals(int.class, imageHeight.getReturnType());
        assertEquals(int.class, visibleRows.getReturnType());
    }

    @Test
    void terminalScreenStatusDistinguishesMissingAndBlankSnapshots() {
        final TerminalScreenSnapshot missing = new TerminalScreenSnapshot(0, 0, new String[0]);
        final TerminalScreenSnapshot blank = new TerminalScreenSnapshot(4, 2, new String[]{"", "   "});
        final TerminalScreenSnapshot visible = new TerminalScreenSnapshot(4, 2, new String[]{"neo", ""});

        assertEquals(false, TerminalScreen.hasVisibleText(missing));
        assertEquals(false, TerminalScreen.hasVisibleText(blank));
        assertEquals(true, TerminalScreen.hasVisibleText(visible));
        assertTranslationKey("gui.neoopencomputers.terminal.no_screen_data", TerminalScreen.statusLabel(missing));
        assertTranslationKey("gui.neoopencomputers.terminal.blank_screen", TerminalScreen.statusLabel(blank));
        assertEquals(null, TerminalScreen.statusLabel(visible));
    }

    @Test
    void terminalScreenTreatsColoredBackgroundAsVisibleContent() {
        final TerminalScreenSnapshot visibleBackground = new TerminalScreenSnapshot(
            2,
            1,
            new String[]{"  "},
            new int[][]{{0xFFFFFF, 0xFFFFFF}},
            new int[][]{{0x000000, 0x223344}});

        assertEquals(true, TerminalScreen.hasVisibleText(visibleBackground));
        assertEquals(null, TerminalScreen.statusLabel(visibleBackground));
    }

    @Test
    void terminalScreenAcceptsInputOnlyWithScreenDimensions() {
        assertEquals(false, TerminalScreen.acceptsInput(null));
        assertEquals(false, TerminalScreen.acceptsInput(new TerminalScreenSnapshot(0, 0, new String[0])));
        assertEquals(true, TerminalScreen.acceptsInput(new TerminalScreenSnapshot(4, 2, new String[]{"", ""})));
    }

    @Test
    void terminalScreenCapturesKeyboardInputEvenWhenItemSearchInputFocused() {
        assertEquals(true, TerminalScreen.shouldForwardKeyboardInput(true, false));
        assertEquals(false, TerminalScreen.shouldForwardKeyboardInput(false, false));
        assertEquals(true, TerminalScreen.shouldForwardKeyboardInput(true, true));
    }

    @Test
    void terminalScreenHandlesTypingBeforeMinecraftGuiShortcuts() {
        assertEquals(true, TerminalScreen.shouldHandleBeforeScreenShortcuts(true, false, GLFW.GLFW_KEY_E));
        assertEquals(true, TerminalScreen.shouldHandleBeforeScreenShortcuts(true, false, GLFW.GLFW_KEY_W));
        assertEquals(false, TerminalScreen.shouldHandleBeforeScreenShortcuts(true, false, GLFW.GLFW_KEY_ESCAPE));
        assertEquals(false, TerminalScreen.shouldHandleBeforeScreenShortcuts(true, false, GLFW.GLFW_KEY_F11));
        assertEquals(false, TerminalScreen.shouldHandleBeforeScreenShortcuts(false, false, GLFW.GLFW_KEY_E));
        assertEquals(true, TerminalScreen.shouldHandleBeforeScreenShortcuts(true, true, GLFW.GLFW_KEY_E));
    }

    @Test
    void terminalScreenDoesNotForwardF11LikeUpstreamInputBuffer() {
        assertEquals(false, TerminalScreen.shouldForwardTerminalKey(GLFW.GLFW_KEY_ESCAPE));
        assertEquals(false, TerminalScreen.shouldForwardTerminalKey(GLFW.GLFW_KEY_F11));
        assertEquals(true, TerminalScreen.shouldForwardTerminalKey(GLFW.GLFW_KEY_F12));
        assertEquals(true, TerminalScreen.shouldForwardTerminalKey(GLFW.GLFW_KEY_E));
    }

    @Test
    void terminalScreenSizesPanelForSnapshotDimensions() {
        final TerminalScreenSnapshot missing = new TerminalScreenSnapshot(0, 0, new String[0]);
        final TerminalScreenSnapshot terminalServerDefault = new TerminalScreenSnapshot(80, 25, new String[25]);

        assertEquals(248, TerminalScreen.imageWidth(missing));
        assertEquals(166, TerminalScreen.imageHeight(missing));
        assertEquals(504, TerminalScreen.imageWidth(terminalServerDefault));
        assertEquals(259, TerminalScreen.imageHeight(terminalServerDefault));
    }

    @Test
    void terminalScreenFitsPanelInsideAvailableGuiArea() {
        final TerminalScreenSnapshot terminalServerDefault = new TerminalScreenSnapshot(80, 25, new String[25]);

        assertEquals(409, TerminalScreen.imageWidth(terminalServerDefault, 427));
        assertEquals(222, TerminalScreen.imageHeight(terminalServerDefault, 240));
    }

    @Test
    void terminalScreenRendersAllSnapshotRowsThatFitAdaptivePanel() {
        assertEquals(0, TerminalScreen.visibleRows(new TerminalScreenSnapshot(0, 0, new String[0])));
        assertEquals(25, TerminalScreen.visibleRows(new TerminalScreenSnapshot(80, 25, new String[25])));
    }

    @Test
    void terminalScreenLimitsVisibleColumnsAndRowsToClampedPanel() {
        final TerminalScreenSnapshot terminalServerDefault = new TerminalScreenSnapshot(80, 25, new String[25]);

        assertEquals(64, TerminalScreen.visibleColumns(terminalServerDefault, 409));
        assertEquals(20, TerminalScreen.visibleRows(terminalServerDefault, 222));
    }

    @Test
    void terminalScreenConvertsSnapshotColorsToOpaqueArgb() {
        final TerminalScreenSnapshot snapshot = new TerminalScreenSnapshot(
            2,
            1,
            new String[]{"ab"},
            new int[][]{{0x112233, 0x445566}},
            new int[][]{{0x010203, 0x040506}});

        assertEquals(0xFF112233, TerminalScreen.textColor(snapshot, 0, 0));
        assertEquals(0xFF040506, TerminalScreen.backgroundColor(snapshot, 1, 0));
    }

    @Test
    void terminalScreenGroupsTextRunsByForegroundColor() {
        final TerminalScreenSnapshot snapshot = new TerminalScreenSnapshot(
            4,
            1,
            new String[]{"ABCD"},
            new int[][]{{0x111111, 0x111111, 0x222222, 0x111111}},
            new int[][]{{0, 0, 0, 0}});

        final List<TerminalScreen.TextRun> runs = TerminalScreen.textRuns(snapshot, 0);

        assertEquals(3, runs.size());
        assertEquals(new TerminalScreen.TextRun(0, "AB", 0xFF111111), runs.get(0));
        assertEquals(new TerminalScreen.TextRun(2, "C", 0xFF222222), runs.get(1));
        assertEquals(new TerminalScreen.TextRun(3, "D", 0xFF111111), runs.get(2));
    }

    @Test
    void terminalScreenExposesFixedWidthTextCellsForTerminalAlignment() {
        final TerminalScreenSnapshot snapshot = new TerminalScreenSnapshot(
            3,
            1,
            new String[]{"Wi."},
            new int[][]{{0x111111, 0x222222, 0x222222}},
            new int[][]{{0, 0, 0}});

        final List<TerminalScreen.TextCell> cells = TerminalScreen.textCells(snapshot, 0, 3);

        assertEquals(List.of(
            new TerminalScreen.TextCell(0, "W", 0xFF111111),
            new TerminalScreen.TextCell(1, "i", 0xFF222222),
            new TerminalScreen.TextCell(2, ".", 0xFF222222)), cells);
    }

    @Test
    void terminalScreenDrawsGlyphsAtFixedCellOriginForMonospaceAlignment() {
        assertEquals(0, TerminalScreen.centeredCellOffset(1));
        assertEquals(0, TerminalScreen.centeredCellOffset(6));
        assertEquals(0, TerminalScreen.centeredCellOffset(8));
    }

    @Test
    void terminalScreenUsesBundledOpenComputersBitmapFontForCellGlyphs() throws IOException {
        final String screen = Files.readString(Path.of("src/main/java/li/cil/oc/client/TerminalScreen.java"));

        assertTrue(Files.exists(Path.of("src/main/resources/assets/neoopencomputers/font.hex")));
        assertTrue(TerminalFont.hasGlyph('i'));
        assertTrue(TerminalFont.hasGlyph('W'));
        assertTrue(TerminalFont.hasGlyph(0x754C));
        assertEquals(6, TerminalFont.cellWidth());
        assertEquals(9, TerminalFont.cellHeight());
        assertEquals(6, TerminalFont.glyphCellWidth('i'));
        assertEquals(12, TerminalFont.glyphCellWidth(0x754C));
        assertTrue(screen.contains("TerminalFont.drawGuiCell"), "Terminal GUI should render fixed bitmap cells, not proportional Minecraft glyphs");
    }

    @Test
    void terminalScreenTextRunsPreserveSupplementaryCodePoints() {
        final String supplementary = new String(Character.toChars(0x10400));
        final TerminalScreenSnapshot snapshot = new TerminalScreenSnapshot(
            1,
            1,
            new String[]{supplementary},
            new int[][]{{0x334455}},
            new int[][]{{0}});

        final List<TerminalScreen.TextRun> runs = TerminalScreen.textRuns(snapshot, 0);

        assertEquals(List.of(new TerminalScreen.TextRun(0, supplementary, 0xFF334455)), runs);
    }

    @Test
    void terminalScreenBuildsKeyPayloadForMenu() throws ReflectiveOperationException {
        final TerminalMenu menu = allocateMenu(12);

        final TerminalKeyPayload payload = TerminalScreen.keyPayload(menu, true, 'x', 999);

        assertEquals(12, payload.containerId());
        assertEquals(true, payload.pressed());
        assertEquals((int) 'x', payload.character());
        assertEquals(999, payload.keyCode());
    }

    @Test
    void terminalScreenMapsGlfwEnterToOpenComputersKeyCode() throws ReflectiveOperationException {
        final TerminalMenu menu = allocateMenu(12);

        final TerminalKeyPayload payload = TerminalScreen.keyPayload(menu, true, (char) 0, GLFW.GLFW_KEY_ENTER);

        assertEquals(0x1C, payload.keyCode());
    }

    @Test
    void terminalScreenMapsPrintableCharactersToOpenComputersKeyCode() throws ReflectiveOperationException {
        final TerminalMenu menu = allocateMenu(12);

        assertEquals(0x1E, TerminalScreen.keyPayload(menu, true, 'a', 0).keyCode());
        assertEquals(0x02, TerminalScreen.keyPayload(menu, true, '!', 0).keyCode());
        assertEquals(0x39, TerminalScreen.keyPayload(menu, true, ' ', 0).keyCode());
    }

    @Test
    void terminalScreenKeepsTypedCharactersPressedUntilKeyRelease() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/client/TerminalScreen.java"));

        assertFalse(source.contains("sendKeyInput(false, codePoint, 0)"));
        assertTrue(source.contains("pressedKeys.put(keyCode, codePoint)"));
    }

    @Test
    void terminalScreenMapsPrintableGlfwKeysToOpenComputersLegacyCodes() {
        assertEquals(0x01, TerminalScreen.openComputersKeyCode(GLFW.GLFW_KEY_ESCAPE));
        assertEquals(0x02, TerminalScreen.openComputersKeyCode(GLFW.GLFW_KEY_1));
        assertEquals(0x0B, TerminalScreen.openComputersKeyCode(GLFW.GLFW_KEY_0));
        assertEquals(0x0C, TerminalScreen.openComputersKeyCode(GLFW.GLFW_KEY_MINUS));
        assertEquals(0x0D, TerminalScreen.openComputersKeyCode(GLFW.GLFW_KEY_EQUAL));
        assertEquals(0x10, TerminalScreen.openComputersKeyCode(GLFW.GLFW_KEY_Q));
        assertEquals(0x11, TerminalScreen.openComputersKeyCode(GLFW.GLFW_KEY_W));
        assertEquals(0x1E, TerminalScreen.openComputersKeyCode(GLFW.GLFW_KEY_A));
        assertEquals(0x2C, TerminalScreen.openComputersKeyCode(GLFW.GLFW_KEY_Z));
        assertEquals(0x33, TerminalScreen.openComputersKeyCode(GLFW.GLFW_KEY_COMMA));
        assertEquals(0x35, TerminalScreen.openComputersKeyCode(GLFW.GLFW_KEY_SLASH));
    }

    @Test
    void terminalScreenMapsFunctionAndNavigationKeysToOpenComputersLegacyCodes() {
        assertEquals(0x3B, TerminalScreen.openComputersKeyCode(GLFW.GLFW_KEY_F1));
        assertEquals(0x44, TerminalScreen.openComputersKeyCode(GLFW.GLFW_KEY_F10));
        assertEquals(0x57, TerminalScreen.openComputersKeyCode(GLFW.GLFW_KEY_F11));
        assertEquals(0x58, TerminalScreen.openComputersKeyCode(GLFW.GLFW_KEY_F12));
        assertEquals(0xC7, TerminalScreen.openComputersKeyCode(GLFW.GLFW_KEY_HOME));
        assertEquals(0xC8, TerminalScreen.openComputersKeyCode(GLFW.GLFW_KEY_UP));
        assertEquals(0xC9, TerminalScreen.openComputersKeyCode(GLFW.GLFW_KEY_PAGE_UP));
        assertEquals(0xD2, TerminalScreen.openComputersKeyCode(GLFW.GLFW_KEY_INSERT));
        assertEquals(0xD3, TerminalScreen.openComputersKeyCode(GLFW.GLFW_KEY_DELETE));
    }

    @Test
    void terminalScreenMapsKeypadKeysToOpenComputersLegacyCodes() {
        assertEquals(0x37, TerminalScreen.openComputersKeyCode(GLFW.GLFW_KEY_KP_MULTIPLY));
        assertEquals(0x47, TerminalScreen.openComputersKeyCode(GLFW.GLFW_KEY_KP_7));
        assertEquals(0x4A, TerminalScreen.openComputersKeyCode(GLFW.GLFW_KEY_KP_SUBTRACT));
        assertEquals(0x4F, TerminalScreen.openComputersKeyCode(GLFW.GLFW_KEY_KP_1));
        assertEquals(0x52, TerminalScreen.openComputersKeyCode(GLFW.GLFW_KEY_KP_0));
        assertEquals(0x53, TerminalScreen.openComputersKeyCode(GLFW.GLFW_KEY_KP_DECIMAL));
        assertEquals(0x9C, TerminalScreen.openComputersKeyCode(GLFW.GLFW_KEY_KP_ENTER));
        assertEquals(0xB5, TerminalScreen.openComputersKeyCode(GLFW.GLFW_KEY_KP_DIVIDE));
    }

    @Test
    void terminalScreenSuppressesRepeatedModifierKeysLikeUpstreamInputBuffer() {
        assertEquals(false, TerminalScreen.shouldForwardKeyPress(true, GLFW.GLFW_KEY_LEFT_CONTROL));
        assertEquals(false, TerminalScreen.shouldForwardKeyPress(true, GLFW.GLFW_KEY_RIGHT_CONTROL));
        assertEquals(false, TerminalScreen.shouldForwardKeyPress(true, GLFW.GLFW_KEY_LEFT_ALT));
        assertEquals(false, TerminalScreen.shouldForwardKeyPress(true, GLFW.GLFW_KEY_RIGHT_ALT));
        assertEquals(false, TerminalScreen.shouldForwardKeyPress(true, GLFW.GLFW_KEY_LEFT_SHIFT));
        assertEquals(false, TerminalScreen.shouldForwardKeyPress(true, GLFW.GLFW_KEY_RIGHT_SHIFT));
        assertEquals(false, TerminalScreen.shouldForwardKeyPress(true, GLFW.GLFW_KEY_LEFT_SUPER));
        assertEquals(false, TerminalScreen.shouldForwardKeyPress(true, GLFW.GLFW_KEY_RIGHT_SUPER));

        assertEquals(true, TerminalScreen.shouldForwardKeyPress(false, GLFW.GLFW_KEY_LEFT_CONTROL));
        assertEquals(true, TerminalScreen.shouldForwardKeyPress(true, GLFW.GLFW_KEY_W));
    }

    @Test
    void terminalScreenSendsKeyUpOnlyForTrackedPressedKeysLikeUpstreamInputBuffer() {
        assertEquals(true, TerminalScreen.shouldForwardKeyRelease(true));
        assertEquals(false, TerminalScreen.shouldForwardKeyRelease(false));
    }

    @Test
    void terminalScreenReleasesTrackedKeysWhenClosedLikeUpstreamInputBuffer() throws ReflectiveOperationException {
        final TerminalMenu menu = allocateMenu(12);
        final Map<Integer, Character> pressedKeys = new LinkedHashMap<>();
        pressedKeys.put(GLFW.GLFW_KEY_A, 'a');
        pressedKeys.put(GLFW.GLFW_KEY_LEFT_SHIFT, (char) 0);

        final List<TerminalKeyPayload> payloads = TerminalScreen.releaseKeyPayloads(menu, pressedKeys);

        assertEquals(2, payloads.size());
        assertEquals(new TerminalKeyPayload(12, false, 'a', 0x1E), payloads.get(0));
        assertEquals(new TerminalKeyPayload(12, false, 0, 0x2A), payloads.get(1));
    }

    @Test
    void terminalScreenBuildsClipboardPayloadForMenu() throws ReflectiveOperationException {
        final TerminalMenu menu = allocateMenu(12);

        final TerminalClipboardPayload payload = TerminalScreen.clipboardPayload(menu, "alpha");

        assertEquals(12, payload.containerId());
        assertEquals("alpha", payload.value());
    }

    @Test
    void terminalScreenSplitsClipboardPayloadsLikeUpstream() throws ReflectiveOperationException {
        final TerminalMenu menu = allocateMenu(12);
        final Method method = TerminalScreen.class.getDeclaredMethod("clipboardPayloads", TerminalMenu.class, String.class);
        final String value = "x".repeat(16 * 1024) + "y";

        @SuppressWarnings("unchecked")
        final List<TerminalClipboardPayload> payloads = (List<TerminalClipboardPayload>) method.invoke(null, menu, value);

        assertEquals(2, payloads.size());
        assertEquals(12, payloads.get(0).containerId());
        assertEquals("x".repeat(16 * 1024), payloads.get(0).value());
        assertEquals("y", payloads.get(1).value());
    }

    @Test
    void terminalScreenDropsClipboardPayloadsAboveUpstreamLimit() throws ReflectiveOperationException {
        final TerminalMenu menu = allocateMenu(12);
        final Method method = TerminalScreen.class.getDeclaredMethod("clipboardPayloads", TerminalMenu.class, String.class);
        final String value = "x".repeat(64 * 1024 + 1);

        @SuppressWarnings("unchecked")
        final List<TerminalClipboardPayload> payloads = (List<TerminalClipboardPayload>) method.invoke(null, menu, value);

        assertEquals(List.of(), payloads);
    }

    @Test
    void terminalScreenTreatsInsertAsDefaultClipboardPasteKeyLikeUpstream() {
        assertEquals(true, TerminalScreen.shouldPasteClipboardForKey(GLFW.GLFW_KEY_INSERT, false));
        assertEquals(true, TerminalScreen.shouldPasteClipboardForKey(GLFW.GLFW_KEY_V, true));
        assertEquals(false, TerminalScreen.shouldPasteClipboardForKey(GLFW.GLFW_KEY_V, false));
        assertEquals(false, TerminalScreen.shouldPasteClipboardForKey(GLFW.GLFW_KEY_A, false));
    }

    @Test
    void terminalScreenTreatsMiddleMouseAsClipboardPasteLikeUpstream() {
        assertEquals(true, TerminalScreen.shouldPasteClipboardForMouseButton(2));
        assertEquals(false, TerminalScreen.shouldPasteClipboardForMouseButton(0));
        assertEquals(false, TerminalScreen.shouldPasteClipboardForMouseButton(1));
    }

    @Test
    void terminalScreenOnlyForwardsLeftAndRightMouseClicksLikeUpstream() {
        assertEquals(true, TerminalScreen.shouldForwardTerminalMouseButton(0));
        assertEquals(true, TerminalScreen.shouldForwardTerminalMouseButton(1));
        assertEquals(false, TerminalScreen.shouldForwardTerminalMouseButton(2));
        assertEquals(false, TerminalScreen.shouldForwardTerminalMouseButton(3));
        assertEquals(false, TerminalScreen.shouldForwardTerminalMouseButton(-1));
    }

    @Test
    void terminalScreenIgnoresZeroScrollLikeUpstream() {
        assertEquals(-1, TerminalScreen.terminalScrollDelta(-4.0D));
        assertEquals(0, TerminalScreen.terminalScrollDelta(0.0D));
        assertEquals(1, TerminalScreen.terminalScrollDelta(2.0D));
        assertEquals(false, TerminalScreen.shouldForwardTerminalScroll(0));
        assertEquals(true, TerminalScreen.shouldForwardTerminalScroll(1));
        assertEquals(true, TerminalScreen.shouldForwardTerminalScroll(-1));
    }

    @Test
    void terminalScreenBuildsMousePayloadForMenuCoordinates() throws ReflectiveOperationException {
        final TerminalMenu menu = allocateMenu(12);

        final TerminalMousePayload payload = TerminalScreen.mousePayload(menu, TerminalMousePayload.MOUSE_DOWN, 34, 60, 0, 10, 20);

        assertEquals(12, payload.containerId());
        assertEquals(TerminalMousePayload.MOUSE_DOWN, payload.kind());
        assertEquals(2.0D, payload.x());
        assertEquals(2.0D, payload.y());
        assertEquals(0, payload.buttonOrDelta());
    }

    @Test
    void terminalScreenBuildsBoundedMousePayloadForSnapshot() throws ReflectiveOperationException {
        final TerminalMenu menu = allocateMenu(12);
        final TerminalScreenSnapshot snapshot = new TerminalScreenSnapshot(4, 2, new String[]{"neo", "oc"});

        final TerminalMousePayload payload = TerminalScreen.mousePayload(menu, TerminalMousePayload.MOUSE_DOWN, 45, 47, 0, 10, 20, snapshot);

        assertEquals(12, payload.containerId());
        assertEquals(23.0D / 6.0D, payload.x(), 1.0E-6D);
        assertEquals(5.0D / 9.0D, payload.y(), 1.0E-6D);
    }

    @Test
    void terminalScreenDropsMousePayloadOutsideSnapshot() throws ReflectiveOperationException {
        final TerminalMenu menu = allocateMenu(12);
        final TerminalScreenSnapshot snapshot = new TerminalScreenSnapshot(4, 2, new String[]{"neo", "oc"});

        assertEquals(null, TerminalScreen.mousePayload(menu, TerminalMousePayload.MOUSE_DOWN, 9, 30, 0, 10, 20, snapshot));
        assertEquals(null, TerminalScreen.mousePayload(menu, TerminalMousePayload.MOUSE_DOWN, 47, 30, 0, 10, 20, snapshot));
        assertEquals(null, TerminalScreen.mousePayload(menu, TerminalMousePayload.MOUSE_DOWN, 20, 40, 0, 10, 20, new TerminalScreenSnapshot(0, 0, new String[0])));
    }

    @Test
    void terminalScreenDropsMousePayloadWhenMenuDoesNotSupportMouseLikeUpstreamHasMouse() throws ReflectiveOperationException {
        final TerminalMenu menu = allocateMenu(12);
        setField(menu, "supportsMouseInput", false);
        final TerminalScreenSnapshot snapshot = new TerminalScreenSnapshot(4, 2, new String[]{"neo", "oc"});

        assertEquals(null, TerminalScreen.mousePayload(menu, TerminalMousePayload.MOUSE_DOWN, 45, 47, 0, 10, 20, snapshot));
        assertEquals(null, TerminalScreen.mousePayload(menu, TerminalMousePayload.MOUSE_SCROLL, 45, 47, 1, 10, 20, snapshot));
    }

    @Test
    void terminalScreenSendsOutsideMouseUpLikeUpstream() throws ReflectiveOperationException {
        final TerminalMenu menu = allocateMenu(12);
        final TerminalScreenSnapshot snapshot = new TerminalScreenSnapshot(4, 2, new String[]{"neo", "oc"});

        final TerminalMousePayload payload = TerminalScreen.mousePayload(menu, TerminalMousePayload.MOUSE_UP, 9, 30, 0, 10, 20, snapshot);

        assertNotNull(payload);
        assertEquals(12, payload.containerId());
        assertEquals(TerminalMousePayload.MOUSE_UP, payload.kind());
        assertEquals(-1.0D, payload.x());
        assertEquals(-1.0D, payload.y());
        assertEquals(0, payload.buttonOrDelta());
    }

    private static TerminalMenu allocateMenu(final int containerId) throws ReflectiveOperationException {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        final TerminalMenu menu = (TerminalMenu) ((Unsafe) unsafeField.get(null)).allocateInstance(TerminalMenu.class);
        final Field containerIdField = net.minecraft.world.inventory.AbstractContainerMenu.class.getDeclaredField("containerId");
        containerIdField.setAccessible(true);
        containerIdField.setInt(menu, containerId);
        return menu;
    }

    private static void setField(final Object instance, final String name, final Object value) throws ReflectiveOperationException {
        final Field field = TerminalMenu.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(instance, value);
    }

    private static void assertTranslationKey(final String expected, final Component component) {
        assertTrue(component.getContents() instanceof TranslatableContents);
        assertEquals(expected, ((TranslatableContents) component.getContents()).getKey());
    }
}
