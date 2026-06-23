package li.cil.oc.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ManualResourceTest {
    private static final Path DOC_ROOT = Path.of("src/main/resources/assets/neoopencomputers/doc");
    private static final Path TEXTURE_ROOT = Path.of("src/main/resources/assets/neoopencomputers/textures/gui");
    private static final Path LANG_ROOT = Path.of("src/main/resources/assets/neoopencomputers/lang");

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
}
