package li.cil.oc.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScreenColorHandlerShapeTest {
    @Test
    void clientRegistersUpstreamScreenTierTintHandlers() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java"));

        assertTrue(source.contains("RegisterColorHandlersEvent.Block"));
        assertTrue(source.contains("RegisterColorHandlersEvent.Item"));
        assertTrue(source.contains("screenTierColor"));
        assertTrue(source.contains("ModBlocks.SCREEN_TIER1.get()"));
        assertTrue(source.contains("ModBlocks.SCREEN_TIER2.get()"));
        assertTrue(source.contains("ModBlocks.SCREEN_TIER3.get()"));
    }

    @Test
    void clientRegistersFloppyColorModelProperty() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java"));

        assertTrue(source.contains("ItemProperties.register"));
        assertTrue(source.contains("floppy_color"));
        assertTrue(source.contains("FloppyItem.floppyColorIndex"));
    }
}
