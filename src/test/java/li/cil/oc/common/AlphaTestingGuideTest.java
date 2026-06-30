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
        assertTrue(guide.contains("NeoForge 21.1.234"), "Guide must name the current NeoForge version");
        assertTrue(guide.contains("build/libs/neoopencomputers-0.1.0.jar"),
            "Guide must name the alpha jar path");
        assertTrue(guide.contains("Do not install `neoopencomputers-0.1.0-thin.jar`"),
            "Guide must warn testers away from the developer thin jar");
        assertTrue(guide.contains("GitHub Actions stay disabled"),
            "Guide must keep CI disabled before release readiness");
        assertTrue(guide.contains("Robots, drones, and block microcontrollers are not available"),
            "Guide must document known unavailable alpha devices");
        assertTrue(guide.contains("Computer and server boot failures show a player-visible last-error message"),
            "Guide must tell alpha testers to verify player-facing boot failure feedback");
        assertTrue(guide.contains("Screen output and keyboard input survive save/reload"),
            "Guide must tell alpha testers to verify screen and keyboard persistence");
        assertTrue(guide.contains("MCP terminal-open evidence alone does not verify screen world rendering"),
            "Guide must not imply MCP terminal-open evidence verifies screen visuals");
        assertTrue(guide.contains("Computer case hard-disk data survives save/reload"),
            "Guide must tell alpha testers to verify computer case hard-disk persistence");
        assertTrue(guide.contains("Power and charging smoke passes"),
            "Guide must tell alpha testers to verify power and charging before alpha handoff");
        assertTrue(guide.contains("Texture picker reports usable atlas texture names"),
            "Guide must tell alpha testers to verify texture picker atlas names before alpha handoff");
        assertTrue(guide.contains("https://github.com/ThEWiZ76/NeoOpenComputers/issues"),
            "Guide must route findings to the community issue tracker");
    }
}
