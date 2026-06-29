package li.cil.oc.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class AlphaTestingGuideTest {
    @Test
    void alphaTestingGuideDocumentsFirstAlphaPackageChecklist() throws IOException {
        final Path guidePath = Path.of("ALPHA_TESTING.md");
        final String readme = Files.readString(Path.of("README.md"));

        assertTrue(Files.exists(guidePath), "First-alpha testing guide must exist");
        assertTrue(readme.contains("[First Alpha Testing Guide](ALPHA_TESTING.md)"),
            "README must link the first-alpha testing guide");

        final String guide = Files.readString(guidePath);
        assertTrue(guide.contains("Minecraft 1.21.1"), "Guide must name the target Minecraft version");
        assertTrue(guide.contains("NeoForge 21.1.233"), "Guide must name the current NeoForge version");
        assertTrue(guide.contains("build/libs/neoopencomputers-0.1.0.jar"),
            "Guide must name the alpha jar path");
        assertTrue(guide.contains("GitHub Actions stay disabled"),
            "Guide must keep CI disabled before release readiness");
        assertTrue(guide.contains("Robots, drones, and block microcontrollers are not available"),
            "Guide must document known unavailable alpha devices");
        assertTrue(guide.contains("https://github.com/ThEWiZ76/NeoOpenComputers/issues"),
            "Guide must route findings to the community issue tracker");
    }
}
