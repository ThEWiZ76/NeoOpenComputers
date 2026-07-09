package li.cil.oc.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ManualContentShapeTest {
    @Test
    void defaultManualTabsUseFlatTextures() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/client/ManualContent.java"));

        assertTrue(!source.contains("ItemStackTabIconRenderer"));
        assertTrue(source.contains("BLOCKS_TAB_TEXTURE"));
        assertTrue(source.contains("ITEMS_TAB_TEXTURE"));
    }
}
