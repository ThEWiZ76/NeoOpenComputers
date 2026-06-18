package li.cil.oc.api.manual;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

final class ManualApiTest {
    @Test
    void contentProviderReturnsManualLinesForPath() {
        ContentProvider provider = path -> List.of("# " + path);

        assertEquals(List.of("# index"), provider.getContent("index"));
    }

    @Test
    void imageProviderReturnsRendererForData() {
        ImageRenderer renderer = new TestImageRenderer();
        ImageProvider provider = data -> renderer;

        assertSame(renderer, provider.getImage("screen"));
    }

    @Test
    void interactiveRendererExtendsImageRenderer() {
        InteractiveImageRenderer renderer = new TestInteractiveImageRenderer();

        assertEquals(8, renderer.getWidth());
        assertEquals(6, renderer.getHeight());
        assertEquals("tooltip", renderer.getTooltip("tooltip"));
        assertFalse(renderer.onMouseClick(1, 2));
    }

    private static class TestImageRenderer implements ImageRenderer {
        @Override
        public int getWidth() {
            return 8;
        }

        @Override
        public int getHeight() {
            return 6;
        }

        @Override
        public void render(final int mouseX, final int mouseY) {
        }
    }

    private static final class TestInteractiveImageRenderer extends TestImageRenderer implements InteractiveImageRenderer {
        @Override
        public String getTooltip(final String tooltip) {
            return tooltip;
        }

        @Override
        public boolean onMouseClick(final int mouseX, final int mouseY) {
            return false;
        }
    }
}
