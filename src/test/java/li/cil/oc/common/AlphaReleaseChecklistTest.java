package li.cil.oc.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
            ".\\scripts\\check-actions-disabled.ps1",
            "git status --short --branch",
            "Get-FileHash",
            "ALPHA_SMOKE_MATRIX.md",
            "SCREEN_WORK_PROTOCOL.md",
            "VISUAL_SMOKE_RUNBOOK.md",
            "Do not add `.github/workflows`",
            "Screen renderer, model, glyph, and multiblock code stay frozen"
        }) {
            assertTrue(checklist.contains(required), "Checklist missing required release gate: " + required);
        }
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
            "Do not change textures, transforms, offsets, render layers, glyph sizing, or multiblock state in the same patch"
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
            "Stop and file a finding"
        }) {
            assertTrue(runbook.contains(required), "Visual smoke runbook missing proof item: " + required);
        }
    }
}
