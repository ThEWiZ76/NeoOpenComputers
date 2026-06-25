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
        assertTrue(readme.contains(".\\scripts\\run-mcp-client-smoke.ps1"), "README must document bounded MCP client smoke script");
        assertTrue(readme.contains(".\\scripts\\run-mcp-world-smoke.ps1"), "README must document bounded MCP world smoke script");
        assertTrue(readme.contains("--quickPlaySingleplayer"), "README must document quick-play world entry for MCP world smoke");
        assertTrue(readme.contains(".\\scripts\\collect-first-smoke-report.ps1"), "README must document first-smoke evidence collection");
        assertTrue(readme.contains("screenshots"), "README must tell testers screenshots are bundled");
        assertTrue(readme.contains("GitHub Actions are intentionally disabled"), "README must warn Actions remain disabled");
    }

    @Test
    void boundedClientSmokeScriptExists() {
        assertTrue(Files.exists(Path.of("scripts/run-client-smoke.ps1")), "Missing bounded client smoke script");
    }

    @Test
    void boundedMcpClientSmokeScriptExists() throws IOException {
        final Path script = Path.of("scripts/run-mcp-client-smoke.ps1");
        assertTrue(Files.exists(script), "Missing bounded MCP client smoke script");

        final String scriptText = Files.readString(script);
        assertTrue(scriptText.contains("McpServerModPath"), "MCP smoke must use local helper mod path");
        assertTrue(scriptText.contains("Invoke-McpRequest"), "MCP smoke must probe JSON-RPC endpoint");
        assertTrue(scriptText.contains("tools/list"), "MCP smoke must verify tools/list");
        assertTrue(scriptText.contains("execute_commands"), "MCP smoke must verify command tool");
        assertTrue(scriptText.contains("get_player_info"), "MCP smoke must verify player info tool");
        assertTrue(scriptText.contains("mcp-client-smoke"), "MCP smoke must write bounded evidence logs");
    }

    @Test
    void boundedMcpWorldSmokeScriptExists() throws IOException {
        final Path script = Path.of("scripts/run-mcp-world-smoke.ps1");
        assertTrue(Files.exists(script), "Missing bounded MCP world smoke script");

        final String scriptText = Files.readString(script);
        assertTrue(scriptText.contains("WorldName"), "World smoke must take an explicit world name");
        assertTrue(scriptText.contains("-Pneoopencomputers.quickPlayWorld="),
            "World smoke must pass quick-play through a Gradle property so NeoGradle launch args are preserved");
        assertTrue(!scriptText.contains("--args="),
            "Gradle --args replaces NeoGradle launch args and breaks ModLauncher");
        assertTrue(scriptText.contains("get_player_info"), "World smoke must prove the client reached a real world");
        assertTrue(scriptText.contains("get_blocks_in_area"), "World smoke must prove world block scanning works");
        assertTrue(scriptText.contains("execute_commands"), "World smoke must include a command transport proof");
        assertTrue(scriptText.contains("mcp-world-smoke"), "World smoke must write bounded evidence logs");
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
        assertTrue(scriptText.contains(".\\scripts\\run-mcp-client-smoke.ps1"),
            "Kit README must document bounded MCP helper smoke");
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
        assertTrue(scriptText.contains("mcp-smoke-tools-list.json"), "Collector must copy bounded MCP smoke tools");
        assertTrue(scriptText.contains("mcp-smoke-extra-mods.txt"), "Collector must copy bounded MCP helper mod manifest");
        assertTrue(scriptText.contains("mcp-world-player-info.json"), "Collector must copy bounded MCP world player info");
        assertTrue(scriptText.contains("mcp-world-blocks.json"), "Collector must copy bounded MCP world block scan");
        assertTrue(scriptText.contains("mcp-world-command.json"), "Collector must copy bounded MCP world command proof");
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
