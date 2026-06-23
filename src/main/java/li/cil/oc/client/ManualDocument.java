package li.cil.oc.client;

import li.cil.oc.api.API;
import li.cil.oc.api.manual.ImageRenderer;
import li.cil.oc.api.manual.InteractiveImageRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ManualDocument {
    private static final Pattern IMAGE_OR_LINK_PATTERN = Pattern.compile("(!)?\\[([^\\[]*)\\]\\(([^\\)]+)\\)");

    private final List<Segment> segments;

    private ManualDocument(final List<Segment> segments) {
        this.segments = List.copyOf(segments);
    }

    public static ManualDocument parse(final Iterable<String> lines) {
        return parse(lines, href -> API.manual == null ? null : API.manual.imageFor(href));
    }

    public static ManualDocument parse(final Iterable<String> lines, final Function<String, ImageRenderer> imageResolver) {
        Objects.requireNonNull(lines);
        Objects.requireNonNull(imageResolver);

        final List<Segment> segments = new ArrayList<>();
        final var iterator = lines.iterator();
        while (iterator.hasNext()) {
            final String line = iterator.next();
            appendLine(segments, trimTrailing(line == null ? "" : line), imageResolver);
            if (iterator.hasNext()) {
                segments.add(new LineBreakSegment());
            }
        }
        return new ManualDocument(segments);
    }

    public List<Segment> segments() {
        return segments;
    }

    private static void appendLine(final List<Segment> segments, final String line, final Function<String, ImageRenderer> imageResolver) {
        final Matcher matcher = IMAGE_OR_LINK_PATTERN.matcher(line);
        int textStart = 0;
        while (matcher.find()) {
            if (matcher.start() > textStart) {
                segments.add(new TextSegment(line.substring(textStart, matcher.start())));
            }
            textStart = matcher.end();
            if (matcher.group(1) != null) {
                segments.add(imageSegment(matcher.group(2), matcher.group(3), imageResolver));
            } else {
                segments.add(new LinkSegment(matcher.group(2), matcher.group(3)));
            }
        }
        if (textStart == 0) {
            segments.add(new TextSegment(line));
        } else if (textStart < line.length()) {
            segments.add(new TextSegment(line.substring(textStart)));
        }
    }

    private static Segment imageSegment(final String title, final String href, final Function<String, ImageRenderer> imageResolver) {
        try {
            final ImageRenderer renderer = imageResolver.apply(href);
            if (renderer != null) {
                return new ImageSegment(title, href, renderer);
            }
            return new TextSegment("No renderer found for: " + href);
        } catch (final Throwable throwable) {
            return new TextSegment(Objects.toString(throwable, "Unknown error."));
        }
    }

    private static String trimTrailing(final String value) {
        int end = value.length();
        while (end > 0 && Character.isWhitespace(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end);
    }

    public interface Segment {
    }

    public record TextSegment(String text) implements Segment {
    }

    public record LinkSegment(String text, String href) implements Segment {
        public String tooltip() {
            return href;
        }
    }

    public record LineBreakSegment() implements Segment {
    }

    public static final class ImageSegment implements Segment {
        private final String title;
        private final String href;
        private final ImageRenderer renderer;
        private int lastX;
        private int lastY;

        private ImageSegment(final String title, final String href, final ImageRenderer renderer) {
            this.title = title;
            this.href = href;
            this.renderer = renderer;
        }

        public String title() {
            return title;
        }

        public String href() {
            return href;
        }

        public ImageRenderer renderer() {
            return renderer;
        }

        public String tooltip() {
            if (renderer instanceof final InteractiveImageRenderer interactive) {
                return interactive.getTooltip(title);
            }
            return title;
        }

        public void renderAt(final int x, final int y, final int mouseX, final int mouseY) {
            lastX = x;
            lastY = y;
            renderer.render(mouseX - x, mouseY - y);
        }

        public boolean onMouseClick(final int mouseX, final int mouseY) {
            if (renderer instanceof final InteractiveImageRenderer interactive) {
                return interactive.onMouseClick(mouseX - lastX, mouseY - lastY);
            }
            return false;
        }
    }
}
