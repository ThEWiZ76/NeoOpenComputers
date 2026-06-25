package li.cil.oc.api;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ApiReadmeTest {
    @Test
    void apiReadmeGuidesCommunityAddonsForNeoOpenComputers() throws Exception {
        final Path readme = Path.of("src/main/java/li/cil/oc/api/README.md");

        assertTrue(Files.exists(readme));

        final String content = Files.readString(readme);
        assertTrue(content.contains("NeoOpenComputers"));
        assertTrue(content.contains("NeoForge"));
        assertTrue(content.contains("SimpleComponent"));
        assertTrue(content.contains("Driver.add"));
        assertTrue(content.contains("FileSystem.asManagedEnvironment"));
    }
}
