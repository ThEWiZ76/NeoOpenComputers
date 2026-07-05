package li.cil.oc.client;

import li.cil.oc.common.ManualRegistry;
import li.cil.oc.api.manual.ImageRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ManualScreenShapeTest {
    private static final Path MANUAL_SCREEN_SOURCE = Path.of("src/main/java/li/cil/oc/client/ManualScreen.java");
    private static final Path CLIENT_SOURCE = Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java");

    @Test
    void manualScreenHasRegistryConstructor() throws NoSuchMethodException {
        final Constructor<ManualScreen> constructor = ManualScreen.class.getConstructor(ManualRegistry.class);

        assertTrue(Screen.class.isAssignableFrom(ManualScreen.class));
        assertArrayEquals(new Class<?>[]{ManualRegistry.class}, constructor.getParameterTypes());
    }

    @Test
    void clientSetupPreloadsManualScreenBeforeManualItemUse() throws IOException {
        final String source = Files.readString(CLIENT_SOURCE);

        assertTrue(source.contains("preloadClientOnlyClasses();"),
            "Client setup must preload manual GUI classes before ManualItem can open the manual.");
        assertTrue(source.contains("\"li.cil.oc.client.ManualScreen\""),
            "Fresh alpha smoke showed ManualItem use can lazily fail loading ManualScreen.");
        assertTrue(source.contains("Class.forName(className, true, loader)"),
            "Client-only preload must initialize classes, matching the common crash-hardening preloader.");
    }

    @Test
    void manualScreenUsesMinecraftPlatformOpenerForExternalLinks() throws IOException {
        final String source = Files.readString(MANUAL_SCREEN_SOURCE);

        assertTrue(source.contains("Util.getPlatform().openUri(uri);"));
        assertFalse(source.contains("java.awt.Desktop"));
        assertFalse(source.contains("Desktop.getDesktop()"));
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
    void manualScreenRendersPlainScreenLayerBeforeBookContent() throws IOException {
        final String source = Files.readString(MANUAL_SCREEN_SOURCE);
        final int superRender = source.indexOf("super.render(graphics, mouseX, mouseY, partialTick);");
        final int bookTexture = source.indexOf("graphics.blit(MANUAL_TEXTURE");
        final int documentRender = source.indexOf("renderDocumentClipped(graphics");

        assertTrue(superRender >= 0, "Manual screen must keep vanilla Screen render call.");
        assertTrue(superRender < bookTexture, "Plain Screen render layer must not run over the manual texture.");
        assertTrue(superRender < documentRender, "Plain Screen render layer must not run over manual text.");
    }

    @Test
    void manualScreenLetsVanillaSuperRenderOwnBackgroundPass() throws IOException {
        final String source = Files.readString(MANUAL_SCREEN_SOURCE);
        final int renderMethod = source.indexOf("public void render(final GuiGraphics graphics");
        final int superRender = source.indexOf("super.render(graphics, mouseX, mouseY, partialTick);", renderMethod);
        final int explicitBackground = source.indexOf("renderBackground(graphics, mouseX, mouseY, partialTick);", renderMethod);

        assertTrue(renderMethod >= 0, "Manual render method must be present.");
        assertTrue(superRender >= 0, "Manual render must keep vanilla Screen render call.");
        assertTrue(explicitBackground < 0 || explicitBackground > superRender,
            "Manual screen must not render the vanilla background before super.render; Screen.render already does that and double blur makes the manual fuzzy.");
    }

    @Test
    void manualScreenPassesGuiGraphicsToManualImageRenderers() throws IOException {
        final String source = Files.readString(MANUAL_SCREEN_SOURCE);

        assertTrue(source.contains("ManualRenderContext.withGraphics(graphics, () -> tab.renderer().render())"),
            "Tab item renderers must draw into the manual GuiGraphics pose, not a fresh top-left GuiGraphics.");
        assertTrue(source.contains("ManualRenderContext.withGraphics(graphics, () -> image.renderAt(left + entry.x(), y, mouseX, mouseY))"),
            "Inline item/block image renderers must draw into the clipped manual GuiGraphics pose.");
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
    void manualScreenWrapsAtPunctuationBreakCharactersLikeUpstream() {
        ManualDocument document = ManualDocument.parse(List.of("alpha-beta"), href -> null);

        List<ManualScreen.LayoutEntry> entries = ManualScreen.layout(document, 35, text -> text.length() * 6);

        assertEquals(2, entries.size());
        assertTextEntry("alpha-", entries.get(0));
        assertEquals(0, entries.get(0).y());
        assertTextEntry("beta", entries.get(1));
        assertEquals(ManualScreen.LINE_HEIGHT, entries.get(1).y());
    }

    @Test
    void manualScreenIndentsWrappedListLinesLikeUpstream() {
        ManualDocument document = ManualDocument.parse(List.of("- alpha beta"), href -> null);

        List<ManualScreen.LayoutEntry> entries = ManualScreen.layout(document, 42, text -> text.length() * 6);

        assertEquals(2, entries.size());
        assertTextEntry("- alpha", entries.get(0));
        assertEquals(0, entries.get(0).x());
        assertTextEntry("beta", entries.get(1));
        assertEquals(12, entries.get(1).x());
        assertEquals(ManualScreen.LINE_HEIGHT, entries.get(1).y());
    }

    @Test
    void manualScreenIndentsWrappedListLinksLikeUpstream() {
        ManualDocument document = ManualDocument.parse(List.of("- [alpha beta](item/manual.md)"), href -> null);

        List<ManualScreen.LayoutEntry> entries = ManualScreen.layout(document, 42, text -> text.length() * 6);

        assertEquals(3, entries.size());
        assertTextEntry("- ", entries.get(0));
        assertEquals(0, entries.get(0).x());
        assertTextEntry("alpha", entries.get(1));
        assertEquals(12, entries.get(1).x());
        assertTextEntry("beta", entries.get(2));
        assertEquals(12, entries.get(2).x());
        assertEquals(ManualScreen.LINE_HEIGHT, entries.get(2).y());
        assertTrue(entries.get(1).segment() instanceof ManualDocument.LinkSegment);
        assertTrue(entries.get(2).segment() instanceof ManualDocument.LinkSegment);
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
    void manualScreenPreservesLeadingSpacesInCodeSegmentsLikeUpstream() {
        ManualDocument document = ManualDocument.parse(List.of("`  indented`"), href -> null);

        List<ManualScreen.LayoutEntry> entries = ManualScreen.layout(document, 120, text -> text.length() * 6);

        assertEquals(1, entries.size());
        assertTrue(entries.get(0).segment() instanceof ManualDocument.CodeSegment);
        assertTextEntry("  indented", entries.get(0));
        assertEquals(0, entries.get(0).x());
    }

    @Test
    void manualScreenWrapsStyledLinksWithoutDroppingStyle() {
        ManualDocument document = ManualDocument.parse(List.of("[**alpha beta**](item/manual.md)"), href -> null);

        List<ManualScreen.LayoutEntry> entries = ManualScreen.layout(document, 42, text -> text.length() * 6);

        assertEquals(2, entries.size());
        ManualDocument.LinkSegment first = (ManualDocument.LinkSegment) entries.get(0).segment();
        ManualDocument.LinkSegment second = (ManualDocument.LinkSegment) entries.get(1).segment();
        assertEquals("alpha", first.text());
        assertEquals("beta", second.text());
        assertEquals("item/manual.md", first.href());
        assertEquals("item/manual.md", second.href());
        assertTrue(first.bold());
        assertTrue(second.bold());
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
    void manualScreenColorsManualLinksByUpstreamAvailability() {
        ManualRegistry registry = new ManualRegistry();
        registry.addProvider(path -> switch (path) {
            case "en_us/item/manual.md" -> List.of("Manual page");
            default -> null;
        });
        ManualDocument.LinkSegment existing = new ManualDocument.LinkSegment("manual", "item/manual.md");
        ManualDocument.LinkSegment missing = new ManualDocument.LinkSegment("missing", "item/missing.md");
        ManualDocument.LinkSegment external = new ManualDocument.LinkSegment("site", "https://example.com");

        assertTrue(ManualScreen.isLinkAvailable(existing, registry, "%LANGUAGE%/index.md"));
        assertFalse(ManualScreen.isLinkAvailable(missing, registry, "%LANGUAGE%/index.md"));
        assertTrue(ManualScreen.isLinkAvailable(external, registry, "%LANGUAGE%/index.md"));
        assertEquals(0xFF66FF66, ManualScreen.linkTextColor(existing, registry, "%LANGUAGE%/index.md", false));
        assertEquals(0xFFAAFFAA, ManualScreen.linkTextColor(existing, registry, "%LANGUAGE%/index.md", true));
        assertEquals(0xFFFF6666, ManualScreen.linkTextColor(missing, registry, "%LANGUAGE%/index.md", false));
        assertEquals(0xFFFFAAAA, ManualScreen.linkTextColor(missing, registry, "%LANGUAGE%/index.md", true));
    }

    @Test
    void manualScreenLocalizesAndSplitsTooltipsLikeUpstream() {
        Map<String, String> translations = Map.of(
            "oc:gui.Manual.Home", " Home [nl] Main page ",
            "oc:gui.Manual.Warning.ImageMissing", " Image not found. ");

        List<Component> home = ManualScreen.localizedTooltipComponents(
            "oc:gui.Manual.Home",
            translations::get,
            translations::containsKey);
        List<Component> fallback = ManualScreen.localizedTooltipComponents(
            "plain tooltip",
            translations::get,
            translations::containsKey);

        assertEquals(List.of("Home", "Main page"), home.stream().map(Component::getString).toList());
        assertEquals(List.of("plain tooltip"), fallback.stream().map(Component::getString).toList());
    }

    @Test
    void manualScreenReportsExternalLinkOpenFailuresLikeUpstream() {
        final List<Component> warnings = new ArrayList<>();

        final boolean opened = ManualScreen.openExternalLink(
            "https://example.com",
            uri -> {
                throw new IOException("boom");
            },
            warnings::add);

        assertFalse(opened);
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).getContents() instanceof TranslatableContents);
        final TranslatableContents contents = (TranslatableContents) warnings.get(0).getContents();
        assertEquals("oc:gui.Chat.WarningLink", contents.getKey());
        assertEquals("java.io.IOException: boom", contents.getArgs()[0]);
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
    void manualScreenInventoryKeyClosesLikeUpstream() {
        TestManualScreen screen = new TestManualScreen(new ManualRegistry());

        boolean handled = screen.keyPressed(GLFW.GLFW_KEY_E, 0, 0);

        assertTrue(handled);
        assertEquals(1, screen.closeCount);
    }

    @Test
    void manualScreenRefreshRestoresCurrentHistoryScrollOffset() {
        ManualRegistry registry = new ManualRegistry();
        registry.addProvider(path -> IntStream.range(0, 50).mapToObj(index -> "line " + index).toList());
        registry.setCurrentOffset(90);
        ManualScreen screen = new ManualScreen(registry);

        screen.refreshPage();

        assertEquals(90, screen.scrollOffset());
    }

    @Test
    void manualScreenStoresScrollOffsetInCurrentHistoryEntry() {
        ManualRegistry registry = new ManualRegistry();
        registry.addProvider(path -> IntStream.range(0, 50).mapToObj(index -> "line " + index).toList());
        ManualScreen screen = new ManualScreen(registry);
        screen.width = 400;
        screen.height = 300;
        screen.refreshPage();

        boolean handled = screen.mouseClicked(317, 239, 0);

        assertTrue(handled);
        assertEquals(screen.scrollOffset(), registry.currentOffset());
    }

    @Test
    void manualScreenBackNavigationRestoresPreviousPageScrollOffset() {
        ManualRegistry registry = new ManualRegistry();
        registry.addProvider(path -> IntStream.range(0, 50).mapToObj(index -> "line " + index).toList());
        registry.setCurrentOffset(75);
        registry.navigate("item/cpu1.md");
        registry.setCurrentOffset(0);
        ManualScreen screen = new ManualScreen(registry);
        screen.width = 400;
        screen.height = 300;

        boolean handled = screen.mouseClicked(200, 150, 1);

        assertTrue(handled);
        assertEquals("%LANGUAGE%/index.md", registry.currentPath());
        assertEquals(75, screen.scrollOffset());
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
