package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.menu.TerminalMenu;
import org.junit.jupiter.api.Test;
import net.minecraft.world.level.Level;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TerminalServerRackMountableEnvironmentTest {
    @Test
    void terminalServerRackNodeIsHiddenLikeUpstream() {
        final TerminalServerRackMountableEnvironment terminal = new TerminalServerRackMountableEnvironment();

        assertEquals(Visibility.None, terminal.node().reachability());
    }

    @Test
    void terminalServerReportsNoStateLikeUpstream() {
        final TerminalServerRackMountableEnvironment terminal = new TerminalServerRackMountableEnvironment();

        assertTrue(terminal.getCurrentState().isEmpty());
    }

    @Test
    void registryPromotesAddresslessTerminalServersLikeUpstreamCache() throws Exception {
        TerminalServerRegistry.clear();
        final TerminalServerRackMountableEnvironment terminal = new TerminalServerRackMountableEnvironment();
        terminal.removeVirtualNodes();
        terminal.node().remove();
        clearNodeAddress(terminal.node());

        TerminalServerRegistry.add(terminal);
        Network.joinNewNetwork(terminal.node());

        assertSame(terminal, TerminalServerRegistry.find(terminal.node().address()));
    }

    @Test
    void terminalServerRejectsPlayersOutsideUpstreamWirelessRange() {
        final TerminalServerRackMountableEnvironment terminal = new TerminalServerRackMountableEnvironment(new TestHost(10.5D, 64.5D, -2.5D), 1);
        final double range = ModSettings.maxWirelessRange(1);

        assertTrue(terminal.isUsableFrom(null, 10.5D + range - 1D, 64.5D, -2.5D));
        assertEquals(false, terminal.isUsableFrom(null, 10.5D + range, 64.5D, -2.5D));
    }

    @Test
    void terminalServerVirtualScreenUsesTierThreeCapacityLikeUpstream() {
        final TerminalServerRackMountableEnvironment terminal = new TerminalServerRackMountableEnvironment();
        final TextBuffer screen = terminal.screen();

        assertEquals(ModSettings.screenWidthByTier(2), screen.getMaximumWidth());
        assertEquals(ModSettings.screenHeightByTier(2), screen.getMaximumHeight());
        assertEquals(ModSettings.screenDepthByTier(2), screen.getMaximumColorDepth());
    }

    @Test
    void screenSnapshotPreservesTrailingSpacesLikeTextBuffer() {
        final TerminalServerRackMountableEnvironment terminal = new TerminalServerRackMountableEnvironment();
        terminal.screen().setResolution(4, 1);
        terminal.screen().setViewport(4, 1);
        terminal.screen().set(0, 0, "A   ", false);

        final TerminalScreenSnapshot snapshot = terminal.screenSnapshot();

        assertEquals(4, snapshot.width());
        assertEquals(1, snapshot.height());
        assertEquals("A   ", snapshot.line(0));
    }

    @Test
    void screenSnapshotIncludesForegroundAndBackgroundColors() {
        final TerminalServerRackMountableEnvironment terminal = new TerminalServerRackMountableEnvironment();
        terminal.screen().setResolution(2, 1);
        terminal.screen().setViewport(2, 1);
        terminal.screen().setForegroundColor(0x112233);
        terminal.screen().setBackgroundColor(0x445566);
        terminal.screen().set(0, 0, "AB", false);

        final TerminalScreenSnapshot snapshot = terminal.screenSnapshot();

        assertEquals(0x112233, snapshot.foregroundColor(0, 0));
        assertEquals(0x112233, snapshot.foregroundColor(1, 0));
        assertEquals(0x445566, snapshot.backgroundColor(0, 0));
        assertEquals(0x445566, snapshot.backgroundColor(1, 0));
    }

    @Test
    void virtualScreenAndKeyboardAreDirectlyConnectedLikeUpstream() {
        final TerminalServerRackMountableEnvironment terminal = new TerminalServerRackMountableEnvironment();

        final Node[] virtualNodes = terminal.onAnalyze(null, null, 0, 0, 0);

        assertEquals(2, virtualNodes.length);
        assertTrue(virtualNodes[0].isNeighborOf(terminal.node()));
        assertTrue(virtualNodes[1].isNeighborOf(terminal.node()));
        assertTrue(virtualNodes[0].isNeighborOf(virtualNodes[1]));
    }

    @Test
    void terminalMenuInvalidatesWhenTerminalKeyChangesLikeUpstream() throws ReflectiveOperationException {
        TerminalServerRegistry.clear();
        final TerminalServerRackMountableEnvironment terminalServer = new TerminalServerRackMountableEnvironment();
        terminalKeys(terminalServer).add("old");
        final TerminalMenu menu = allocateTerminalMenu(terminalServer, "old");
        assertTrue(menu.stillValid(null));

        terminalKeys(terminalServer).clear();

        assertFalse(menu.stillValid(null));
    }

    @SuppressWarnings("unchecked")
    private static List<String> terminalKeys(final TerminalServerRackMountableEnvironment terminalServer) throws ReflectiveOperationException {
        final Field keys = TerminalServerRackMountableEnvironment.class.getDeclaredField("keys");
        keys.setAccessible(true);
        return (List<String>) keys.get(terminalServer);
    }

    private static TerminalMenu allocateTerminalMenu(final TerminalServerRackMountableEnvironment terminalServer, final String terminalKey) throws ReflectiveOperationException {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        final TerminalMenu menu = (TerminalMenu) ((Unsafe) unsafeField.get(null)).allocateInstance(TerminalMenu.class);
        final Field terminalServerField = field(TerminalMenu.class, "terminalServer");
        terminalServerField.setAccessible(true);
        terminalServerField.set(menu, terminalServer);
        final Field terminalKeyField = field(TerminalMenu.class, "terminalKey");
        terminalKeyField.setAccessible(true);
        terminalKeyField.set(menu, terminalKey);
        return menu;
    }

    private static void clearNodeAddress(final Node node) throws ReflectiveOperationException {
        final Field address = field(node.getClass(), "address");
        address.setAccessible(true);
        address.set(node, null);
    }

    private static Field field(final Class<?> type, final String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (final NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static final class TestHost implements EnvironmentHost {
        private final double xPosition;
        private final double yPosition;
        private final double zPosition;
        private int changed;

        private TestHost(final double xPosition, final double yPosition, final double zPosition) {
            this.xPosition = xPosition;
            this.yPosition = yPosition;
            this.zPosition = zPosition;
        }

        @Override
        public double xPosition() {
            return xPosition;
        }

        @Override
        public double yPosition() {
            return yPosition;
        }

        @Override
        public double zPosition() {
            return zPosition;
        }

        @Override
        public Level world() {
            return null;
        }

        @Override
        public void markChanged() {
            changed++;
        }
    }
}
