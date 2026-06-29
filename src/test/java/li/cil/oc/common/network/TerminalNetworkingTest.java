package li.cil.oc.common.network;

import li.cil.oc.common.component.TerminalScreenDelta;
import li.cil.oc.common.component.TerminalScreenSnapshot;
import li.cil.oc.common.component.TerminalServerRackMountableEnvironment;
import li.cil.oc.common.blockentity.ScreenBlockEntity;
import li.cil.oc.common.menu.TerminalMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TerminalNetworkingTest {
    @Test
    void appliesSnapshotPayloadToMatchingTerminalMenu() throws ReflectiveOperationException {
        final TerminalMenu menu = allocateMenu(3, new TerminalScreenSnapshot(1, 1, new String[]{"old"}));
        final TerminalScreenSnapshotPayload payload = new TerminalScreenSnapshotPayload(
            3,
            new TerminalScreenSnapshot(4, 1, new String[]{"new"}));

        TerminalNetworking.applyScreenSnapshot(menu, payload);

        assertEquals(4, menu.snapshot().width());
        assertEquals("new", menu.snapshot().line(0));
    }

    @Test
    void appliesDeltaPayloadToMatchingTerminalMenu() throws ReflectiveOperationException {
        final TerminalScreenSnapshot previous = new TerminalScreenSnapshot(4, 2, new String[]{"old ", "same"});
        final TerminalScreenSnapshot current = new TerminalScreenSnapshot(4, 2, new String[]{"new ", "same"});
        final TerminalMenu menu = allocateMenu(3, previous);
        final TerminalScreenDeltaPayload payload = new TerminalScreenDeltaPayload(3, TerminalScreenDelta.between(previous, current));

        TerminalNetworking.applyScreenDelta(menu, payload);

        assertEquals(4, menu.snapshot().width());
        assertEquals("new ", menu.snapshot().line(0));
        assertEquals("same", menu.snapshot().line(1));
    }

    @Test
    void ignoresSnapshotPayloadForDifferentContainer() throws ReflectiveOperationException {
        final TerminalMenu menu = allocateMenu(3, new TerminalScreenSnapshot(1, 1, new String[]{"old"}));
        final TerminalScreenSnapshotPayload payload = new TerminalScreenSnapshotPayload(
            4,
            new TerminalScreenSnapshot(4, 1, new String[]{"new"}));

        TerminalNetworking.applyScreenSnapshot(menu, payload);

        assertEquals(1, menu.snapshot().width());
        assertEquals("old", menu.snapshot().line(0));
    }

    @Test
    void ignoresSnapshotPayloadForNonTerminalMenu() throws ReflectiveOperationException {
        final TerminalScreenSnapshotPayload payload = new TerminalScreenSnapshotPayload(
            5,
            new TerminalScreenSnapshot(4, 1, new String[]{"new"}));

        TerminalNetworking.applyScreenSnapshot(allocateTestMenu(5), payload);
    }

    @Test
    void checksMousePayloadAgainstSnapshotBounds() {
        final TerminalScreenSnapshot snapshot = new TerminalScreenSnapshot(4, 2, new String[]{"neo", "oc"});

        assertEquals(true, TerminalNetworking.mouseInside(snapshot, new TerminalMousePayload(1, TerminalMousePayload.MOUSE_DOWN, 0, 0, 0)));
        assertEquals(true, TerminalNetworking.mouseInside(snapshot, new TerminalMousePayload(1, TerminalMousePayload.MOUSE_DOWN, 3, 1, 0)));
        assertEquals(false, TerminalNetworking.mouseInside(snapshot, new TerminalMousePayload(1, TerminalMousePayload.MOUSE_DOWN, -1, 0, 0)));
        assertEquals(false, TerminalNetworking.mouseInside(snapshot, new TerminalMousePayload(1, TerminalMousePayload.MOUSE_DOWN, 4, 0, 0)));
        assertEquals(false, TerminalNetworking.mouseInside(new TerminalScreenSnapshot(0, 0, new String[0]), new TerminalMousePayload(1, TerminalMousePayload.MOUSE_DOWN, 0, 0, 0)));
    }

    @Test
    void acceptsOutsideMouseUpForReleaseParity() {
        final TerminalScreenSnapshot snapshot = new TerminalScreenSnapshot(4, 2, new String[]{"neo", "oc"});

        assertEquals(true, TerminalNetworking.acceptsTerminalMouse(snapshot, new TerminalMousePayload(1, TerminalMousePayload.MOUSE_UP, -1, -1, 0)));
        assertEquals(false, TerminalNetworking.acceptsTerminalMouse(snapshot, new TerminalMousePayload(1, TerminalMousePayload.MOUSE_DOWN, -1, -1, 0)));
        assertEquals(false, TerminalNetworking.acceptsTerminalMouse(new TerminalScreenSnapshot(0, 0, new String[0]), new TerminalMousePayload(1, TerminalMousePayload.MOUSE_UP, -1, -1, 0)));
    }

    @Test
    void rejectsPhysicalMouseInputForNonTouchScreenLikeUpstream() throws ReflectiveOperationException {
        final TerminalScreenSnapshot snapshot = new TerminalScreenSnapshot(4, 2, new String[]{"neo", "oc"});
        final TerminalMousePayload payload = new TerminalMousePayload(3, TerminalMousePayload.MOUSE_DOWN, 0, 0, 0);
        final TerminalMenu tierOneMenu = allocateMenu(3, snapshot);
        final TerminalMenu tierTwoMenu = allocateMenu(3, snapshot);
        setField(tierOneMenu, "physicalScreen", screenWithTier(0));
        setField(tierTwoMenu, "physicalScreen", screenWithTier(1));

        assertEquals(false, TerminalNetworking.acceptsTerminalMouse(tierOneMenu, snapshot, payload));
        assertEquals(true, TerminalNetworking.acceptsTerminalMouse(tierTwoMenu, snapshot, payload));
    }

    @Test
    void checksTerminalInputReadinessFromSnapshotBounds() {
        assertEquals(false, TerminalNetworking.acceptsTerminalInput(null));
        assertEquals(false, TerminalNetworking.acceptsTerminalInput(new TerminalScreenSnapshot(0, 0, new String[0])));
        assertEquals(false, TerminalNetworking.acceptsTerminalInput(new TerminalScreenSnapshot(4, 0, new String[0])));
        assertEquals(false, TerminalNetworking.acceptsTerminalInput(new TerminalScreenSnapshot(0, 2, new String[]{"", ""})));
        assertEquals(true, TerminalNetworking.acceptsTerminalInput(new TerminalScreenSnapshot(4, 2, new String[]{"", ""})));
    }

    @Test
    void rejectsStaleTerminalMenuForNetworkInput() throws ReflectiveOperationException {
        final TerminalMenu menu = allocateMenu(3, new TerminalScreenSnapshot(1, 1, new String[]{""}));
        final TerminalServerRackMountableEnvironment staleServer = allocateTerminalServer();
        final Field terminalServerField = TerminalMenu.class.getDeclaredField("terminalServer");
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        final Unsafe unsafe = (Unsafe) unsafeField.get(null);
        unsafe.putObject(menu, unsafe.objectFieldOffset(terminalServerField), staleServer);

        assertEquals(false, TerminalNetworking.acceptsTerminalMenu(menu, null));
    }

    private static TerminalMenu allocateMenu(final int containerId, final TerminalScreenSnapshot snapshot) throws ReflectiveOperationException {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        final TerminalMenu menu = (TerminalMenu) ((Unsafe) unsafeField.get(null)).allocateInstance(TerminalMenu.class);
        final Field containerIdField = AbstractContainerMenu.class.getDeclaredField("containerId");
        containerIdField.setAccessible(true);
        containerIdField.setInt(menu, containerId);
        menu.updateSnapshot(snapshot);
        return menu;
    }

    private static ScreenBlockEntity screenWithTier(final int tier) throws ReflectiveOperationException {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        final ScreenBlockEntity screen = (ScreenBlockEntity) ((Unsafe) unsafeField.get(null)).allocateInstance(ScreenBlockEntity.class);
        setField(screen, "tier", tier);
        return screen;
    }

    private static void setField(final Object target, final String name, final Object value) throws ReflectiveOperationException {
        final Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static TestMenu allocateTestMenu(final int containerId) throws ReflectiveOperationException {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        final TestMenu menu = (TestMenu) ((Unsafe) unsafeField.get(null)).allocateInstance(TestMenu.class);
        final Field containerIdField = AbstractContainerMenu.class.getDeclaredField("containerId");
        containerIdField.setAccessible(true);
        containerIdField.setInt(menu, containerId);
        return menu;
    }

    private static TerminalServerRackMountableEnvironment allocateTerminalServer() throws ReflectiveOperationException {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (TerminalServerRackMountableEnvironment) ((Unsafe) unsafeField.get(null)).allocateInstance(TerminalServerRackMountableEnvironment.class);
    }

    private static final class TestMenu extends AbstractContainerMenu {
        private TestMenu() {
            super(null, 0);
        }

        @Override
        public ItemStack quickMoveStack(final net.minecraft.world.entity.player.Player player, final int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(final net.minecraft.world.entity.player.Player player) {
            return true;
        }
    }
}
