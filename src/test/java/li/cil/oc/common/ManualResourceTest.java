package li.cil.oc.common;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ManualResourceTest {
    private static final Path DOC_ROOT = Path.of("src/main/resources/assets/neoopencomputers/doc");
    private static final Path TEXTURE_ROOT = Path.of("src/main/resources/assets/neoopencomputers/textures/gui");

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
}
