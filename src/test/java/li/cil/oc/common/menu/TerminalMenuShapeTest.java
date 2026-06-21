package li.cil.oc.common.menu;

import li.cil.oc.common.component.TerminalScreenSnapshot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TerminalMenuShapeTest {
    @Test
    void terminalMenuHasClientConstructor() throws NoSuchMethodException {
        final Constructor<TerminalMenu> clientConstructor = TerminalMenu.class.getConstructor(int.class, Inventory.class);
        final Constructor<TerminalMenu> serverConstructor = TerminalMenu.class.getConstructor(int.class, Inventory.class, TerminalScreenSnapshot.class);

        assertTrue(AbstractContainerMenu.class.isAssignableFrom(TerminalMenu.class));
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class}, clientConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, TerminalScreenSnapshot.class}, serverConstructor.getParameterTypes());
    }

    @Test
    void terminalMenuSlotCountsAreStable() {
        assertEquals(0, TerminalMenu.TERMINAL_SLOT_COUNT);
        assertEquals(0, TerminalMenu.TOTAL_SLOT_COUNT);
    }

    @Test
    void terminalMenuExposesScreenSnapshot() throws NoSuchMethodException {
        final Method method = TerminalMenu.class.getMethod("snapshot");

        assertEquals(TerminalScreenSnapshot.class, method.getReturnType());
    }
}
