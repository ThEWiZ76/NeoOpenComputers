package li.cil.oc.common.network;

import li.cil.oc.common.component.TerminalScreenSnapshot;
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

        assertEquals(true, TerminalNetworking.mouseInside(snapshot, new TerminalMousePayload(1, TerminalMousePayload.MOUSE_DOWN, 1, 1, 0)));
        assertEquals(true, TerminalNetworking.mouseInside(snapshot, new TerminalMousePayload(1, TerminalMousePayload.MOUSE_DOWN, 4, 2, 0)));
        assertEquals(false, TerminalNetworking.mouseInside(snapshot, new TerminalMousePayload(1, TerminalMousePayload.MOUSE_DOWN, 0, 1, 0)));
        assertEquals(false, TerminalNetworking.mouseInside(snapshot, new TerminalMousePayload(1, TerminalMousePayload.MOUSE_DOWN, 5, 1, 0)));
        assertEquals(false, TerminalNetworking.mouseInside(new TerminalScreenSnapshot(0, 0, new String[0]), new TerminalMousePayload(1, TerminalMousePayload.MOUSE_DOWN, 1, 1, 0)));
    }

    @Test
    void checksTerminalInputReadinessFromSnapshotBounds() {
        assertEquals(false, TerminalNetworking.acceptsTerminalInput(null));
        assertEquals(false, TerminalNetworking.acceptsTerminalInput(new TerminalScreenSnapshot(0, 0, new String[0])));
        assertEquals(false, TerminalNetworking.acceptsTerminalInput(new TerminalScreenSnapshot(4, 0, new String[0])));
        assertEquals(false, TerminalNetworking.acceptsTerminalInput(new TerminalScreenSnapshot(0, 2, new String[]{"", ""})));
        assertEquals(true, TerminalNetworking.acceptsTerminalInput(new TerminalScreenSnapshot(4, 2, new String[]{"", ""})));
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

    private static TestMenu allocateTestMenu(final int containerId) throws ReflectiveOperationException {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        final TestMenu menu = (TestMenu) ((Unsafe) unsafeField.get(null)).allocateInstance(TestMenu.class);
        final Field containerIdField = AbstractContainerMenu.class.getDeclaredField("containerId");
        containerIdField.setAccessible(true);
        containerIdField.setInt(menu, containerId);
        return menu;
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
