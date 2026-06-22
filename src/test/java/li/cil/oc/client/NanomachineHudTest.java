package li.cil.oc.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class NanomachineHudTest {
    @Test
    void layoutUsesUpstreamDefaultAnchors() {
        final NanomachineHud.Layout layout = NanomachineHud.layout(320, 240, List.of(-1D, -1D), 0.5D);

        assertEquals(57, layout.left());
        assertEquals(201, layout.top());
        assertEquals(8, layout.width());
        assertEquals(12, layout.height());
        assertEquals(207, layout.fillTop());
    }

    @Test
    void layoutSupportsRelativeAndAbsolutePositions() {
        final NanomachineHud.Layout relative = NanomachineHud.layout(320, 240, List.of(0.5D, 0.25D), 1.25D);
        final NanomachineHud.Layout absolute = NanomachineHud.layout(320, 240, List.of(999D, 16D), -1D);

        assertEquals(160, relative.left());
        assertEquals(60, relative.top());
        assertEquals(60, relative.fillTop());

        assertEquals(312, absolute.left());
        assertEquals(16, absolute.top());
        assertEquals(28, absolute.fillTop());
    }
}
