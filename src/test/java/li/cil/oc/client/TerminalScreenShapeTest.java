package li.cil.oc.client;

import li.cil.oc.common.component.TerminalScreenSnapshot;
import li.cil.oc.common.menu.TerminalMenu;
import li.cil.oc.common.network.TerminalClipboardPayload;
import li.cil.oc.common.network.TerminalKeyPayload;
import li.cil.oc.common.network.TerminalMousePayload;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
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
    void terminalScreenBuildsKeyPayloadForMenu() throws ReflectiveOperationException {
        final TerminalMenu menu = allocateMenu(12);

        final TerminalKeyPayload payload = TerminalScreen.keyPayload(menu, true, 'x', 45);

        assertEquals(12, payload.containerId());
        assertEquals(true, payload.pressed());
        assertEquals((int) 'x', payload.character());
        assertEquals(45, payload.keyCode());
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

    private static TerminalMenu allocateMenu(final int containerId) throws ReflectiveOperationException {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        final TerminalMenu menu = (TerminalMenu) ((Unsafe) unsafeField.get(null)).allocateInstance(TerminalMenu.class);
        final Field containerIdField = net.minecraft.world.inventory.AbstractContainerMenu.class.getDeclaredField("containerId");
        containerIdField.setAccessible(true);
        containerIdField.setInt(menu, containerId);
        return menu;
    }
}
