package li.cil.oc.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReadmeSmokeTest {
    private static final Pattern SHORT_COMMIT_HASH = Pattern.compile("`[0-9a-f]{9}(?:\\s|`)");

    @Test
    void readmeDocumentsCurrentSmokeTestState() throws IOException {
        final String readme = Files.readString(Path.of("README.md"));

        assertTrue(readme.contains("First Technical Smoke Test"), "README must document first smoke testing");
        assertTrue(readme.contains("Current pushed first-smoke base"), "README must name the smoke-test base");
        assertTrue(readme.contains("develop"), "README must direct testers to develop");
        assertTrue(readme.contains("428/428"), "README must include current GameTest evidence");
        assertTrue(readme.contains("Disk-drive floppy data survives save/reload"), "README must include disk-drive persistence smoke item");
        assertTrue(readme.contains("Computer case hard-disk data survives save/reload"),
            "README must include computer case hard-disk persistence smoke item");
        assertTrue(readme.contains("Terminal item key input, paste, mouse click/drag/release, and scroll reach the bound computer"),
            "README must include terminal item input smoke item");
        assertTrue(readme.contains("Tier 2 and tier 3 screens open their terminal GUI after a fresh client restart"),
            "README must include current tiered screen GUI smoke evidence");
        assertTrue(readme.contains("Restart the Minecraft client after replacing the mod jar"),
            "README must warn testers to restart after jar replacement");
        assertTrue(readme.contains("screenshots"), "README must tell testers to save screenshots");
        assertTrue(readme.contains("GitHub Actions are intentionally disabled"), "README must warn Actions remain disabled");
        assertTrue(readme.contains("Robots, drones, and block microcontrollers are not available in this alpha build"),
            "README must state unavailable alpha devices");
        assertTrue(readme.contains("Do not install `neoopencomputers-0.1.0-thin.jar`"),
            "README must warn testers away from the developer thin jar");
    }

    @Test
    void readmeDoesNotDocumentIgnoredLocalScripts() throws IOException {
        final String readme = Files.readString(Path.of("README.md"));

        assertTrue(!readme.contains(".\\scripts\\"), "Public README must not reference ignored local helper scripts");
    }

    @Test
    void buildScriptSupportsQuickPlayWorldProperty() throws IOException {
        final String buildScript = Files.readString(Path.of("build.gradle"));

        assertTrue(buildScript.contains("neoopencomputers.quickPlayWorld"),
            "Client run must expose a property for MCP world-smoke quick-play");
        assertTrue(buildScript.contains("argument '--quickPlaySingleplayer'"),
            "Client run must append Minecraft quick-play singleplayer argument");
    }

    @Test
    void readmeDoesNotAssertAgainstMovingCommitHash() throws IOException {
        final String readme = Files.readString(Path.of("README.md"));

        assertTrue(!SHORT_COMMIT_HASH.matcher(readme).find(),
            "README smoke-test base should not hardcode a moving commit hash");
    }

    @Test
    void readmeNoLongerDescribesOnlyInitialScaffold() throws IOException {
        final String readme = Files.readString(Path.of("README.md"));

        assertTrue(!readme.contains("clean NeoForge 1.21.1 scaffold with the first standalone OpenComputers API contracts"),
            "README still describes the initial scaffold instead of current port state");
    }
}
