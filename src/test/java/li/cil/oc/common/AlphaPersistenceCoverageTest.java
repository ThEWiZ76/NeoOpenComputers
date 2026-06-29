package li.cil.oc.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class AlphaPersistenceCoverageTest {
    @Test
    void firstAlphaPersistenceGameTestsCoverRackDiskDriveReload() throws IOException {
        final String gameTests = Files.readString(Path.of("src/main/java/li/cil/oc/common/gametest/NeoOpenComputersGameTests.java"));

        assertTrue(gameTests.contains("rackDiskDriveWritableFloppyStateSurvivesNbtReloadForFirstSmoke"),
            "First-alpha GameTests must cover rack disk-drive floppy persistence across NBT reload");
    }
}
