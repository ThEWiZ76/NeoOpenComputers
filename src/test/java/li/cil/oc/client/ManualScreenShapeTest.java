package li.cil.oc.client;

import li.cil.oc.common.ManualRegistry;
import li.cil.oc.api.manual.ImageRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ManualScreenShapeTest {
    @Test
    void manualScreenHasRegistryConstructor() throws NoSuchMethodException {
        final Constructor<ManualScreen> constructor = ManualScreen.class.getConstructor(ManualRegistry.class);

        assertTrue(Screen.class.isAssignableFrom(ManualScreen.class));
        assertArrayEquals(new Class<?>[]{ManualRegistry.class}, constructor.getParameterTypes());
    }

    @Test
    void manualScreenExposesUpstreamLayoutConstants() {
        assertEquals(256, ManualScreen.WINDOW_WIDTH);
        assertEquals(192, ManualScreen.WINDOW_HEIGHT);
        assertEquals(230, ManualScreen.DOCUMENT_MAX_WIDTH);
        assertEquals(176, ManualScreen.DOCUMENT_MAX_HEIGHT);
        assertEquals(7, ManualScreen.MAX_TABS_PER_SIDE);
    }

    @Test
    void manualScreenCanRefreshCurrentManualPage() throws NoSuchMethodException {
        final Method refreshPage = ManualScreen.class.getMethod("refreshPage");
        final Method document = ManualScreen.class.getMethod("document");

        assertEquals(void.class, refreshPage.getReturnType());
        assertEquals(ManualDocument.class, document.getReturnType());
    }

    @Test
    void manualScreenUsesManualTitle() {
        assertEquals("gui.neoopencomputers.manual", ManualScreen.title().getString());
        assertTrue(ManualScreen.title() instanceof Component);
    }

    @Test
    void manualScreenLayoutsTextAndImagesForRendering() {
        ManualDocument document = ManualDocument.parse(
            List.of("alpha ![tip](image:ok)"),
            href -> new TestImageRenderer(50, 20));

        List<ManualScreen.LayoutEntry> entries = ManualScreen.layout(document, 230);

        assertEquals(2, entries.size());
        assertTrue(entries.get(0).segment() instanceof ManualDocument.TextSegment);
        assertEquals(0, entries.get(0).x());
        assertEquals(0, entries.get(0).y());
        assertEquals(ManualScreen.LINE_HEIGHT, entries.get(0).height());
        assertTrue(entries.get(1).segment() instanceof ManualDocument.ImageSegment);
        assertEquals(90, entries.get(1).x());
        assertEquals(ManualScreen.LINE_HEIGHT + ManualScreen.SEGMENT_PADDING, entries.get(1).y());
        assertEquals(50, entries.get(1).width());
        assertEquals(20, entries.get(1).height());
    }

    private record TestImageRenderer(int getWidth, int getHeight) implements ImageRenderer {
        @Override
        public void render(final int mouseX, final int mouseY) {
        }
    }
}
