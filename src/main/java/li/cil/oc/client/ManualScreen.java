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

    private final ManualRegistry registry;
    private ManualDocument document = ManualDocument.parse(List.of());

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
        graphics.fill(left + 8, top + 8, left + 8 + DOCUMENT_MAX_WIDTH, top + 8 + DOCUMENT_MAX_HEIGHT, 0xFF3B4252);
        renderDocument(graphics, left + 8, top + 8, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderDocument(final GuiGraphics graphics, final int left, final int top, final int mouseX, final int mouseY) {
        for (final LayoutEntry entry : layout(document, DOCUMENT_MAX_WIDTH)) {
            if (entry.segment() instanceof final ManualDocument.TextSegment text) {
                graphics.drawString(font, text.text(), left + entry.x(), top + entry.y(), 0xFFE5E9F0, false);
            } else if (entry.segment() instanceof final ManualDocument.ImageSegment image) {
                graphics.pose().pushPose();
                graphics.pose().translate(left + entry.x(), top + entry.y(), 0);
                image.renderAt(left + entry.x(), top + entry.y(), mouseX, mouseY);
                graphics.pose().popPose();
            }
        }
    }

    public record LayoutEntry(ManualDocument.Segment segment, int x, int y, int width, int height) {
    }
}
