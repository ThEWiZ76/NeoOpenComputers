package li.cil.oc.client;

import li.cil.oc.common.menu.RackMenu;
import li.cil.oc.common.network.RackControlPayload;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RackScreenShapeTest {
    @Test
    void rackScreenHasMenuConstructor() throws NoSuchMethodException {
        final Constructor<RackScreen> constructor = RackScreen.class.getConstructor(
            RackMenu.class,
            Inventory.class,
            Component.class);

        assertTrue(AbstractContainerScreen.class.isAssignableFrom(RackScreen.class));
        assertArrayEquals(new Class<?>[]{RackMenu.class, Inventory.class, Component.class}, constructor.getParameterTypes());
    }

    @Test
    void rackScreenBuildsControlPayloadForMenu() throws ReflectiveOperationException {
        final RackMenu menu = allocateMenu(14);

        final RackControlPayload payload = RackScreen.controlPayload(menu, 2, RackControlPayload.TOGGLE);

        assertEquals(14, payload.containerId());
        assertEquals(2, payload.slot());
        assertEquals(RackControlPayload.TOGGLE, payload.action());
    }

    @Test
    void rackScreenUsesRackStateForControlColor() {
        assertEquals(0xFF4C566A, RackScreen.controlColor(RackMenu.STATE_EMPTY));
        assertEquals(0xFFA3BE8C, RackScreen.controlColor(RackMenu.STATE_READY));
        assertEquals(0xFF88C0D0, RackScreen.controlColor(RackMenu.STATE_RUNNING));
    }

    private static RackMenu allocateMenu(final int containerId) throws ReflectiveOperationException {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        final RackMenu menu = (RackMenu) ((Unsafe) unsafeField.get(null)).allocateInstance(RackMenu.class);
        final Field containerIdField = net.minecraft.world.inventory.AbstractContainerMenu.class.getDeclaredField("containerId");
        containerIdField.setAccessible(true);
        containerIdField.setInt(menu, containerId);
        return menu;
    }
}
