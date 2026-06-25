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
        assertTrue(readme.contains("291/291"), "README must include current GameTest evidence");
        assertTrue(readme.contains("Disk-drive floppy data survives save/reload"), "README must include current disk-drive persistence smoke item");
        assertTrue(readme.contains(".\\scripts\\run-client-smoke.ps1"), "README must document bounded client smoke script");
        assertTrue(readme.contains(".\\scripts\\collect-first-smoke-report.ps1"), "README must document first-smoke evidence collection");
        assertTrue(readme.contains("GitHub Actions are intentionally disabled"), "README must warn Actions remain disabled");
    }

    @Test
    void boundedClientSmokeScriptExists() {
        assertTrue(Files.exists(Path.of("scripts/run-client-smoke.ps1")), "Missing bounded client smoke script");
    }

    @Test
    void firstSmokeReportCollectorExists() throws IOException {
        final Path script = Path.of("scripts/collect-first-smoke-report.ps1");
        assertTrue(Files.exists(script), "Missing first-smoke report collector");

        final String scriptText = Files.readString(script);
        assertTrue(scriptText.contains("Tester Checklist"), "Collector must write tester checklist");
        assertTrue(scriptText.contains("Compress-Archive"), "Collector must package a zip by default");
        assertTrue(scriptText.contains("Missing texture"), "Collector must scan missing texture failures");
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
