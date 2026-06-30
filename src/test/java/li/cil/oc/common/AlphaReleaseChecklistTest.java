package li.cil.oc.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class AlphaReleaseChecklistTest {
    @Test
    void alphaReleaseChecklistDocumentsHandoffGates() throws IOException {
        final Path checklistPath = Path.of("ALPHA_RELEASE_CHECKLIST.md");
        final String readme = Files.readString(Path.of("README.md"));

        assertTrue(Files.isRegularFile(checklistPath), "First-alpha release checklist must exist");
        assertTrue(readme.contains("[Alpha Release Checklist](ALPHA_RELEASE_CHECKLIST.md)"),
            "README must link alpha release checklist");

        final String checklist = Files.readString(checklistPath);
        for (final String required : new String[]{
            "build/libs/neoopencomputers-0.1.0.jar",
            "NeoForge 21.1.234",
            "Do not ship `neoopencomputers-0.1.0-thin.jar`",
            ".\\gradlew.bat test build --no-daemon --console=plain",
            ".\\gradlew.bat runGameTestServer --no-daemon --console=plain",
            "Test-Path .github\\workflows",
            "git ls-tree -r HEAD .github/workflows",
            "git ls-tree -r origin/develop .github/workflows",
            "git status --short --branch",
            "Get-FileHash",
            "Compare the installed profile jar hash to the built jar hash",
            "Close Minecraft before replacing the installed jar",
            "$installedAfter",
            "Verify the installed jar hash matches the built jar hash after copying",
            "ALPHA_SMOKE_MATRIX.md",
            "SCREEN_WORK_PROTOCOL.md",
            "VISUAL_SMOKE_RUNBOOK.md",
            "ALPHA_FINDING_TEMPLATE.md",
            ".github/ISSUE_TEMPLATE/alpha-finding.yml",
            "Do not add `.github/workflows`",
            "Screen renderer, model, glyph, and multiblock code stay frozen",
            "MCP terminal-open evidence alone is not enough",
            "NeoOpenComputers-owned missing model, missing texture, or mod loading errors",
            "Power and charging",
            "Texture picker"
        }) {
            assertTrue(checklist.contains(required), "Checklist missing required release gate: " + required);
        }
        assertTrue(!checklist.contains(".\\scripts\\"),
            "Public alpha checklist must not depend on ignored local helper scripts");
    }

    @Test
    void alphaFindingTemplateDocumentsReportBundle() throws IOException {
        final Path templatePath = Path.of("ALPHA_FINDING_TEMPLATE.md");
        final String readme = Files.readString(Path.of("README.md"));
        final String guide = Files.readString(Path.of("ALPHA_TESTING.md"));
        final String checklist = Files.readString(Path.of("ALPHA_RELEASE_CHECKLIST.md"));

        assertTrue(Files.isRegularFile(templatePath), "Reusable alpha finding template must exist");
        assertTrue(Files.isRegularFile(Path.of(".github", "ISSUE_TEMPLATE", "alpha-finding.yml")),
            "GitHub alpha finding issue form must exist");
        assertTrue(Files.isRegularFile(Path.of(".github", "ISSUE_TEMPLATE", "config.yml")),
            "GitHub issue template config must exist");
        assertTrue(!Files.exists(Path.of(".github", "workflows")),
            "GitHub Actions workflows must stay absent during alpha hardening");
        assertTrue(readme.contains("[Alpha Finding Template](ALPHA_FINDING_TEMPLATE.md)"),
            "README must link the alpha finding template");
        assertTrue(readme.contains("GitHub `Alpha finding` issue form"),
            "README must mention the GitHub alpha finding issue form");
        assertTrue(guide.contains("ALPHA_FINDING_TEMPLATE.md"),
            "Alpha testing guide must link the alpha finding template");
        assertTrue(guide.contains("GitHub `Alpha finding` issue form"),
            "Alpha testing guide must mention the GitHub alpha finding issue form");
        assertTrue(checklist.contains("ALPHA_FINDING_TEMPLATE.md"),
            "Alpha release checklist must require the alpha finding template");

        final String template = Files.readString(templatePath);
        for (final String required : new String[]{
            "NeoOpenComputers jar version or commit",
            "Exact reproduction steps",
            "Expected result",
            "Actual result",
            "Crash report path or full crash log",
            "Client log section around the failure",
            "Crash report timestamp",
            "Minecraft restart timestamp",
            "Does the crash report timestamp come after the current jar install",
            "Screenshot or short video",
            "World name, seed, coordinates, component tiers",
            "Jar SHA256",
            "Third-party profile warnings",
            "Screen issue evidence"
        }) {
            assertTrue(template.contains(required), "Alpha finding template missing report field: " + required);
        }

        final String issueForm = Files.readString(Path.of(".github", "ISSUE_TEMPLATE", "alpha-finding.yml"));
        assertTrue(!Pattern.compile("(?m)^\\s+description: [^\"'][^#\\r\\n]*:\\s+").matcher(issueForm).find(),
            "GitHub issue form descriptions with colon-space must be quoted or GitHub ignores the form");
        for (final String required : new String[]{
            "NeoOpenComputers jar version or commit",
            "Jar SHA256",
            "Exact reproduction steps",
            "Expected result",
            "Actual result",
            "Crash report timestamp",
            "Minecraft restart timestamp",
            "Does the crash report timestamp come after the current jar install",
            "Screen issue evidence"
        }) {
            assertTrue(issueForm.contains(required), "GitHub issue form missing report field: " + required);
        }

        final String issueConfig = Files.readString(Path.of(".github", "ISSUE_TEMPLATE", "config.yml"));
        assertTrue(issueConfig.contains("blank_issues_enabled: false"),
            "GitHub issue config should force alpha reports through the structured form");
        assertTrue(issueConfig.contains("issues/new?template=alpha-finding.yml"),
            "GitHub issue config should include a direct alpha finding form link");
    }

    @Test
    void screenWorkProtocolDocumentsAntiLoopGate() throws IOException {
        final Path protocolPath = Path.of("SCREEN_WORK_PROTOCOL.md");
        final String readme = Files.readString(Path.of("README.md"));

        assertTrue(Files.isRegularFile(protocolPath), "Screen work protocol must exist");
        assertTrue(readme.contains("[Screen Work Protocol](SCREEN_WORK_PROTOCOL.md)"),
            "README must link screen work protocol");

        final String protocol = Files.readString(protocolPath);
        for (final String required : new String[]{
            "No screen code changes before evidence",
            "one reproducible root cause",
            "one screen layer only",
            "before screenshot",
            "after screenshot",
            "rollback point",
            "If the after screenshot proves one behavior but another screen behavior regresses, revert the screen patch",
            "After two failed screen patches in the same layer, stop implementation and write a root-cause note",
            "After three failed screen patches in the same area, require architecture review before another screen patch",
            "Do not change textures, transforms, offsets, render layers, glyph sizing, or multiblock state in the same patch",
            "Do not reintroduce a previously reverted screen texture, model, glyph, transform, render layer, or multiblock choice",
            "Do not claim world-render verification from MCP terminal-open evidence or screenshots that do not show Minecraft pixels",
            "If a screen fix has been reverted once, the next screen implementation patch must name the architecture owner"
        }) {
            assertTrue(protocol.contains(required), "Screen work protocol missing anti-loop gate: " + required);
        }
    }

    @Test
    void visualSmokeRunbookDocumentsManualProof() throws IOException {
        final Path runbookPath = Path.of("VISUAL_SMOKE_RUNBOOK.md");
        final String readme = Files.readString(Path.of("README.md"));

        assertTrue(Files.isRegularFile(runbookPath), "Visual smoke runbook must exist");
        assertTrue(readme.contains("[Visual Smoke Runbook](VISUAL_SMOKE_RUNBOOK.md)"),
            "README must link visual smoke runbook");

        final String runbook = Files.readString(runbookPath);
        for (final String required : new String[]{
            "01-openos-prompt.png",
            "02-computer-gui.png",
            "03-screen-after-reload.png",
            "04-creative-tab.png",
            "05-manual.png",
            "06-printer-print.png",
            "Jar SHA256",
            "Compare the installed profile jar hash to the built jar hash",
            "Close Minecraft before replacing the installed jar",
            "Copy-Item .\\build\\libs\\neoopencomputers-0.1.0.jar",
            "$installedAfter",
            "Verify the installed jar hash matches the built jar hash after copying",
            "mcp-client.json",
            "8081",
            "NVIDIA Broadcast",
            "Compress-Archive",
            "Create the smoke report folder before capturing screenshots",
            "Save every required screenshot directly in `$report`",
            "(alpha unavailable)",
            "Stop and file a finding"
        }) {
            assertTrue(runbook.contains(required), "Visual smoke runbook missing proof item: " + required);
        }
        assertTrue(!runbook.contains(".\\scripts\\"),
            "Public visual smoke runbook must not depend on ignored local helper scripts");
    }
}
