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
        assertTrue(readme.contains("293/293"), "README must include current GameTest evidence");
        assertTrue(readme.contains(".\\scripts\\package-first-smoke-kit.ps1"), "README must document first-smoke kit packaging");
        assertTrue(readme.contains("Disk-drive floppy data survives save/reload"), "README must include current disk-drive persistence smoke item");
        assertTrue(readme.contains("Terminal item key input, paste, mouse click/drag/release, and scroll reach the bound computer"),
            "README must include current terminal item input smoke item");
        assertTrue(readme.contains(".\\scripts\\run-first-smoke-client.ps1"), "README must document interactive first-smoke launcher");
        assertTrue(readme.contains("-WithLocalMcpServerMod"), "README must document optional MCP helper mod launch");
        assertTrue(readme.contains("mcp-server-mod-neoforge-1.1.0+neoforge.mc1.21.1.jar"),
            "README must document current local MCP helper jar path");
        assertTrue(readme.contains("build\\first-smoke-sessions"), "README must document interactive session logs");
        assertTrue(readme.contains(".\\scripts\\run-client-smoke.ps1"), "README must document bounded client smoke script");
        assertTrue(readme.contains(".\\scripts\\collect-first-smoke-report.ps1"), "README must document first-smoke evidence collection");
        assertTrue(readme.contains("screenshots"), "README must tell testers screenshots are bundled");
        assertTrue(readme.contains("GitHub Actions are intentionally disabled"), "README must warn Actions remain disabled");
    }

    @Test
    void boundedClientSmokeScriptExists() {
        assertTrue(Files.exists(Path.of("scripts/run-client-smoke.ps1")), "Missing bounded client smoke script");
    }

    @Test
    void firstSmokeKitPackagerExists() throws IOException {
        final Path script = Path.of("scripts/package-first-smoke-kit.ps1");
        assertTrue(Files.exists(script), "Missing first-smoke kit packager");

        final String scriptText = Files.readString(script);
        assertTrue(scriptText.contains("neoopencomputers-$modVersion-all.jar"), "Kit must include bundled install jar");
        assertTrue(scriptText.contains("neoopencomputers-$modVersion-api.jar"), "Kit must include API jar");
        assertTrue(scriptText.contains("neoopencomputers-$modVersion-javadoc.jar"), "Kit must include Javadoc jar");
        assertTrue(scriptText.contains("SHA256SUMS.txt"), "Kit must write checksums");
        assertTrue(scriptText.contains("DryRun"), "Kit must have a dry-run path for verification");
        assertTrue(scriptText.contains("Use neoopencomputers-$modVersion-all.jar"),
            "Kit README must not use PowerShell backticks that corrupt generated filenames");
        assertTrue(scriptText.contains("-WithLocalMcpServerMod"), "Kit README must document MCP-assisted smoke launch");
        assertTrue(scriptText.contains("-ExtraMod <path>"), "Kit README must document custom helper mod launch");
        assertTrue(scriptText.contains("    .\\scripts\\run-first-smoke-client.ps1 -WithLocalMcpServerMod"),
            "Kit README must render MCP-assisted smoke command without Markdown backtick escapes");
        assertTrue(!scriptText.contains("`neoopencomputers"),
            "PowerShell treats Markdown backticks as escapes inside generated README text");
        assertTrue(scriptText.contains("Compress-Archive -Path"), "Kit zip must expand wildcard contents");
        assertTrue(!scriptText.contains("Compress-Archive -LiteralPath (Join-Path $kitDir '*')"),
            "LiteralPath does not expand the kit wildcard when zipping");
    }

    @Test
    void firstSmokeClientLauncherExists() throws IOException {
        final Path script = Path.of("scripts/run-first-smoke-client.ps1");
        assertTrue(Files.exists(script), "Missing first-smoke client launcher");

        final String scriptText = Files.readString(script);
        assertTrue(scriptText.contains("runClient"), "Launcher must start development client");
        assertTrue(scriptText.contains("first-smoke-sessions"), "Launcher must write interactive session logs");
        assertTrue(scriptText.contains("RedirectStandardOutput"), "Launcher must capture stdout");
        assertTrue(scriptText.contains("RedirectStandardError"), "Launcher must capture stderr");
        assertTrue(scriptText.contains("collect-first-smoke-report.ps1"), "Launcher must collect evidence after client exit");
        assertTrue(scriptText.contains("WithLocalMcpServerMod"), "Launcher must support the local MCP helper mod");
        assertTrue(scriptText.contains("ExtraMod"), "Launcher must support extra helper mods");
        assertTrue(scriptText.contains("extra-mods.txt"), "Launcher must record copied helper mods");
        assertTrue(scriptText.contains("KeepExtraMods"), "Launcher must let testers keep helper mods when requested");
        assertTrue(scriptText.contains("DryRun"), "Launcher must have a dry-run path for verification");
    }

    @Test
    void firstSmokeReportCollectorExists() throws IOException {
        final Path script = Path.of("scripts/collect-first-smoke-report.ps1");
        assertTrue(Files.exists(script), "Missing first-smoke report collector");

        final String scriptText = Files.readString(script);
        assertTrue(scriptText.contains("Tester Checklist"), "Collector must write tester checklist");
        assertTrue(scriptText.contains("Compress-Archive -Path"), "Collector zip must expand wildcard contents");
        assertTrue(!scriptText.contains("Compress-Archive -LiteralPath (Join-Path $reportDir '*')"),
            "LiteralPath does not expand the report wildcard when zipping");
        assertTrue(scriptText.contains("Missing texture"), "Collector must scan missing texture failures");
        assertTrue(scriptText.contains("interactive-client-stdout.log"), "Collector must copy interactive stdout");
        assertTrue(scriptText.contains("interactive-client-stderr.log"), "Collector must copy interactive stderr");
        assertTrue(scriptText.contains("interactive-extra-mods.txt"), "Collector must copy interactive helper mod manifest");
        assertTrue(scriptText.contains("screenshots"), "Collector must copy recent Minecraft screenshots");
        assertTrue(scriptText.contains("Select-Object -First 20"), "Collector must bound copied screenshots");
        assertTrue(scriptText.contains("Terminal item key input, paste, mouse click/drag/release, and scroll reach the bound computer"),
            "Collector checklist must include terminal item input smoke item");
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
