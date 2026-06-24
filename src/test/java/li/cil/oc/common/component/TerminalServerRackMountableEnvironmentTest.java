package li.cil.oc.common.component;

import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.common.ModSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TerminalServerRackMountableEnvironmentTest {
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
}
