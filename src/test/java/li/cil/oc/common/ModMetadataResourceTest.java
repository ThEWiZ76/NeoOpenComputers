package li.cil.oc.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ModMetadataResourceTest {
    private static final Path MODS_TOML = Path.of("src/main/resources/META-INF/neoforge.mods.toml");

    @Test
    void modMetadataUsesCommunityRepositoryLinks() throws IOException {
        final String metadata = Files.readString(MODS_TOML);

        assertTrue(metadata.contains("issueTrackerURL=\"https://github.com/ThEWiZ76/NeoOpenComputers/issues\""));
        assertTrue(metadata.contains("displayURL=\"https://github.com/ThEWiZ76/NeoOpenComputers\""));
        assertTrue(metadata.contains("credits=\"Based on MightyPirates/OpenComputers.\""));
        assertTrue(metadata.contains("authors=\"NeoOpenComputers community\""));
    }
}
