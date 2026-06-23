package li.cil.oc.client;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.ManualRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;

public class ManualScreen extends Screen {
    public static final ResourceLocation MANUAL_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "textures/gui/manual.png");
    public static final ResourceLocation TAB_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "textures/gui/manual_tab.png");
    public static final ResourceLocation SCROLL_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "textures/gui/button_scroll.png");
    public static final int WINDOW_WIDTH = 256;
    public static final int WINDOW_HEIGHT = 192;
    public static final int DOCUMENT_MAX_WIDTH = 230;
    public static final int DOCUMENT_MAX_HEIGHT = 176;
    public static final int MAX_TABS_PER_SIDE = 7;
    public static final int LINE_HEIGHT = 10;
    public static final int SEGMENT_PADDING = 4;
    public static final int TAB_POS_X = -23;
    public static final int TAB_POS_Y = 7;
    public static final int TAB_WIDTH = 23;
    public static final int TAB_HEIGHT = 26;
    public static final int TAB_TEXTURE_WIDTH = 23;
    public static final int TAB_TEXTURE_HEIGHT = 52;
    public static final int SCROLL_POS_X = 244;
    public static final int SCROLL_POS_Y = 6;
    public static final int SCROLL_WIDTH = 6;
    public static final int SCROLL_HEIGHT = 180;
    public static final int SCROLL_THUMB_HEIGHT = 13;
    public static final int SCROLL_TEXTURE_WIDTH = 6;
    public static final int SCROLL_TEXTURE_HEIGHT = 26;
    private static final int DOCUMENT_POS_X = 8;
    private static final int DOCUMENT_POS_Y = 8;
    private static final int SCROLL_STEP = LINE_HEIGHT * 3;
    private static final int DEFAULT_CHAR_WIDTH = 6;

    private final ManualRegistry registry;
    private ManualDocument document = ManualDocument.parse(List.of());
    private int scrollOffset;
    private boolean draggingScrollBar;

    public ManualScreen(final ManualRegistry registry) {
        super(title());
        this.registry = Objects.requireNonNull(registry);
    }

    public static Component title() {
        return Component.translatable("gui.neoopencomputers.manual");
    }

    public static void open(final ManualRegistry registry) {
        Minecraft.getInstance().setScreen(new ManualScreen(registry));
    }

    public void refreshPage() {
        final Iterable<String> content = registry.contentFor(registry.currentPath());
        document = ManualDocument.parse(content == null ? List.of("Document not found: " + registry.currentPath()) : content);
        scrollOffset = clampScrollOffset(scrollOffset, documentHeight(document, DOCUMENT_MAX_WIDTH), DOCUMENT_MAX_HEIGHT);
    }

    public ManualDocument document() {
        return document;
    }

    public int scrollOffset() {
        return scrollOffset;
    }

    public static List<LayoutEntry> layout(final ManualDocument document, final int maxWidth) {
        return layout(document, maxWidth, text -> text.length() * DEFAULT_CHAR_WIDTH);
    }

    public static List<LayoutEntry> layout(final ManualDocument document, final int maxWidth, final ToIntFunction<String> textWidth) {
        int y = 0;
        int x = 0;
        int lineHeight = LINE_HEIGHT;
        final var entries = new java.util.ArrayList<LayoutEntry>();
        for (final ManualDocument.Segment segment : document.segments()) {
            if (segment instanceof final ManualDocument.ImageSegment image) {
                if (x > 0) {
                    y += lineHeight;
                    x = 0;
                    lineHeight = LINE_HEIGHT;
                }
                y += entries.isEmpty() ? 2 : SEGMENT_PADDING;
                final int width = Math.min(maxWidth, image.renderer().getWidth());
                final int height = image.renderer().getHeight();
                entries.add(new LayoutEntry(segment, (maxWidth - width) / 2, y, width, height));
                y += height;
            } else if (segment instanceof ManualDocument.LineBreakSegment) {
                y += lineHeight;
                x = 0;
                lineHeight = LINE_HEIGHT;
            } else {
                final TextFlow flow = appendTextEntries(entries, segment, x, y, lineHeight, maxWidth, textWidth);
                x = flow.x();
                y = flow.y();
                lineHeight = flow.lineHeight();
            }
        }
        return List.copyOf(entries);
    }

    private static TextFlow appendTextEntries(
        final java.util.ArrayList<LayoutEntry> entries,
        final ManualDocument.Segment segment,
        final int startX,
        final int startY,
        final int startLineHeight,
        final int maxWidth,
        final ToIntFunction<String> textWidth
    ) {
        String remaining = segmentText(segment);
        if (remaining.isEmpty()) {
            return new TextFlow(startX, startY, startLineHeight);
        }
        int x = startX;
        int y = startY;
        int lineHeight = startLineHeight;
        while (!remaining.isEmpty()) {
            if (x == 0) {
                remaining = remaining.stripLeading();
            }
            if (remaining.isEmpty()) {
                break;
            }
            final String part = fittingText(remaining, maxWidth - x, maxWidth, segment, textWidth);
            if (part.isEmpty()) {
                y += lineHeight;
                x = 0;
                lineHeight = LINE_HEIGHT;
                continue;
            }
            final String segmentText = segmentText(segment);
            final ManualDocument.Segment entrySegment = part.equals(segmentText) ? segment : copyTextSegment(segment, part);
            final int width = textPixelWidth(entrySegment, textWidth);
            final int height = textLineHeight(entrySegment);
            entries.add(new LayoutEntry(entrySegment, x, y, width, height));
            remaining = remaining.substring(part.length()).stripLeading();
            x += width;
            lineHeight = Math.max(lineHeight, height);
            if (x >= maxWidth && !remaining.isEmpty()) {
                y += lineHeight;
                x = 0;
                lineHeight = LINE_HEIGHT;
            }
        }
        return new TextFlow(x, y, lineHeight);
    }

    private static String segmentText(final ManualDocument.Segment segment) {
        if (segment instanceof final ManualDocument.TextualSegment text) {
            return text.text();
        }
        return "";
    }

    private static ManualDocument.Segment copyTextSegment(final ManualDocument.Segment segment, final String text) {
        if (segment instanceof final ManualDocument.LinkSegment link) {
            return new ManualDocument.LinkSegment(text, link.href(), link.headerLevel());
        }
        if (segment instanceof final ManualDocument.HeaderSegment header) {
            return new ManualDocument.HeaderSegment(text, header.level());
        }
        if (segment instanceof final ManualDocument.BoldSegment bold) {
            return new ManualDocument.BoldSegment(text, bold.headerLevel());
        }
        if (segment instanceof final ManualDocument.ItalicSegment italic) {
            return new ManualDocument.ItalicSegment(text, italic.headerLevel());
        }
        if (segment instanceof final ManualDocument.CodeSegment code) {
            return new ManualDocument.CodeSegment(text, code.headerLevel());
        }
        if (segment instanceof final ManualDocument.StrikethroughSegment strike) {
            return new ManualDocument.StrikethroughSegment(text, strike.headerLevel());
        }
        return new ManualDocument.TextSegment(text);
    }

    private static String fittingText(
        final String text,
        final int availableWidth,
        final int maxWidth,
        final ManualDocument.Segment segment,
        final ToIntFunction<String> textWidth
    ) {
        if (availableWidth <= 0) {
            return "";
        }
        if (textPixelWidth(segment, text, textWidth) <= availableWidth) {
            return text;
        }
        final int firstWhitespace = firstWhitespace(text);
        final String firstWord = firstWhitespace < 0 ? text : text.substring(0, firstWhitespace);
        if (!firstWord.isEmpty()
            && textPixelWidth(segment, firstWord, textWidth) > availableWidth
            && textPixelWidth(segment, firstWord, textWidth) <= maxWidth) {
            return "";
        }
        int bestLength = 0;
        int lastWhitespace = -1;
        for (int index = 1; index <= text.length(); index++) {
            final char current = text.charAt(index - 1);
            if (Character.isWhitespace(current)) {
                lastWhitespace = index - 1;
            }
            if (textPixelWidth(segment, text.substring(0, index), textWidth) > availableWidth) {
                break;
            }
            bestLength = index;
        }
        if (lastWhitespace > 0 && textPixelWidth(segment, text.substring(0, lastWhitespace), textWidth) <= availableWidth) {
            return text.substring(0, lastWhitespace);
        }
        return bestLength > 0 ? text.substring(0, bestLength) : "";
    }

    private static int firstWhitespace(final String text) {
        for (int index = 0; index < text.length(); index++) {
            if (Character.isWhitespace(text.charAt(index))) {
                return index;
            }
        }
        return -1;
    }

    private static int textPixelWidth(final ManualDocument.Segment segment, final ToIntFunction<String> textWidth) {
        return textPixelWidth(segment, segmentText(segment), textWidth);
    }

    private static int textPixelWidth(final ManualDocument.Segment segment, final String text, final ToIntFunction<String> textWidth) {
        return (int) Math.ceil(textWidth.applyAsInt(text) * textScale(segment));
    }

    private static int textLineHeight(final ManualDocument.Segment segment) {
        return (int) Math.ceil(LINE_HEIGHT * textScale(segment));
    }

    private static float textScale(final ManualDocument.Segment segment) {
        if (segment instanceof final ManualDocument.TextualSegment text && text.headerLevel() > 0) {
            return Math.max(2, 5 - text.headerLevel()) / 2.0f;
        }
        return 1.0f;
    }

    public static int documentHeight(final ManualDocument document, final int maxWidth) {
        int height = 0;
        for (final LayoutEntry entry : layout(document, maxWidth)) {
            height = Math.max(height, entry.y() + entry.height());
        }
        return height;
    }

    public static int clampScrollOffset(final int requestedOffset, final int documentHeight, final int viewportHeight) {
        return Math.max(0, Math.min(requestedOffset, Math.max(0, documentHeight - viewportHeight)));
    }

    public static boolean isCoordinateOverScrollBar(final int x, final int y) {
        return x > SCROLL_POS_X && x < SCROLL_POS_X + SCROLL_WIDTH && y >= SCROLL_POS_Y && y < SCROLL_POS_Y + SCROLL_HEIGHT;
    }

    public static int scrollbarThumbY(final int scrollOffset, final int documentHeight, final int viewportHeight) {
        final int maxOffset = Math.max(0, documentHeight - viewportHeight);
        if (maxOffset == 0) {
            return SCROLL_POS_Y;
        }
        return SCROLL_POS_Y + (SCROLL_HEIGHT - SCROLL_THUMB_HEIGHT) * clampScrollOffset(scrollOffset, documentHeight, viewportHeight) / maxOffset;
    }

    public static int scrollOffsetForMouseY(final int mouseY, final int documentHeight, final int viewportHeight) {
        final int maxOffset = Math.max(0, documentHeight - viewportHeight);
        if (maxOffset == 0) {
            return 0;
        }
        final double offset = (mouseY - SCROLL_POS_Y - SCROLL_THUMB_HEIGHT / 2.0) * maxOffset / (SCROLL_HEIGHT - (double) SCROLL_THUMB_HEIGHT);
        return clampScrollOffset((int) Math.round(offset), documentHeight, viewportHeight);
    }

    public static int tabIndexAt(final int x, final int y, final int tabCount) {
        if (tabCount <= 0 || tabCount > MAX_TABS_PER_SIDE) {
            return -1;
        }
        if (x <= TAB_POS_X || x >= TAB_POS_X + TAB_WIDTH) {
            return -1;
        }
        for (int index = 0; index < tabCount; index++) {
            final int tabY = TAB_POS_Y + index * (TAB_HEIGHT - 1);
            if (y > tabY && y < tabY + TAB_HEIGHT) {
                return index;
            }
        }
        return -1;
    }

    public static int buttonTextureYOffset(final boolean hovered, final int buttonHeight) {
        return hovered ? buttonHeight : 0;
    }

    public static int tabTextureYOffset(final int tabIndex, final int mouseX, final int mouseY) {
        final int tabY = TAB_POS_Y + tabIndex * (TAB_HEIGHT - 1);
        final boolean hovered = mouseX >= TAB_POS_X && mouseX < TAB_POS_X + TAB_WIDTH
            && mouseY >= tabY && mouseY < tabY + TAB_HEIGHT;
        return buttonTextureYOffset(hovered, TAB_HEIGHT);
    }

    public static int scrollTextureYOffset(
        final boolean draggingScrollBar,
        final int mouseX,
        final int mouseY,
        final int scrollOffset,
        final int documentHeight,
        final int viewportHeight
    ) {
        final int thumbY = scrollbarThumbY(scrollOffset, documentHeight, viewportHeight);
        final boolean hovered = mouseX >= SCROLL_POS_X && mouseX < SCROLL_POS_X + SCROLL_WIDTH
            && mouseY >= thumbY && mouseY < thumbY + SCROLL_THUMB_HEIGHT;
        return buttonTextureYOffset(draggingScrollBar || hovered, SCROLL_THUMB_HEIGHT);
    }

    public static ClipRect documentClipRect(final int left, final int top) {
        return new ClipRect(
            left + DOCUMENT_POS_X,
            top + DOCUMENT_POS_Y,
            left + DOCUMENT_POS_X + DOCUMENT_MAX_WIDTH,
            top + DOCUMENT_POS_Y + DOCUMENT_MAX_HEIGHT);
    }

    public static boolean isCoordinateOverDocument(final int x, final int y) {
        return x >= DOCUMENT_POS_X && x < DOCUMENT_POS_X + DOCUMENT_MAX_WIDTH
            && y >= DOCUMENT_POS_Y && y < DOCUMENT_POS_Y + DOCUMENT_MAX_HEIGHT;
    }

    public static ManualDocument.ImageSegment interactiveImageAt(
        final ManualDocument document,
        final int left,
        final int top,
        final int mouseX,
        final int mouseY,
        final int scrollOffset
    ) {
        return interactiveImageAt(document, left, top, mouseX, mouseY, scrollOffset, text -> text.length() * DEFAULT_CHAR_WIDTH);
    }

    public static ManualDocument.ImageSegment interactiveImageAt(
        final ManualDocument document,
        final int left,
        final int top,
        final int mouseX,
        final int mouseY,
        final int scrollOffset,
        final ToIntFunction<String> textWidth
    ) {
        if (!isAbsoluteCoordinateOverDocument(left, top, mouseX, mouseY)) {
            return null;
        }
        for (final LayoutEntry entry : layout(document, DOCUMENT_MAX_WIDTH, textWidth)) {
            if (entry.segment() instanceof final ManualDocument.ImageSegment image) {
                final int x = left + entry.x();
                final int y = top + entry.y() - scrollOffset;
                if (mouseX >= x && mouseX < x + entry.width() && mouseY >= y && mouseY < y + entry.height()) {
                    return image;
                }
            }
        }
        return null;
    }

    public static ManualDocument.LinkSegment interactiveLinkAt(
        final ManualDocument document,
        final int left,
        final int top,
        final int mouseX,
        final int mouseY,
        final int scrollOffset,
        final ToIntFunction<String> textWidth
    ) {
        if (!isAbsoluteCoordinateOverDocument(left, top, mouseX, mouseY)) {
            return null;
        }
        for (final LayoutEntry entry : layout(document, DOCUMENT_MAX_WIDTH, textWidth)) {
            if (entry.segment() instanceof final ManualDocument.LinkSegment link) {
                final int x = left + entry.x();
                final int y = top + entry.y() - scrollOffset;
                if (mouseX >= x && mouseX < x + entry.width() && mouseY >= y && mouseY < y + entry.height()) {
                    return link;
                }
            }
        }
        return null;
    }

    private static boolean isAbsoluteCoordinateOverDocument(final int left, final int top, final int mouseX, final int mouseY) {
        return mouseX >= left && mouseX < left + DOCUMENT_MAX_WIDTH
            && mouseY >= top && mouseY < top + DOCUMENT_MAX_HEIGHT;
    }

    public static String tooltipAt(
        final ManualDocument document,
        final List<ManualRegistry.ManualTab> tabs,
        final int mouseX,
        final int mouseY,
        final int scrollOffset,
        final boolean draggingScrollBar,
        final ToIntFunction<String> textWidth
    ) {
        final ManualDocument.LinkSegment link = interactiveLinkAt(document, DOCUMENT_POS_X, DOCUMENT_POS_Y, mouseX, mouseY, scrollOffset, textWidth);
        if (link != null) {
            return link.tooltip();
        }
        final ManualDocument.ImageSegment image = interactiveImageAt(document, DOCUMENT_POS_X, DOCUMENT_POS_Y, mouseX, mouseY, scrollOffset, textWidth);
        if (image != null) {
            return image.tooltip();
        }
        final int tabIndex = tabIndexAt(mouseX, mouseY, tabs.size());
        if (tabIndex >= 0) {
            return tabs.get(tabIndex).tooltip();
        }
        final int documentHeight = documentHeight(document, DOCUMENT_MAX_WIDTH);
        final int maxOffset = Math.max(0, documentHeight - DOCUMENT_MAX_HEIGHT);
        if (maxOffset > 0 && (draggingScrollBar || isCoordinateOverScrollBar(mouseX, mouseY))) {
            return 100 * clampScrollOffset(scrollOffset, documentHeight, DOCUMENT_MAX_HEIGHT) / maxOffset + "%";
        }
        return null;
    }

    @Override
    protected void init() {
        refreshPage();
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        final int left = (width - WINDOW_WIDTH) / 2;
        final int top = (height - WINDOW_HEIGHT) / 2;
        graphics.blit(MANUAL_TEXTURE, left, top, 0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        renderTabs(graphics, left, top, mouseX - left, mouseY - top);
        renderDocumentClipped(graphics, left, top, mouseX, mouseY);
        renderScrollBar(graphics, left, top, mouseX - left, mouseY - top);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, left, top, mouseX, mouseY);
    }

    private void renderTabs(final GuiGraphics graphics, final int left, final int top, final int mouseX, final int mouseY) {
        final List<ManualRegistry.ManualTab> tabs = registry.tabs();
        for (int index = 0; index < Math.min(tabs.size(), MAX_TABS_PER_SIDE); index++) {
            final int x = left + TAB_POS_X;
            final int y = top + TAB_POS_Y + index * (TAB_HEIGHT - 1);
            graphics.blit(TAB_TEXTURE, x, y, 0, tabTextureYOffset(index, mouseX, mouseY), TAB_WIDTH, TAB_HEIGHT);
            graphics.pose().pushPose();
            graphics.pose().translate(x + 4, y + 5, 0);
            tabs.get(index).renderer().render();
            graphics.pose().popPose();
        }
    }

    private void renderDocumentClipped(final GuiGraphics graphics, final int left, final int top, final int mouseX, final int mouseY) {
        final ClipRect clip = documentClipRect(left, top);
        graphics.enableScissor(clip.left(), clip.top(), clip.right(), clip.bottom());
        try {
            renderDocument(graphics, clip.left(), clip.top(), mouseX, mouseY);
        } finally {
            graphics.disableScissor();
        }
    }

    private void renderDocument(final GuiGraphics graphics, final int left, final int top, final int mouseX, final int mouseY) {
        for (final LayoutEntry entry : layout(document, DOCUMENT_MAX_WIDTH, this::textWidth)) {
            final int y = top + entry.y() - scrollOffset;
            if (entry.segment() instanceof final ManualDocument.TextualSegment text) {
                renderTextSegment(graphics, text, left + entry.x(), y);
            } else if (entry.segment() instanceof final ManualDocument.ImageSegment image) {
                graphics.pose().pushPose();
                graphics.pose().translate(left + entry.x(), y, 0);
                image.renderAt(left + entry.x(), y, mouseX, mouseY);
                graphics.pose().popPose();
            }
        }
    }

    private void renderTextSegment(final GuiGraphics graphics, final ManualDocument.TextualSegment text, final int x, final int y) {
        final float scale = textScale(text);
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, styledComponent(text), 0, 0, textColor(text), false);
        graphics.pose().popPose();
    }

    private static Component styledComponent(final ManualDocument.TextualSegment text) {
        Component component = Component.literal(text.text());
        if (text.headerLevel() > 0) {
            component = component.copy().withStyle(ChatFormatting.UNDERLINE);
        }
        if (text.bold()) {
            component = component.copy().withStyle(ChatFormatting.BOLD);
        }
        if (text.italic()) {
            component = component.copy().withStyle(ChatFormatting.ITALIC);
        }
        if (text.strikethrough()) {
            component = component.copy().withStyle(ChatFormatting.STRIKETHROUGH);
        }
        return component;
    }

    private static int textColor(final ManualDocument.TextualSegment text) {
        if (text instanceof ManualDocument.LinkSegment) {
            return 0xFF66FF66;
        }
        if (text.code()) {
            return 0xFFBFCBFF;
        }
        return 0xFFE5E9F0;
    }

    private void renderScrollBar(final GuiGraphics graphics, final int left, final int top, final int mouseX, final int mouseY) {
        final int documentHeight = documentHeight(document, DOCUMENT_MAX_WIDTH);
        final int trackX = left + SCROLL_POS_X;
        final int thumbY = top + scrollbarThumbY(scrollOffset, documentHeight, DOCUMENT_MAX_HEIGHT);
        final int textureY = scrollTextureYOffset(draggingScrollBar, mouseX, mouseY, scrollOffset, documentHeight, DOCUMENT_MAX_HEIGHT);
        graphics.blit(SCROLL_TEXTURE, trackX, thumbY, 0, textureY, SCROLL_WIDTH, SCROLL_THUMB_HEIGHT);
    }

    private void renderTooltip(final GuiGraphics graphics, final int left, final int top, final int mouseX, final int mouseY) {
        final String tooltip = tooltipAt(
            document,
            registry.tabs(),
            mouseX - left,
            mouseY - top,
            scrollOffset,
            draggingScrollBar,
            this::textWidth);
        if (tooltip != null && !tooltip.isBlank()) {
            graphics.renderTooltip(font, Component.literal(tooltip), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
        final int documentHeight = documentHeight(document, DOCUMENT_MAX_WIDTH);
        final int nextOffset = clampScrollOffset(scrollOffset - (int) Math.signum(scrollY) * SCROLL_STEP, documentHeight, DOCUMENT_MAX_HEIGHT);
        if (nextOffset != scrollOffset) {
            scrollOffset = nextOffset;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == 0) {
            final int left = (width - WINDOW_WIDTH) / 2;
            final int top = (height - WINDOW_HEIGHT) / 2;
            if (canScroll() && isCoordinateOverScrollBar((int) mouseX - left, (int) mouseY - top)) {
                draggingScrollBar = true;
                scrollToMouse((int) mouseY - top);
                return true;
            }
            final int tabIndex = tabIndexAt((int) mouseX - left, (int) mouseY - top, registry.tabs().size());
            if (tabIndex >= 0) {
                registry.navigate(registry.tabs().get(tabIndex).path());
                scrollOffset = 0;
                refreshPage();
                return true;
            }
            final ManualDocument.LinkSegment link = interactiveLinkAt(
                document,
                left + DOCUMENT_POS_X,
                top + DOCUMENT_POS_Y,
                (int) mouseX,
                (int) mouseY,
                scrollOffset,
                this::textWidth);
            if (link != null) {
                if (link.href().startsWith("http://") || link.href().startsWith("https://")) {
                    openExternalLink(link.href());
                } else {
                    registry.navigate(ManualRegistry.resolveLinkPath(link.href(), registry.currentPath()));
                    scrollOffset = 0;
                    refreshPage();
                }
                return true;
            }
            final ManualDocument.ImageSegment image = interactiveImageAt(
                document,
                left + DOCUMENT_POS_X,
                top + DOCUMENT_POS_Y,
                (int) mouseX,
                (int) mouseY,
                scrollOffset,
                this::textWidth);
            if (image != null && image.onMouseClick((int) mouseX, (int) mouseY)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(final double mouseX, final double mouseY, final int button, final double dragX, final double dragY) {
        if (draggingScrollBar && button == 0) {
            final int top = (height - WINDOW_HEIGHT) / 2;
            scrollToMouse((int) mouseY - top);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        if (button == 0 && draggingScrollBar) {
            draggingScrollBar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean canScroll() {
        return documentHeight(document, DOCUMENT_MAX_WIDTH) > DOCUMENT_MAX_HEIGHT;
    }

    private void scrollToMouse(final int mouseY) {
        scrollOffset = scrollOffsetForMouseY(mouseY, documentHeight(document, DOCUMENT_MAX_WIDTH), DOCUMENT_MAX_HEIGHT);
    }

    private int textWidth(final String text) {
        return font == null ? text.length() * DEFAULT_CHAR_WIDTH : font.width(text);
    }

    private static void openExternalLink(final String href) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(href));
            }
        } catch (final RuntimeException | java.io.IOException ignored) {
        }
    }

    public record LayoutEntry(ManualDocument.Segment segment, int x, int y, int width, int height) {
    }

    public record ClipRect(int left, int top, int right, int bottom) {
    }

    private record TextFlow(int x, int y, int lineHeight) {
    }
}
