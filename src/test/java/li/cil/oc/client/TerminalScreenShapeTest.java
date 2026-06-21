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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void terminalScreenAcceptsInputOnlyWithScreenDimensions() {
        assertEquals(false, TerminalScreen.acceptsInput(null));
        assertEquals(false, TerminalScreen.acceptsInput(new TerminalScreenSnapshot(0, 0, new String[0])));
        assertEquals(true, TerminalScreen.acceptsInput(new TerminalScreenSnapshot(4, 2, new String[]{"", ""})));
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
    void terminalScreenRendersAllSnapshotRowsThatFitAdaptivePanel() {
        assertEquals(0, TerminalScreen.visibleRows(new TerminalScreenSnapshot(0, 0, new String[0])));
        assertEquals(25, TerminalScreen.visibleRows(new TerminalScreenSnapshot(80, 25, new String[25])));
    }

    @Test
    void terminalScreenBuildsKeyPayloadForMenu() throws ReflectiveOperationException {
        final TerminalMenu menu = allocateMenu(12);

        final TerminalKeyPayload payload = TerminalScreen.keyPayload(menu, true, 'x', 45);

        assertEquals(12, payload.containerId());
        assertEquals(true, payload.pressed());
        assertEquals((int) 'x', payload.character());
        assertEquals(45, payload.keyCode());
    }

    @Test
    void terminalScreenMapsGlfwEnterToOpenComputersKeyCode() throws ReflectiveOperationException {
        final TerminalMenu menu = allocateMenu(12);

        final TerminalKeyPayload payload = TerminalScreen.keyPayload(menu, true, (char) 0, GLFW.GLFW_KEY_ENTER);

        assertEquals(0x1C, payload.keyCode());
    }

    @Test
    void terminalScreenBuildsClipboardPayloadForMenu() throws ReflectiveOperationException {
        final TerminalMenu menu = allocateMenu(12);

        final TerminalClipboardPayload payload = TerminalScreen.clipboardPayload(menu, "alpha");

        assertEquals(12, payload.containerId());
        assertEquals("alpha", payload.value());
    }

    @Test
    void terminalScreenBuildsMousePayloadForMenuCoordinates() throws ReflectiveOperationException {
        final TerminalMenu menu = allocateMenu(12);

        final TerminalMousePayload payload = TerminalScreen.mousePayload(menu, TerminalMousePayload.MOUSE_DOWN, 34, 60, 0, 10, 20);

        assertEquals(12, payload.containerId());
        assertEquals(TerminalMousePayload.MOUSE_DOWN, payload.kind());
        assertEquals(3.0D, payload.x());
        assertEquals(3.0D, payload.y());
        assertEquals(0, payload.buttonOrDelta());
    }

    @Test
    void terminalScreenBuildsBoundedMousePayloadForSnapshot() throws ReflectiveOperationException {
        final TerminalMenu menu = allocateMenu(12);
        final TerminalScreenSnapshot snapshot = new TerminalScreenSnapshot(4, 2, new String[]{"neo", "oc"});

        final TerminalMousePayload payload = TerminalScreen.mousePayload(menu, TerminalMousePayload.MOUSE_DOWN, 45, 47, 0, 10, 20, snapshot);

        assertEquals(12, payload.containerId());
        assertEquals(4.0D, payload.x());
        assertEquals(1.0D, payload.y());
    }

    @Test
    void terminalScreenDropsMousePayloadOutsideSnapshot() throws ReflectiveOperationException {
        final TerminalMenu menu = allocateMenu(12);
        final TerminalScreenSnapshot snapshot = new TerminalScreenSnapshot(4, 2, new String[]{"neo", "oc"});

        assertEquals(null, TerminalScreen.mousePayload(menu, TerminalMousePayload.MOUSE_DOWN, 9, 30, 0, 10, 20, snapshot));
        assertEquals(null, TerminalScreen.mousePayload(menu, TerminalMousePayload.MOUSE_DOWN, 47, 30, 0, 10, 20, snapshot));
        assertEquals(null, TerminalScreen.mousePayload(menu, TerminalMousePayload.MOUSE_DOWN, 20, 40, 0, 10, 20, new TerminalScreenSnapshot(0, 0, new String[0])));
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

    private static void assertTranslationKey(final String expected, final Component component) {
        assertTrue(component.getContents() instanceof TranslatableContents);
        assertEquals(expected, ((TranslatableContents) component.getContents()).getKey());
    }
}
