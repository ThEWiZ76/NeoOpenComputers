package li.cil.oc.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ModPackagingTest {
    private static final Path BUILD_GRADLE = Path.of("build.gradle");

    @Test
    void jarPackagingIncludesProjectAndBundledLibraryLicenses() throws IOException {
        final String build = Files.readString(BUILD_GRADLE);

        assertTrue(build.contains("LICENSE-neoopencomputers.txt"));
        assertTrue(build.contains("LICENSE-luaj.txt"));
    }
}
