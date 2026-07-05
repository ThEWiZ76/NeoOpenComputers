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
        assertTrue(readme.contains("439/439"), "README must include current GameTest evidence");
        assertTrue(readme.contains("Disk-drive floppy data survives save/reload"), "README must include disk-drive persistence smoke item");
        assertTrue(readme.contains("Computer case hard-disk data survives save/reload"),
            "README must include computer case hard-disk persistence smoke item");
        assertTrue(readme.contains("Power and charging smoke passes"),
            "README must include power and charging smoke item");
        assertTrue(readme.contains("Terminal item key input, paste, mouse click/drag/release, and scroll reach the bound computer"),
            "README must include terminal item input smoke item");
        assertTrue(readme.contains("MCP evidence shows tier 2 and tier 3 screens open their terminal GUI after a fresh client restart"),
            "README must scope current tiered screen GUI evidence to MCP");
        assertTrue(readme.contains("World-render screenshot proof for tier 2 and tier 3 screens is still pending"),
            "README must not imply tiered screen world rendering is visually verified");
        assertTrue(readme.contains("Restart the Minecraft client after replacing the mod jar"),
            "README must warn testers to restart after jar replacement");
        assertTrue(readme.contains("Verify the installed jar hash matches the built jar hash after copying"),
            "README must require post-copy installed jar hash proof so stale profile jars are caught");
        assertTrue(readme.contains("screenshots"), "README must tell testers to save screenshots");
        assertTrue(readme.contains("Jar SHA256"),
            "README finding instructions must ask for the exact jar hash");
        assertTrue(readme.contains("current jar install timestamp"),
            "README finding instructions must ask for jar install freshness");
        assertTrue(readme.contains("Crash report timestamp"),
            "README finding instructions must ask for crash-report freshness");
        assertTrue(readme.contains("GitHub Actions are intentionally disabled"), "README must warn Actions remain disabled");
        assertTrue(readme.contains("Robots, drones, and microcontrollers have automated GameTest coverage"),
            "README must keep agent-device alpha smoke status clear");
        assertTrue(readme.contains("NeoOpenComputers-owned missing model, missing texture, or mod loading errors"),
            "README must scope log-error smoke failures to NeoOpenComputers-owned resources");
        assertTrue(readme.contains("Do not install `neoopencomputers-0.1.0-thin.jar`"),
            "README must warn testers away from the developer thin jar");
    }

    @Test
    void readmeDoesNotDocumentIgnoredLocalScripts() throws IOException {
        final String readme = Files.readString(Path.of("README.md"));

        assertTrue(!readme.contains(".\\scripts\\"), "Public README must not reference ignored local helper scripts");
        assertTrue(!readme.contains("C:\\Users\\rolan"), "Public README must not include machine-specific profile paths");
        assertTrue(readme.contains("$profileRoot = Join-Path $env:APPDATA \"ModrinthApp\\profiles\\<profile-name>\""),
            "Public README should use a generic Modrinth profile path placeholder");
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
