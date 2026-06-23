package li.cil.oc.common;

import li.cil.oc.api.detail.ManualAPI;
import li.cil.oc.api.manual.ContentProvider;
import li.cil.oc.api.manual.ImageProvider;
import li.cil.oc.api.manual.ImageRenderer;
import li.cil.oc.api.manual.PathProvider;
import li.cil.oc.api.manual.TabIconRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

public final class ManualRegistry implements ManualAPI {
    private static final String LANGUAGE_KEY = "%LANGUAGE%";
    private static final String FALLBACK_LANGUAGE = "en_us";
    private static final String DEFAULT_PATH = LANGUAGE_KEY + "/index.md";
    private static final String REDIRECT_PREFIX = "#redirect ";

    private final List<Tab> tabs = new ArrayList<>();
    private final List<PathProvider> pathProviders = new ArrayList<>();
    private final List<ContentProvider> contentProviders = new ArrayList<>();
    private final List<ImageProviderEntry> imageProviders = new ArrayList<>();
    private final ArrayDeque<History> history = new ArrayDeque<>();
    private boolean opened;
    private boolean reset;
    private String lastNavigationPath;
    private Supplier<String> languageSupplier;

    public ManualRegistry() {
        this(() -> FALLBACK_LANGUAGE);
    }

    ManualRegistry(final Supplier<String> languageSupplier) {
        this.languageSupplier = Objects.requireNonNull(languageSupplier);
        resetHistory();
    }

    public void setLanguageSupplier(final Supplier<String> languageSupplier) {
        this.languageSupplier = Objects.requireNonNull(languageSupplier);
    }

    @Override
    public void addTab(final TabIconRenderer renderer, final String tooltip, final String path) {
        tabs.add(new Tab(renderer, tooltip, path));
    }

    @Override
    public void addProvider(final PathProvider provider) {
        pathProviders.add(provider);
    }

    @Override
    public void addProvider(final ContentProvider provider) {
        contentProviders.add(provider);
    }

    @Override
    public void addProvider(final String prefix, final ImageProvider provider) {
        final String normalizedPrefix = prefix == null || prefix.isEmpty() ? "" : prefix + ":";
        imageProviders.add(new ImageProviderEntry(normalizedPrefix, provider));
    }

    @Override
    public String pathFor(final ItemStack stack) {
        for (final PathProvider provider : pathProviders) {
            final String path = provider.pathFor(stack);
            if (path != null) {
                return path;
            }
        }
        return null;
    }

    @Override
    public String pathFor(final Level world, final BlockPos pos) {
        for (final PathProvider provider : pathProviders) {
            final String path = provider.pathFor(world, pos);
            if (path != null) {
                return path;
            }
        }
        return null;
    }

    @Override
    public Iterable<String> contentFor(final String path) {
        final String cleanPath = simplifyPath(path);
        final Iterable<String> content = contentForWithRedirects(cleanPath.replace(LANGUAGE_KEY, currentLanguage()), new ArrayList<>());
        if (content != null) {
            return content;
        }
        return contentForWithRedirects(cleanPath.replace(LANGUAGE_KEY, FALLBACK_LANGUAGE), new ArrayList<>());
    }

    private String currentLanguage() {
        try {
            final String language = languageSupplier.get();
            if (language != null && !language.isBlank()) {
                return language;
            }
        } catch (final RuntimeException ignored) {
        }
        return FALLBACK_LANGUAGE;
    }

    private Iterable<String> contentForWithRedirects(final String path, final List<String> seen) {
        if (seen.contains(path)) {
            final List<String> loop = new ArrayList<>();
            loop.add("Redirection loop: ");
            loop.addAll(seen);
            loop.add(path);
            return loop;
        }
        final Iterable<String> content = doContentLookup(path);
        if (content == null) {
            return null;
        }
        final String firstLine = firstLine(content);
        if (firstLine != null && firstLine.toLowerCase(Locale.ROOT).startsWith(REDIRECT_PREFIX)) {
            final List<String> nextSeen = new ArrayList<>(seen);
            nextSeen.add(path);
            return contentForWithRedirects(makeRelative(firstLine.substring(REDIRECT_PREFIX.length()), path), nextSeen);
        }
        return content;
    }

    private Iterable<String> doContentLookup(final String path) {
        for (final ContentProvider provider : contentProviders) {
            final Iterable<String> content = provider.getContent(path);
            if (content != null) {
                return content;
            }
        }
        return null;
    }

    private static String firstLine(final Iterable<String> content) {
        final var iterator = content.iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    private static String makeRelative(final String path, final String base) {
        if (path.startsWith("/")) {
            return simplifyPath(path);
        }
        final int splitAt = base.lastIndexOf('/');
        return simplifyPath(splitAt >= 0 ? base.substring(0, splitAt) + "/" + path : path);
    }

    private static String simplifyPath(final String path) {
        final boolean absolute = path.startsWith("/");
        final ArrayDeque<String> parts = new ArrayDeque<>();
        for (final String part : path.replace('\\', '/').split("/+")) {
            if (part.isEmpty() || ".".equals(part)) {
                continue;
            }
            if ("..".equals(part)) {
                if (!parts.isEmpty() && !"..".equals(parts.peekLast())) {
                    parts.removeLast();
                } else if (!absolute) {
                    parts.addLast(part);
                }
            } else {
                parts.addLast(part);
            }
        }
        final String simplified = String.join("/", parts);
        return absolute ? "/" + simplified : simplified;
    }

    @Override
    public ImageRenderer imageFor(final String path) {
        for (int index = imageProviders.size() - 1; index >= 0; index--) {
            final ImageProviderEntry entry = imageProviders.get(index);
            if (path.startsWith(entry.prefix())) {
                final ImageRenderer image = entry.provider().getImage(path.substring(entry.prefix().length()));
                if (image != null) {
                    return image;
                }
            }
        }
        return null;
    }

    @Override
    public void openFor(final Player player) {
        opened = true;
    }

    @Override
    public void reset() {
        reset = true;
        resetHistory();
    }

    @Override
    public void navigate(final String path) {
        lastNavigationPath = path;
        if (!Objects.equals(currentPath(), path)) {
            history.push(new History(path, 0));
        }
    }

    int tabCount() {
        return tabs.size();
    }

    int pathProviderCount() {
        return pathProviders.size();
    }

    int contentProviderCount() {
        return contentProviders.size();
    }

    int imageProviderCount() {
        return imageProviders.size();
    }

    boolean wasOpened() {
        return opened;
    }

    boolean wasReset() {
        return reset;
    }

    String lastNavigationPath() {
        return lastNavigationPath;
    }

    String currentPath() {
        return history.peek().path();
    }

    int historySize() {
        return history.size();
    }

    private void resetHistory() {
        history.clear();
        history.push(new History(DEFAULT_PATH, 0));
    }

    private record Tab(TabIconRenderer renderer, String tooltip, String path) {
    }

    private record ImageProviderEntry(String prefix, ImageProvider provider) {
    }

    private record History(String path, int offset) {
    }
}
