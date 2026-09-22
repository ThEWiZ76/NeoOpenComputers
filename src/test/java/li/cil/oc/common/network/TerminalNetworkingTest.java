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
    void routesItemScreenInputThroughRealKeyboardAndRejectsInvalidMenuOrMouseTier() throws Exception {
        li.cil.oc.common.OpenComputersApi.initialize();
        final var screen = new li.cil.oc.common.blockentity.ScreenItemEnvironment(null, 1);
        final var keyboard = new li.cil.oc.common.component.KeyboardItemEnvironment();
        final InputSink sink = new InputSink();
        li.cil.oc.api.Network.joinNewNetwork(screen.node());
        screen.node().connect(keyboard.node());
        screen.node().connect(sink.node());
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        final ItemScreenMenu menu = (ItemScreenMenu) ((Unsafe) unsafeField.get(null)).allocateInstance(ItemScreenMenu.class);
        menu.screen = screen;
        menu.allowed = true;
        menu.mouse = true;
        TerminalNetworking.applyTerminalKey(menu, new TerminalKeyPayload(0, true, 'a', 30), null);
        TerminalNetworking.applyTerminalKey(menu, new TerminalKeyPayload(0, false, 'a', 30), null);
        TerminalNetworking.applyTerminalClipboard(menu, new TerminalClipboardPayload(0, "paste"), null);
        TerminalNetworking.applyTerminalMouse(menu, new TerminalMousePayload(0, TerminalMousePayload.MOUSE_DOWN, 1, 2, 0), null);
        assertEquals(java.util.List.of("key_down", "key_up", "clipboard", "touch"), sink.signals);
        menu.allowed = false;
        TerminalNetworking.applyTerminalKey(menu, new TerminalKeyPayload(0, true, 'b', 48), null);
        TerminalNetworking.applyTerminalClipboard(menu, new TerminalClipboardPayload(0, "blocked"), null);
        menu.allowed = true;
        TerminalNetworking.applyTerminalKey(menu, new TerminalKeyPayload(1, true, 'b', 48), null);
        menu.mouse = false;
        TerminalNetworking.applyTerminalMouse(menu, new TerminalMousePayload(0, TerminalMousePayload.MOUSE_DOWN, 1, 2, 0), null);
        menu.mouse = true;
        TerminalNetworking.applyTerminalMouse(menu, new TerminalMousePayload(0, TerminalMousePayload.MOUSE_DOWN, 999, 2, 0), null);
        assertEquals(4, sink.signals.size());
        screen.node().remove();
        keyboard.node().remove();
        sink.node().remove();
    }

    private static final class ItemScreenMenu extends TerminalMenu {
        private li.cil.oc.common.blockentity.ScreenItemEnvironment screen;
        private boolean allowed;
        private boolean mouse;

        private ItemScreenMenu() { super(null, 0, null); }
        @Override public li.cil.oc.common.blockentity.ScreenItemEnvironment itemScreen() { return screen; }
        @Override public boolean acceptsInput(final net.minecraft.world.entity.player.Player player) { return allowed; }
        @Override public boolean supportsMouseInput() { return mouse; }
    }

    private static final class InputSink extends li.cil.oc.api.prefab.AbstractManagedEnvironment {
        private final java.util.List<String> signals = new java.util.ArrayList<>();
        private InputSink() {
            setNode(li.cil.oc.api.Network.newNode(this, li.cil.oc.api.network.Visibility.Network).create());
        }
        @Override public void onMessage(final li.cil.oc.api.network.Message message) {
            if (message.name().equals("computer.checked_signal")) {
                signals.add(String.valueOf(message.data()[1]));
            }
        }
    }

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
    void appliesMouseInputSupportPayloadToMatchingTerminalMenu() throws ReflectiveOperationException {
        final TerminalMenu menu = allocateMenu(3, new TerminalScreenSnapshot(4, 2, new String[]{"neo", "oc"}));

        TerminalNetworking.applyMouseInputSupport(menu, new TerminalMouseInputSupportPayload(3, false));

        assertEquals(false, menu.supportsMouseInput());
    }

    @Test
    void ignoresMouseInputSupportPayloadForDifferentContainer() throws ReflectiveOperationException {
        final TerminalMenu menu = allocateMenu(3, new TerminalScreenSnapshot(4, 2, new String[]{"neo", "oc"}));

        TerminalNetworking.applyMouseInputSupport(menu, new TerminalMouseInputSupportPayload(4, false));

        assertEquals(true, menu.supportsMouseInput());
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
