package li.cil.oc.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ManualResourceTest {
    private static final Path DOC_ROOT = Path.of("src/main/resources/assets/neoopencomputers/doc");
    private static final Path ASSET_ROOT = Path.of("src/main/resources/assets/neoopencomputers");
    private static final Path TEXTURE_ROOT = Path.of("src/main/resources/assets/neoopencomputers/textures/gui");
    private static final Path LANG_ROOT = Path.of("src/main/resources/assets/neoopencomputers/lang");
    private static final Pattern INTERNAL_MARKDOWN_LINK = Pattern.compile("(?<!!)\\[[^\\]]+]\\(([^)]+)\\)");
    private static final Pattern MARKDOWN_IMAGE = Pattern.compile("!\\[[^\\]]*]\\(([^)]+)\\)");
    private static final Pattern URL_SCHEME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*:");

    @Test
    void bundledManualResourcesIncludeUpstreamEnglishPagesAndImages() {
        assertTrue(Files.exists(DOC_ROOT.resolve("en_us/index.md")));
        assertTrue(Files.exists(DOC_ROOT.resolve("en_us/item/manual.md")));
        assertTrue(Files.exists(DOC_ROOT.resolve("en_us/general/quickstart.md")));
        assertTrue(Files.exists(DOC_ROOT.resolve("img/manual.png")));
        assertTrue(Files.exists(DOC_ROOT.resolve("img/configuration_case1.png")));
        assertTrue(Files.exists(TEXTURE_ROOT.resolve("manual_home.png")));
    }

    @Test
    void bundledManualGuiTexturesMatchUpstreamDimensions() throws IOException {
        assertArrayEquals(new int[]{256, 192}, pngDimensions(TEXTURE_ROOT.resolve("manual.png")));
        assertArrayEquals(new int[]{23, 52}, pngDimensions(TEXTURE_ROOT.resolve("manual_tab.png")));
        assertArrayEquals(new int[]{6, 26}, pngDimensions(TEXTURE_ROOT.resolve("button_scroll.png")));
    }

    @Test
    void bundledManualMarkdownUsesNeoOpenComputersResourceNamespace() throws Exception {
        try (Stream<Path> files = Files.walk(DOC_ROOT)) {
            assertTrue(files
                .filter(path -> path.getFileName().toString().endsWith(".md"))
                .map(path -> {
                    try {
                        return Files.readString(path);
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                })
                .noneMatch(content -> content.contains("(opencomputers:")));
        }
    }

    @Test
    void bundledManualMarkdownUsesCommunityIssueTracker() throws Exception {
        try (Stream<Path> files = Files.walk(DOC_ROOT)) {
            assertTrue(files
                .filter(path -> path.getFileName().toString().endsWith(".md"))
                .map(path -> {
                    try {
                        return Files.readString(path);
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                })
                .noneMatch(content -> content.contains("github.com/MightyPirates/OpenComputers/issues")));
        }
    }

    @Test
    void bundledManualMarkdownResourcePathsAreLowercase() throws Exception {
        try (Stream<Path> files = Files.walk(DOC_ROOT)) {
            assertTrue(files
                .filter(path -> path.getFileName().toString().endsWith(".md"))
                .map(DOC_ROOT::relativize)
                .map(path -> path.toString().replace('\\', '/'))
                .noneMatch(path -> !path.equals(path.toLowerCase(java.util.Locale.ROOT))));
        }
    }

    @Test
    void bundledManualMarkdownInternalLinksResolveToPages() throws Exception {
        final Set<String> pages = new HashSet<>();
        final Set<String> locales = new HashSet<>();
        try (Stream<Path> files = Files.list(DOC_ROOT)) {
            files.filter(Files::isDirectory)
                .map(path -> path.getFileName().toString().toLowerCase(Locale.ROOT))
                .filter(locale -> !"img".equals(locale))
                .forEach(locales::add);
        }
        try (Stream<Path> files = Files.walk(DOC_ROOT)) {
            files.filter(path -> path.getFileName().toString().endsWith(".md"))
                .map(DOC_ROOT::relativize)
                .map(path -> path.toString().replace('\\', '/').toLowerCase(Locale.ROOT))
                .forEach(pages::add);
        }

        final List<String> brokenLinks = new ArrayList<>();
        try (Stream<Path> files = Files.walk(DOC_ROOT)) {
            for (final Path file : files.filter(path -> path.getFileName().toString().endsWith(".md")).toList()) {
                final Path relativeFile = DOC_ROOT.relativize(file);
                final String content = Files.readString(file);
                final var matcher = INTERNAL_MARKDOWN_LINK.matcher(content);
                while (matcher.find()) {
                    final String target = matcher.group(1);
                    final String resolved = resolveManualLink(relativeFile, target);
                    if (resolved != null && !manualPageExists(resolved, pages, locales)) {
                        brokenLinks.add(relativeFile.toString().replace('\\', '/') + " -> " + target + " -> " + resolved);
                    }
                }
            }
        }

        assertTrue(brokenLinks.isEmpty(), () -> "Broken manual links:\n" + String.join("\n", brokenLinks));
    }

    @Test
    void bundledManualMarkdownImageReferencesResolveToAssets() throws Exception {
        final List<String> brokenImages = new ArrayList<>();
        try (Stream<Path> files = Files.walk(DOC_ROOT)) {
            for (final Path file : files.filter(path -> path.getFileName().toString().endsWith(".md")).toList()) {
                final Path relativeFile = DOC_ROOT.relativize(file);
                final String content = Files.readString(file);
                final var matcher = MARKDOWN_IMAGE.matcher(content);
                while (matcher.find()) {
                    final String target = matcher.group(1);
                    final Path resolved = resolveManualImage(relativeFile, target);
                    if (resolved != null && !Files.exists(resolved)) {
                        brokenImages.add(relativeFile.toString().replace('\\', '/') + " -> " + target + " -> " + resolved);
                    }
                }
            }
        }

        assertTrue(brokenImages.isEmpty(), () -> "Broken manual images:\n" + String.join("\n", brokenImages));
    }

    @Test
    void bundledLanguageIncludesManualTooltipKeys() throws IOException {
        final String english = Files.readString(LANG_ROOT.resolve("en_us.json"));

        assertTrue(english.contains("\"oc:gui.Manual.Home\""));
        assertTrue(english.contains("\"oc:gui.Manual.Blocks\""));
        assertTrue(english.contains("\"oc:gui.Manual.Items\""));
        assertTrue(english.contains("\"oc:gui.Manual.Warning.ImageMissing\""));
        assertTrue(english.contains("\"oc:gui.Manual.Warning.ItemMissing\""));
        assertTrue(english.contains("\"oc:gui.Manual.Warning.BlockMissing\""));
        assertTrue(english.contains("\"oc:gui.Manual.Warning.OreDictMissing\""));
        assertTrue(english.contains("\"oc:gui.Chat.WarningLink\""));
    }

    private static int[] pngDimensions(final Path path) throws IOException {
        final byte[] bytes = Files.readAllBytes(path);
        return new int[]{
            readBigEndianInt(bytes, 16),
            readBigEndianInt(bytes, 20)
        };
    }

    private static int readBigEndianInt(final byte[] bytes, final int offset) {
        return ((bytes[offset] & 0xFF) << 24)
            | ((bytes[offset + 1] & 0xFF) << 16)
            | ((bytes[offset + 2] & 0xFF) << 8)
            | (bytes[offset + 3] & 0xFF);
    }

    private static String resolveManualLink(final Path source, final String target) {
        final String path = target.split("#", 2)[0].trim();
        if (path.isEmpty() || URL_SCHEME.matcher(path).find()) {
            return null;
        }
        final Path relative = path.startsWith("/")
            ? Path.of(path.substring(1))
            : source.getParent().resolve(path);
        return relative.normalize().toString().replace('\\', '/').toLowerCase(Locale.ROOT);
    }

    private static boolean manualPageExists(final String path, final Set<String> pages, final Set<String> locales) {
        if (pages.contains(path)) {
            return true;
        }
        final int separator = path.indexOf('/');
        if (separator > 0 && locales.contains(path.substring(0, separator))) {
            return pages.contains("en_us/" + path.substring(separator + 1));
        }
        return false;
    }

    private static Path resolveManualImage(final Path source, final String target) {
        final String path = target.split("#", 2)[0].trim();
        if (path.isEmpty()
            || path.startsWith("item:")
            || path.startsWith("block:")
            || path.startsWith("oredict:")
            || (URL_SCHEME.matcher(path).find() && !path.startsWith("neoopencomputers:"))) {
            return null;
        }
        if (path.startsWith("neoopencomputers:")) {
            final String resourcePath = path.substring("neoopencomputers:".length()).replaceFirst("^/+", "");
            return ASSET_ROOT.resolve(resourcePath.toLowerCase(Locale.ROOT));
        }
        if (URL_SCHEME.matcher(path).find()) {
            return null;
        }
        final Path relative = path.startsWith("/")
            ? Path.of(path.substring(1))
            : source.getParent().resolve(path);
        return DOC_ROOT.resolve(relative.normalize().toString().toLowerCase(Locale.ROOT));
    }
}
