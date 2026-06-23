package li.cil.oc.client;

import li.cil.oc.common.ManualRegistry;
import li.cil.oc.api.manual.ImageRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
        assertEquals(-23, ManualScreen.TAB_POS_X);
        assertEquals(7, ManualScreen.TAB_POS_Y);
        assertEquals(23, ManualScreen.TAB_WIDTH);
        assertEquals(26, ManualScreen.TAB_HEIGHT);
        assertEquals(244, ManualScreen.SCROLL_POS_X);
        assertEquals(6, ManualScreen.SCROLL_POS_Y);
        assertEquals(6, ManualScreen.SCROLL_WIDTH);
        assertEquals(180, ManualScreen.SCROLL_HEIGHT);
        assertEquals(13, ManualScreen.SCROLL_THUMB_HEIGHT);
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
    void manualScreenUsesBundledUpstreamGuiTextures() {
        assertEquals(ResourceLocation.fromNamespaceAndPath("neoopencomputers", "textures/gui/manual.png"), ManualScreen.MANUAL_TEXTURE);
        assertEquals(ResourceLocation.fromNamespaceAndPath("neoopencomputers", "textures/gui/manual_tab.png"), ManualScreen.TAB_TEXTURE);
        assertEquals(ResourceLocation.fromNamespaceAndPath("neoopencomputers", "textures/gui/button_scroll.png"), ManualScreen.SCROLL_TEXTURE);
        assertEquals(23, ManualScreen.TAB_TEXTURE_WIDTH);
        assertEquals(52, ManualScreen.TAB_TEXTURE_HEIGHT);
        assertEquals(6, ManualScreen.SCROLL_TEXTURE_WIDTH);
        assertEquals(26, ManualScreen.SCROLL_TEXTURE_HEIGHT);
    }

    @Test
    void manualScreenSelectsUpstreamButtonTextureRowsForHoverState() {
        assertEquals(0, ManualScreen.buttonTextureYOffset(false, ManualScreen.TAB_HEIGHT));
        assertEquals(ManualScreen.TAB_HEIGHT, ManualScreen.buttonTextureYOffset(true, ManualScreen.TAB_HEIGHT));
        assertEquals(ManualScreen.TAB_HEIGHT, ManualScreen.tabTextureYOffset(0, ManualScreen.TAB_POS_X, ManualScreen.TAB_POS_Y));
        assertEquals(0, ManualScreen.tabTextureYOffset(0, ManualScreen.TAB_POS_X + ManualScreen.TAB_WIDTH, ManualScreen.TAB_POS_Y));
        assertEquals(ManualScreen.TAB_HEIGHT, ManualScreen.tabTextureYOffset(1, ManualScreen.TAB_POS_X + 1, ManualScreen.TAB_POS_Y + ManualScreen.TAB_HEIGHT - 1));
    }

    @Test
    void manualScreenSelectsUpstreamScrollTextureRowForHoverAndDragState() {
        assertEquals(
            ManualScreen.SCROLL_THUMB_HEIGHT,
            ManualScreen.scrollTextureYOffset(false, ManualScreen.SCROLL_POS_X, 89, 90, 356, 176));
        assertEquals(
            0,
            ManualScreen.scrollTextureYOffset(false, ManualScreen.SCROLL_POS_X, 88, 90, 356, 176));
        assertEquals(
            ManualScreen.SCROLL_THUMB_HEIGHT,
            ManualScreen.scrollTextureYOffset(true, 0, 0, 90, 356, 176));
    }

    @Test
    void manualScreenComputesDocumentClipRectangleForScissorRendering() {
        ManualScreen.ClipRect clip = ManualScreen.documentClipRect(100, 40);

        assertEquals(108, clip.left());
        assertEquals(48, clip.top());
        assertEquals(338, clip.right());
        assertEquals(224, clip.bottom());
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

    @Test
    void manualScreenLaysOutInlineLinksWithoutForcingNewRows() {
        ManualDocument document = ManualDocument.parse(List.of("Read [manual](item/manual.md) now"), href -> null);

        List<ManualScreen.LayoutEntry> entries = ManualScreen.layout(document, 200, text -> text.length() * 5);

        assertEquals(3, entries.size());
        assertEquals(0, entries.get(0).x());
        assertEquals(0, entries.get(0).y());
        assertEquals(25, entries.get(1).x());
        assertEquals(0, entries.get(1).y());
        assertTrue(entries.get(1).segment() instanceof ManualDocument.LinkSegment);
        assertEquals(55, entries.get(2).x());
        assertEquals(0, entries.get(2).y());
    }

    @Test
    void manualScreenWrapsTextUsingMeasuredWidth() {
        ManualDocument document = ManualDocument.parse(List.of("alpha beta"), href -> null);

        List<ManualScreen.LayoutEntry> entries = ManualScreen.layout(document, 30, text -> text.length() * 6);

        assertEquals(2, entries.size());
        assertTextEntry("alpha", entries.get(0));
        assertEquals(0, entries.get(0).y());
        assertTextEntry("beta", entries.get(1));
        assertEquals(ManualScreen.LINE_HEIGHT, entries.get(1).y());
    }

    @Test
    void manualScreenLayoutsHeadersWithUpstreamScaleAndPreservesInlineStyle() {
        ManualDocument headerDocument = ManualDocument.parse(List.of("# Title", "next"), href -> null);

        List<ManualScreen.LayoutEntry> headerEntries = ManualScreen.layout(headerDocument, 100, text -> text.length() * 6);

        assertTrue(headerEntries.get(0).segment() instanceof ManualDocument.HeaderSegment);
        assertEquals(60, headerEntries.get(0).width());
        assertEquals(ManualScreen.LINE_HEIGHT * 2, headerEntries.get(0).height());
        assertEquals(ManualScreen.LINE_HEIGHT * 2, headerEntries.get(1).y());

        ManualDocument boldDocument = ManualDocument.parse(List.of("**alpha beta**"), href -> null);

        List<ManualScreen.LayoutEntry> boldEntries = ManualScreen.layout(boldDocument, 42, text -> text.length() * 6);

        assertEquals(2, boldEntries.size());
        assertTrue(boldEntries.get(0).segment() instanceof ManualDocument.BoldSegment);
        assertTextEntry("alpha", boldEntries.get(0));
        assertEquals(ManualScreen.LINE_HEIGHT, boldEntries.get(1).y());
        assertTrue(boldEntries.get(1).segment() instanceof ManualDocument.BoldSegment);
        assertTextEntry("beta", boldEntries.get(1));
    }

    @Test
    void manualScreenComputesDocumentHeightFromLayoutBottom() {
        ManualDocument document = ManualDocument.parse(
            List.of("alpha", "![tip](image:ok)"),
            href -> new TestImageRenderer(50, 20));

        assertEquals(ManualScreen.LINE_HEIGHT + ManualScreen.SEGMENT_PADDING + 20, ManualScreen.documentHeight(document, 230));
    }

    @Test
    void manualScreenClampsScrollOffsetToDocumentBounds() {
        assertEquals(0, ManualScreen.clampScrollOffset(-5, 300, 176));
        assertEquals(124, ManualScreen.clampScrollOffset(999, 300, 176));
        assertEquals(0, ManualScreen.clampScrollOffset(12, 100, 176));
    }

    @Test
    void manualScreenMapsScrollbarCoordinatesLikeUpstream() {
        assertTrue(ManualScreen.isCoordinateOverScrollBar(245, 6));
        assertTrue(ManualScreen.isCoordinateOverScrollBar(249, 185));
        assertFalse(ManualScreen.isCoordinateOverScrollBar(244, 6));
        assertFalse(ManualScreen.isCoordinateOverScrollBar(250, 185));
        assertFalse(ManualScreen.isCoordinateOverScrollBar(245, 186));
    }

    @Test
    void manualScreenMapsScrollThumbAndMouseOffsetLikeUpstream() {
        assertEquals(ManualScreen.SCROLL_POS_Y, ManualScreen.scrollbarThumbY(0, 356, 176));
        assertEquals(89, ManualScreen.scrollbarThumbY(90, 356, 176));
        assertEquals(173, ManualScreen.scrollbarThumbY(180, 356, 176));

        assertEquals(0, ManualScreen.scrollOffsetForMouseY(ManualScreen.SCROLL_POS_Y, 356, 176));
        assertEquals(90, ManualScreen.scrollOffsetForMouseY(ManualScreen.SCROLL_POS_Y + 90, 356, 176));
        assertEquals(180, ManualScreen.scrollOffsetForMouseY(ManualScreen.SCROLL_POS_Y + ManualScreen.SCROLL_HEIGHT, 356, 176));
    }

    @Test
    void manualScreenMapsTabCoordinatesLikeUpstream() {
        assertEquals(0, ManualScreen.tabIndexAt(-22, 8, 3));
        assertEquals(1, ManualScreen.tabIndexAt(-22, 33, 3));
        assertEquals(-1, ManualScreen.tabIndexAt(-24, 8, 3));
        assertEquals(-1, ManualScreen.tabIndexAt(-22, 8, 0));
        assertEquals(-1, ManualScreen.tabIndexAt(-22, 8, 8));
    }

    @Test
    void manualScreenFindsImageEntriesUnderMouseAfterScroll() {
        ManualDocument document = ManualDocument.parse(
            List.of("alpha ![tip](image:ok)"),
            href -> new TestImageRenderer(50, 20));
        ManualDocument.ImageSegment image = (ManualDocument.ImageSegment) ManualScreen.layout(document, 230).get(1).segment();

        assertSame(image, ManualScreen.interactiveImageAt(document, 100, 40, 191, 55, 0));
        assertSame(image, ManualScreen.interactiveImageAt(document, 100, 40, 191, 50, 5));
        assertNull(ManualScreen.interactiveImageAt(document, 100, 40, 80, 55, 0));
    }

    @Test
    void manualScreenFindsLinkEntriesUnderMouseAfterScroll() {
        ManualDocument document = ManualDocument.parse(List.of("Read [manual](item/manual.md)"), href -> null);
        ManualDocument.LinkSegment link = (ManualDocument.LinkSegment) ManualScreen.layout(document, 230, text -> text.length() * 6).get(1).segment();

        assertSame(link, ManualScreen.interactiveLinkAt(document, 100, 40, 132, 45, 0, text -> text.length() * 6));
        assertSame(link, ManualScreen.interactiveLinkAt(document, 100, 40, 132, 40, 5, text -> text.length() * 6));
        assertNull(ManualScreen.interactiveLinkAt(document, 100, 40, 80, 45, 0, text -> text.length() * 6));
    }

    @Test
    void manualScreenIgnoresInteractiveEntriesOutsideDocumentViewport() {
        ManualDocument document = ManualDocument.parse(
            List.of("Read [manual](item/manual.md)", "![tip](image:ok)"),
            href -> new TestImageRenderer(50, 20));

        assertNull(ManualScreen.interactiveLinkAt(document, 100, 40, 132, 25, 20, text -> text.length() * 6));
        assertNull(ManualScreen.interactiveImageAt(document, 100, 40, 191, 230, -160, text -> text.length() * 6));
    }

    @Test
    void manualScreenReportsLinkImageTabAndScrollbarTooltips() {
        ManualDocument document = ManualDocument.parse(
            List.of("Read [manual](item/manual.md)", "![tip](image:ok)"),
            href -> new TestImageRenderer(50, 20));
        List<ManualRegistry.ManualTab> tabs = List.of(new ManualRegistry.ManualTab(() -> {}, "tab.home", "index.md"));

        assertEquals(
            "item/manual.md",
            ManualScreen.tooltipAt(document, tabs, 8 + 32, 8 + 5, 0, false, text -> text.length() * 6));
        assertEquals(
            "tip",
            ManualScreen.tooltipAt(document, tabs, 8 + 91, 8 + ManualScreen.LINE_HEIGHT + ManualScreen.SEGMENT_PADDING + 1, 0, false, text -> text.length() * 6));
        assertEquals(
            "tab.home",
            ManualScreen.tooltipAt(document, tabs, -22, 8, 0, false, text -> text.length() * 6));
        ManualDocument longDocument = ManualDocument.parse(IntStream.range(0, 36).mapToObj(index -> "line " + index).toList(), href -> null);
        assertEquals(
            "50%",
            ManualScreen.tooltipAt(longDocument, tabs, 245, 100, 92, false, text -> text.length() * 6));
    }

    @Test
    void manualScreenTabClickNavigatesToTabPath() {
        ManualRegistry registry = new ManualRegistry();
        registry.addTab(() -> {}, "home", "index");
        registry.addTab(() -> {}, "items", "item/cpu1.md");
        ManualScreen screen = new ManualScreen(registry);
        screen.width = 400;
        screen.height = 300;

        boolean handled = screen.mouseClicked(50, 87, 0);

        assertTrue(handled);
        assertEquals("item/cpu1.md", registry.currentPath());
    }

    @Test
    void manualScreenLinkClickNavigatesRelativeToCurrentPath() {
        ManualRegistry registry = new ManualRegistry();
        registry.addProvider(path -> switch (path) {
            case "en_us/index.md" -> List.of("Read [manual](item/manual.md)");
            case "en_us/item/manual.md" -> List.of("Manual page");
            default -> null;
        });
        ManualScreen screen = new ManualScreen(registry);
        screen.width = 400;
        screen.height = 300;
        screen.refreshPage();

        boolean handled = screen.mouseClicked(112, 67, 0);

        assertTrue(handled);
        assertEquals("%LANGUAGE%/item/manual.md", registry.currentPath());
    }

    @Test
    void manualScreenRightClickReturnsToPreviousHistoryEntryLikeUpstream() {
        ManualRegistry registry = new ManualRegistry();
        registry.navigate("general/computer.md");
        registry.navigate("item/cpu1.md");
        TestManualScreen screen = new TestManualScreen(registry);
        screen.width = 400;
        screen.height = 300;

        boolean handled = screen.mouseClicked(200, 150, 1);

        assertTrue(handled);
        assertEquals("general/computer.md", registry.currentPath());
        assertEquals(0, screen.closeCount);
    }

    @Test
    void manualScreenRightClickClosesWhenNoHistoryLikeUpstream() {
        TestManualScreen screen = new TestManualScreen(new ManualRegistry());
        screen.width = 400;
        screen.height = 300;

        boolean handled = screen.mouseClicked(200, 150, 1);

        assertTrue(handled);
        assertEquals(1, screen.closeCount);
    }

    @Test
    void manualScreenJumpKeyReturnsToPreviousHistoryEntryLikeUpstream() {
        ManualRegistry registry = new ManualRegistry();
        registry.navigate("general/computer.md");
        registry.navigate("item/cpu1.md");
        TestManualScreen screen = new TestManualScreen(registry);

        boolean handled = screen.keyPressed(GLFW.GLFW_KEY_SPACE, 0, 0);

        assertTrue(handled);
        assertEquals("general/computer.md", registry.currentPath());
        assertEquals(0, screen.closeCount);
    }

    @Test
    void manualScreenJumpKeyClosesWhenNoHistoryLikeUpstream() {
        TestManualScreen screen = new TestManualScreen(new ManualRegistry());

        boolean handled = screen.keyPressed(GLFW.GLFW_KEY_SPACE, 0, 0);

        assertTrue(handled);
        assertEquals(1, screen.closeCount);
    }

    @Test
    void manualScreenScrollbarClickUpdatesScrollOffset() {
        ManualRegistry registry = new ManualRegistry();
        registry.addProvider(path -> IntStream.range(0, 50).mapToObj(index -> "line " + index).toList());
        ManualScreen screen = new ManualScreen(registry);
        screen.width = 400;
        screen.height = 300;
        screen.refreshPage();

        boolean handled = screen.mouseClicked(317, 239, 0);

        assertTrue(handled);
        assertTrue(screen.scrollOffset() > 0);
    }

    private static void assertTextEntry(final String expected, final ManualScreen.LayoutEntry entry) {
        assertTrue(entry.segment() instanceof ManualDocument.TextualSegment);
        ManualDocument.TextualSegment text = (ManualDocument.TextualSegment) entry.segment();
        assertEquals(expected, text.text());
    }

    private static final class TestManualScreen extends ManualScreen {
        private int closeCount;

        private TestManualScreen(final ManualRegistry registry) {
            super(registry);
        }

        @Override
        public void onClose() {
            closeCount++;
        }
    }

    private record TestImageRenderer(int getWidth, int getHeight) implements ImageRenderer {
        @Override
        public void render(final int mouseX, final int mouseY) {
        }
    }
}
