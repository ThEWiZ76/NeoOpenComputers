package li.cil.oc.client;

import li.cil.oc.api.API;
import li.cil.oc.api.manual.ImageRenderer;
import li.cil.oc.api.manual.InteractiveImageRenderer;
import li.cil.oc.common.ManualRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ManualDocumentTest {
    @AfterEach
    void tearDown() {
        API.manual = null;
    }

    @Test
    void parsesMarkdownImagesIntoRenderSegmentsLikeUpstream() {
        TestImageRenderer renderer = new TestImageRenderer();

        ManualDocument document = ManualDocument.parse(List.of("before ![tip](image:ok) after"), href -> renderer);

        assertEquals(3, document.segments().size());
        assertText("before ", document.segments().get(0));
        ManualDocument.ImageSegment image = assertInstanceOf(ManualDocument.ImageSegment.class, document.segments().get(1));
        assertEquals("tip", image.title());
        assertEquals("image:ok", image.href());
        assertSame(renderer, image.renderer());
        assertText(" after", document.segments().get(2));
    }

    @Test
    void parsesMarkdownLinksIntoInteractiveSegmentsLikeUpstream() {
        ManualDocument document = ManualDocument.parse(List.of("Read [the manual](item/manual.md) now"), href -> null);

        assertEquals(3, document.segments().size());
        assertText("Read ", document.segments().get(0));
        ManualDocument.LinkSegment link = assertInstanceOf(ManualDocument.LinkSegment.class, document.segments().get(1));
        assertEquals("the manual", link.text());
        assertEquals("item/manual.md", link.href());
        assertText(" now", document.segments().get(2));
    }

    @Test
    void preservesInlineFormattingInsideLinksLikeUpstream() {
        ManualDocument document = ManualDocument.parse(List.of("Read [**manual**](item/manual.md)"), href -> null);

        ManualDocument.LinkSegment link = assertInstanceOf(ManualDocument.LinkSegment.class, document.segments().get(1));
        assertEquals("manual", link.text());
        assertEquals("item/manual.md", link.href());
        assertTrue(link.bold());
    }

    @Test
    void parsesMarkdownFormattingSegmentsLikeUpstream() {
        ManualDocument document = ManualDocument.parse(List.of(
            "# Heading",
            "**Bold** *Italic* `code` ~~Gone~~"),
            href -> null);

        ManualDocument.HeaderSegment header = assertInstanceOf(ManualDocument.HeaderSegment.class, document.segments().get(0));
        assertEquals("Heading", header.text());
        assertEquals(1, header.level());

        ManualDocument.BoldSegment bold = assertInstanceOf(ManualDocument.BoldSegment.class, document.segments().get(2));
        assertEquals("Bold", bold.text());
        ManualDocument.ItalicSegment italic = assertInstanceOf(ManualDocument.ItalicSegment.class, document.segments().get(4));
        assertEquals("Italic", italic.text());
        ManualDocument.CodeSegment code = assertInstanceOf(ManualDocument.CodeSegment.class, document.segments().get(6));
        assertEquals("code", code.text());
        ManualDocument.StrikethroughSegment strike = assertInstanceOf(ManualDocument.StrikethroughSegment.class, document.segments().get(8));
        assertEquals("Gone", strike.text());
    }

    @Test
    void missingImageRendererBecomesDiagnosticTextLikeUpstream() {
        ManualDocument document = ManualDocument.parse(List.of("![missing](image:missing)"), href -> null);

        assertEquals(1, document.segments().size());
        assertText("No renderer found for: image:missing", document.segments().get(0));
    }

    @Test
    void imageSegmentUsesInteractiveTooltipAndLocalClickCoordinates() {
        TestInteractiveImageRenderer renderer = new TestInteractiveImageRenderer();

        ManualDocument.ImageSegment image = assertInstanceOf(
            ManualDocument.ImageSegment.class,
            ManualDocument.parse(List.of("![fallback](image:ok)"), href -> renderer).segments().get(0));

        image.renderAt(12, 20, 15, 24);

        assertEquals("interactive:fallback", image.tooltip());
        assertTrue(image.onMouseClick(17, 29));
        assertEquals(List.of(5, 9), renderer.clicked);
        assertEquals(List.of(3, 4), renderer.renderMouse);
    }

    @Test
    void nonInteractiveImageUsesMarkdownTitleAsTooltip() {
        TestImageRenderer renderer = new TestImageRenderer();

        ManualDocument.ImageSegment image = assertInstanceOf(
            ManualDocument.ImageSegment.class,
            ManualDocument.parse(List.of("![plain](image:ok)"), href -> renderer).segments().get(0));

        assertEquals("plain", image.tooltip());
    }

    @Test
    void defaultParserUsesManualApiImageLookup() {
        TestImageRenderer renderer = new TestImageRenderer();
        ManualRegistry registry = new ManualRegistry();
        registry.addProvider("image", data -> "ok".equals(data) ? renderer : null);
        API.manual = registry;

        ManualDocument.ImageSegment image = assertInstanceOf(
            ManualDocument.ImageSegment.class,
            ManualDocument.parse(List.of("![tip](image:ok)")).segments().get(0));

        assertSame(renderer, image.renderer());
    }

    private static void assertText(final String expected, final ManualDocument.Segment segment) {
        ManualDocument.TextSegment text = assertInstanceOf(ManualDocument.TextSegment.class, segment);
        assertEquals(expected, text.text());
    }

    private static class TestImageRenderer implements ImageRenderer {
        protected final List<Integer> renderMouse = new ArrayList<>();

        @Override
        public int getWidth() {
            return 16;
        }

        @Override
        public int getHeight() {
            return 16;
        }

        @Override
        public void render(final int mouseX, final int mouseY) {
            renderMouse.clear();
            renderMouse.add(mouseX);
            renderMouse.add(mouseY);
        }
    }

    private static final class TestInteractiveImageRenderer extends TestImageRenderer implements InteractiveImageRenderer {
        private final List<Integer> clicked = new ArrayList<>();

        @Override
        public String getTooltip(final String tooltip) {
            return "interactive:" + tooltip;
        }

        @Override
        public boolean onMouseClick(final int mouseX, final int mouseY) {
            clicked.clear();
            clicked.add(mouseX);
            clicked.add(mouseY);
            return true;
        }
    }
}
