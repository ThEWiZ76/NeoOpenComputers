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
            "Do not add `.github/workflows`",
            "Screen renderer, model, glyph, and multiblock code stay frozen"
        }) {
            assertTrue(checklist.contains(required), "Checklist missing required release gate: " + required);
        }
    }
}
