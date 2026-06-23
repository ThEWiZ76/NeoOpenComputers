package li.cil.oc.client;

import li.cil.oc.common.ManualRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;

public class ManualScreen extends Screen {
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
    private static final int DOCUMENT_POS_X = 8;
    private static final int DOCUMENT_POS_Y = 8;
    private static final int SCROLL_STEP = LINE_HEIGHT * 3;

    private final ManualRegistry registry;
    private ManualDocument document = ManualDocument.parse(List.of());
    private int scrollOffset;

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

    public static List<LayoutEntry> layout(final ManualDocument document, final int maxWidth) {
        int y = 0;
        final var entries = new java.util.ArrayList<LayoutEntry>();
        for (final ManualDocument.Segment segment : document.segments()) {
            if (segment instanceof final ManualDocument.ImageSegment image) {
                y += entries.isEmpty() ? 2 : SEGMENT_PADDING;
                final int width = Math.min(maxWidth, image.renderer().getWidth());
                final int height = image.renderer().getHeight();
                entries.add(new LayoutEntry(segment, (maxWidth - width) / 2, y, width, height));
                y += height;
            } else if (segment instanceof final ManualDocument.TextSegment text && !text.text().isEmpty()) {
                entries.add(new LayoutEntry(segment, 0, y, maxWidth, LINE_HEIGHT));
                y += LINE_HEIGHT;
            }
        }
        return List.copyOf(entries);
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

    public static ManualDocument.ImageSegment interactiveImageAt(
        final ManualDocument document,
        final int left,
        final int top,
        final int mouseX,
        final int mouseY,
        final int scrollOffset
    ) {
        for (final LayoutEntry entry : layout(document, DOCUMENT_MAX_WIDTH)) {
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

    @Override
    protected void init() {
        refreshPage();
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        final int left = (width - WINDOW_WIDTH) / 2;
        final int top = (height - WINDOW_HEIGHT) / 2;
        graphics.fill(left, top, left + WINDOW_WIDTH, top + WINDOW_HEIGHT, 0xFF2E3440);
        renderTabs(graphics, left, top);
        graphics.fill(left + DOCUMENT_POS_X, top + DOCUMENT_POS_Y, left + DOCUMENT_POS_X + DOCUMENT_MAX_WIDTH, top + DOCUMENT_POS_Y + DOCUMENT_MAX_HEIGHT, 0xFF3B4252);
        renderDocument(graphics, left + DOCUMENT_POS_X, top + DOCUMENT_POS_Y, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderTabs(final GuiGraphics graphics, final int left, final int top) {
        final List<ManualRegistry.ManualTab> tabs = registry.tabs();
        for (int index = 0; index < Math.min(tabs.size(), MAX_TABS_PER_SIDE); index++) {
            final int x = left + TAB_POS_X;
            final int y = top + TAB_POS_Y + index * (TAB_HEIGHT - 1);
            graphics.fill(x, y, x + TAB_WIDTH, y + TAB_HEIGHT, 0xFF434C5E);
            graphics.pose().pushPose();
            graphics.pose().translate(x + 4, y + 5, 0);
            tabs.get(index).renderer().render();
            graphics.pose().popPose();
        }
    }

    private void renderDocument(final GuiGraphics graphics, final int left, final int top, final int mouseX, final int mouseY) {
        for (final LayoutEntry entry : layout(document, DOCUMENT_MAX_WIDTH)) {
            final int y = top + entry.y() - scrollOffset;
            if (entry.segment() instanceof final ManualDocument.TextSegment text) {
                graphics.drawString(font, text.text(), left + entry.x(), y, 0xFFE5E9F0, false);
            } else if (entry.segment() instanceof final ManualDocument.ImageSegment image) {
                graphics.pose().pushPose();
                graphics.pose().translate(left + entry.x(), y, 0);
                image.renderAt(left + entry.x(), y, mouseX, mouseY);
                graphics.pose().popPose();
            }
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
            final int tabIndex = tabIndexAt((int) mouseX - left, (int) mouseY - top, registry.tabs().size());
            if (tabIndex >= 0) {
                registry.navigate(registry.tabs().get(tabIndex).path());
                scrollOffset = 0;
                refreshPage();
                return true;
            }
            final ManualDocument.ImageSegment image = interactiveImageAt(
                document,
                left + DOCUMENT_POS_X,
                top + DOCUMENT_POS_Y,
                (int) mouseX,
                (int) mouseY,
                scrollOffset);
            if (image != null && image.onMouseClick((int) mouseX, (int) mouseY)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public record LayoutEntry(ManualDocument.Segment segment, int x, int y, int width, int height) {
    }
}
