package li.cil.oc.common.menu;

import li.cil.oc.common.component.TerminalScreenSnapshot;
import li.cil.oc.common.component.TerminalServerRackMountableEnvironment;
import li.cil.oc.common.blockentity.ScreenBlockEntity;
import li.cil.oc.common.network.TerminalScreenSnapshotPayload;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

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
        final Constructor<TerminalMenu> physicalScreenConstructor = TerminalMenu.class.getConstructor(
            int.class,
            Inventory.class,
            TerminalScreenSnapshot.class,
            ScreenBlockEntity.class);

        assertTrue(AbstractContainerMenu.class.isAssignableFrom(TerminalMenu.class));
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class}, clientConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, TerminalScreenSnapshot.class}, serverConstructor.getParameterTypes());
        assertArrayEquals(
            new Class<?>[]{int.class, Inventory.class, TerminalScreenSnapshot.class, TerminalServerRackMountableEnvironment.class},
            terminalServerConstructor.getParameterTypes());
        assertArrayEquals(
            new Class<?>[]{int.class, Inventory.class, TerminalScreenSnapshot.class, TerminalServerRackMountableEnvironment.class, String.class},
            keyedTerminalServerConstructor.getParameterTypes());
        assertArrayEquals(
            new Class<?>[]{int.class, Inventory.class, TerminalScreenSnapshot.class, ScreenBlockEntity.class},
            physicalScreenConstructor.getParameterTypes());
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
    void terminalMenuExposesPhysicalScreenTarget() throws NoSuchMethodException {
        final Method method = TerminalMenu.class.getMethod("physicalScreen");

        assertEquals(ScreenBlockEntity.class, method.getReturnType());
    }

    @Test
    void terminalMenuUpdatesMouseCapabilityFromClientSyncPayload() throws ReflectiveOperationException {
        final TerminalMenu menu = allocateMenu();

        assertEquals(true, menu.supportsMouseInput());
        menu.updateMouseInputSupport(false);
        assertEquals(false, menu.supportsMouseInput());
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

    @Test
    void terminalMenuDoesNotStreamScreenChangesAfterTerminalKeyInvalidates() throws ReflectiveOperationException {
        final TerminalServerRackMountableEnvironment terminalServer = new TerminalServerRackMountableEnvironment();
        terminalKeys(terminalServer).add("old");
        terminalServer.screen().setResolution(2, 1);
        terminalServer.screen().setViewport(2, 1);
        terminalServer.screen().set(0, 0, "AB", false);
        final TerminalMenu menu = allocateMenu();
        setField(menu, "snapshot", new TerminalScreenSnapshot(2, 1, new String[]{"CD"}));
        setField(menu, "terminalServer", terminalServer);
        setField(menu, "terminalKey", "old");

        terminalKeys(terminalServer).clear();

        assertEquals(null, menu.changedScreenPayload());
    }

    private static TerminalMenu allocateMenu() throws ReflectiveOperationException {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (TerminalMenu) ((Unsafe) unsafeField.get(null)).allocateInstance(TerminalMenu.class);
    }

    @SuppressWarnings("unchecked")
    private static List<String> terminalKeys(final TerminalServerRackMountableEnvironment terminalServer) throws ReflectiveOperationException {
        final Field keys = TerminalServerRackMountableEnvironment.class.getDeclaredField("keys");
        keys.setAccessible(true);
        return (List<String>) keys.get(terminalServer);
    }

    private static void setField(final Object instance, final String name, final Object value) throws ReflectiveOperationException {
        final Field field = TerminalMenu.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(instance, value);
    }
}
