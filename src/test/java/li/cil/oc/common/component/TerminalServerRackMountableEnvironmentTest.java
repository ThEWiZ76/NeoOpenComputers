package li.cil.oc.common.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TerminalServerRackMountableEnvironmentTest {
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
}
