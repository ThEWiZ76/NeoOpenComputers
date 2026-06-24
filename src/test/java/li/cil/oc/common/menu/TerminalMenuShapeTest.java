package li.cil.oc.common.menu;

import li.cil.oc.common.component.TerminalScreenSnapshot;
import li.cil.oc.common.component.TerminalServerRackMountableEnvironment;
import li.cil.oc.common.network.TerminalScreenSnapshotPayload;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
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
        final Constructor<TerminalMenu> terminalServerConstructor = TerminalMenu.class.getConstructor(
            int.class,
            Inventory.class,
            TerminalScreenSnapshot.class,
            TerminalServerRackMountableEnvironment.class);
        final Constructor<TerminalMenu> keyedTerminalServerConstructor = TerminalMenu.class.getConstructor(
            int.class,
            Inventory.class,
            TerminalScreenSnapshot.class,
            TerminalServerRackMountableEnvironment.class,
            String.class);

        assertTrue(AbstractContainerMenu.class.isAssignableFrom(TerminalMenu.class));
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class}, clientConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, TerminalScreenSnapshot.class}, serverConstructor.getParameterTypes());
        assertArrayEquals(
            new Class<?>[]{int.class, Inventory.class, TerminalScreenSnapshot.class, TerminalServerRackMountableEnvironment.class},
            terminalServerConstructor.getParameterTypes());
        assertArrayEquals(
            new Class<?>[]{int.class, Inventory.class, TerminalScreenSnapshot.class, TerminalServerRackMountableEnvironment.class, String.class},
            keyedTerminalServerConstructor.getParameterTypes());
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

    @Test
    void terminalMenuCanUpdateScreenSnapshot() throws NoSuchMethodException {
        final Method method = TerminalMenu.class.getMethod("updateSnapshot", TerminalScreenSnapshot.class);

        assertEquals(void.class, method.getReturnType());
    }

    @Test
    void terminalMenuCanCreateChangedSnapshotPayload() throws NoSuchMethodException {
        final Method method = TerminalMenu.class.getDeclaredMethod("changedSnapshotPayload");

        assertEquals(TerminalScreenSnapshotPayload.class, method.getReturnType());
    }

    @Test
    void terminalMenuExposesTerminalServerTarget() throws NoSuchMethodException {
        final Method method = TerminalMenu.class.getMethod("terminalServer");

        assertEquals(TerminalServerRackMountableEnvironment.class, method.getReturnType());
    }

    @Test
    void terminalMenuStoresUpdatedScreenSnapshot() throws ReflectiveOperationException {
        final TerminalMenu menu = allocateMenu();

        menu.updateSnapshot(new TerminalScreenSnapshot(3, 1, new String[]{"new"}));

        assertEquals(3, menu.snapshot().width());
        assertEquals("new", menu.snapshot().line(0));
    }

    @Test
    void terminalMenuDoesNotCreateChangedSnapshotPayloadWithoutServerTarget() throws ReflectiveOperationException {
        final TerminalMenu menu = allocateMenu();

        assertEquals(null, menu.changedSnapshotPayload());
    }

    private static TerminalMenu allocateMenu() throws ReflectiveOperationException {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (TerminalMenu) ((Unsafe) unsafeField.get(null)).allocateInstance(TerminalMenu.class);
    }
}
