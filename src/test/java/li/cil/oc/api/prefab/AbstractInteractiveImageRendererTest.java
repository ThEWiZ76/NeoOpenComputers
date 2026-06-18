package li.cil.oc.api.prefab;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class AbstractInteractiveImageRendererTest {
    @Test
    void defaultInteractiveRendererKeepsTooltipAndDoesNotHandleClicks() {
        var renderer = new TestRenderer();

        assertEquals("info", renderer.getTooltip("info"));
        assertFalse(renderer.onMouseClick(1, 2));
    }

    private static final class TestRenderer extends AbstractInteractiveImageRenderer {
        @Override
        public int getWidth() {
            return 1;
        }

        @Override
        public int getHeight() {
            return 1;
        }

        @Override
        public void render(final int mouseX, final int mouseY) {
        }
    }
}
