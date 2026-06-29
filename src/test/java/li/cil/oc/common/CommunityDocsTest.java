package li.cil.oc.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class CommunityDocsTest {
    @Test
    void contributorGuideDocumentsCommunityPortRules() throws IOException {
        final Path guidePath = Path.of("CONTRIBUTING.md");
        final String readme = Files.readString(Path.of("README.md"));

        assertTrue(Files.isRegularFile(guidePath), "Contributor guide must exist before first alpha sharing");
        assertTrue(readme.contains("[Contributing](CONTRIBUTING.md)"),
            "README must link contributor guide for community developers");

        final String guide = Files.readString(guidePath);
        assertTrue(guide.contains("Java-first"), "Contributor guide must state Java-first direction");
        assertTrue(guide.contains("Do not port Scala source into this repository"),
            "Contributor guide must prevent reintroducing Scala implementation code");
        assertTrue(guide.contains("MIT"), "Contributor guide must document project license expectation");
        assertTrue(guide.contains("Do not add `.github/workflows`"),
            "Contributor guide must keep GitHub Actions disabled before release readiness");
        assertTrue(guide.contains("Do not change screen renderer, model, glyph, or multiblock code"),
            "Contributor guide must preserve the screen loop-control rule");
    }
}
