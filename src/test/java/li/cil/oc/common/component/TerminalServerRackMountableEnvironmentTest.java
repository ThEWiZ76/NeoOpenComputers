package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ModSettings;
import org.junit.jupiter.api.Test;
import net.minecraft.world.level.Level;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TerminalServerRackMountableEnvironmentTest {
    @Test
    void terminalServerRackNodeIsHiddenLikeUpstream() {
        final TerminalServerRackMountableEnvironment terminal = new TerminalServerRackMountableEnvironment();

        assertEquals(Visibility.None, terminal.node().reachability());
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

    private record TestHost(double xPosition, double yPosition, double zPosition) implements EnvironmentHost {
        @Override
        public Level world() {
            return null;
        }

        @Override
        public void markChanged() {
        }
    }
}
