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
    private static final Pattern HEADER_PATTERN = Pattern.compile("^(#+)\\s(.*)");
    private static final Pattern CODE_PATTERN = Pattern.compile("`(.*?)`");
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[([^\\[]*)\\]\\(([^\\)]+)\\)");
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\[]+)\\]\\(([^\\)]+)\\)");
    private static final Pattern BOLD_PATTERN = Pattern.compile("(\\*\\*|__)(\\S.*?\\S|$)\\1");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("(\\*|_)(\\S.*?\\S|$)\\1");
    private static final Pattern STRIKETHROUGH_PATTERN = Pattern.compile("~~(\\S.*?\\S|$)~~");

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
        final Matcher header = HEADER_PATTERN.matcher(line);
        if (header.matches()) {
            appendInlineSegments(segments, header.group(2), imageResolver, Integer.min(header.group(1).length(), 6));
        } else {
            appendInlineSegments(segments, line, imageResolver, 0);
        }
    }

    private static void appendInlineSegments(
        final List<Segment> segments,
        final String text,
        final Function<String, ImageRenderer> imageResolver,
        final int headerLevel
    ) {
        int index = 0;
        while (index < text.length()) {
            final TokenMatch token = nextToken(text, index);
            if (token == null) {
                addTextSegment(segments, text.substring(index), headerLevel);
                return;
            }
            if (token.start() > index) {
                addTextSegment(segments, text.substring(index, token.start()), headerLevel);
            }
            token.addTo(segments, imageResolver, headerLevel);
            index = token.end();
        }
        if (text.isEmpty()) {
            addTextSegment(segments, text, headerLevel);
        }
    }

    private static TokenMatch nextToken(final String text, final int start) {
        TokenMatch best = null;
        for (final TokenType type : TokenType.values()) {
            final Matcher matcher = type.pattern.matcher(text);
            if (matcher.find(start)) {
                final TokenMatch candidate = new TokenMatch(type, matcher.start(), matcher.end(), matcher);
                if (best == null || candidate.start < best.start || (candidate.start == best.start && type.ordinal() < best.type.ordinal())) {
                    best = candidate;
                }
            }
        }
        return best;
    }

    private static void addTextSegment(final List<Segment> segments, final String text, final int headerLevel) {
        if (headerLevel > 0) {
            segments.add(new HeaderSegment(text, headerLevel));
        } else {
            segments.add(new TextSegment(text));
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

    public interface TextualSegment extends Segment {
        String text();

        default int headerLevel() {
            return 0;
        }

        default boolean bold() {
            return false;
        }

        default boolean italic() {
            return false;
        }

        default boolean code() {
            return false;
        }

        default boolean strikethrough() {
            return false;
        }
    }

    public record TextSegment(String text) implements TextualSegment {
    }

    public record LinkSegment(String text, String href, int headerLevel) implements TextualSegment {
        public LinkSegment(final String text, final String href) {
            this(text, href, 0);
        }

        public String tooltip() {
            return href;
        }
    }

    public record HeaderSegment(String text, int level) implements TextualSegment {
        @Override
        public int headerLevel() {
            return level;
        }
    }

    public record BoldSegment(String text, int headerLevel) implements TextualSegment {
        public BoldSegment(final String text) {
            this(text, 0);
        }

        @Override
        public boolean bold() {
            return true;
        }
    }

    public record ItalicSegment(String text, int headerLevel) implements TextualSegment {
        public ItalicSegment(final String text) {
            this(text, 0);
        }

        @Override
        public boolean italic() {
            return true;
        }
    }

    public record CodeSegment(String text, int headerLevel) implements TextualSegment {
        public CodeSegment(final String text) {
            this(text, 0);
        }

        @Override
        public boolean code() {
            return true;
        }
    }

    public record StrikethroughSegment(String text, int headerLevel) implements TextualSegment {
        public StrikethroughSegment(final String text) {
            this(text, 0);
        }

        @Override
        public boolean strikethrough() {
            return true;
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

    private enum TokenType {
        CODE(CODE_PATTERN),
        IMAGE(IMAGE_PATTERN),
        LINK(LINK_PATTERN),
        BOLD(BOLD_PATTERN),
        ITALIC(ITALIC_PATTERN),
        STRIKETHROUGH(STRIKETHROUGH_PATTERN);

        private final Pattern pattern;

        TokenType(final Pattern pattern) {
            this.pattern = pattern;
        }
    }

    private record TokenMatch(TokenType type, int start, int end, Matcher matcher) {
        private void addTo(final List<Segment> segments, final Function<String, ImageRenderer> imageResolver, final int headerLevel) {
            switch (type) {
                case CODE -> segments.add(new CodeSegment(matcher.group(1), headerLevel));
                case IMAGE -> segments.add(imageSegment(matcher.group(1), matcher.group(2), imageResolver));
                case LINK -> segments.add(new LinkSegment(matcher.group(1), matcher.group(2), headerLevel));
                case BOLD -> segments.add(new BoldSegment(matcher.group(2), headerLevel));
                case ITALIC -> segments.add(new ItalicSegment(matcher.group(2), headerLevel));
                case STRIKETHROUGH -> segments.add(new StrikethroughSegment(matcher.group(1), headerLevel));
            }
        }
    }
}
