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
    void screenBlockColorUsesBlockEntityRenderColorLikeUpstream() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java"));

        assertTrue(source.contains("screenBlockColor"));
        assertTrue(source.contains("tintGetter.getBlockEntity(pos) instanceof ScreenBlockEntity"));
        assertTrue(source.contains("screen.getRenderColor()"));
        assertTrue(source.contains("screenTierColor(state.getBlock())"));
    }

    @Test
    void clientRegistersFloppyColorModelProperty() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java"));

        assertTrue(source.contains("ItemProperties.register"));
        assertTrue(source.contains("floppy_color"));
        assertTrue(source.contains("FloppyItem.floppyColorIndex"));
    }

    @Test
    void clientRegistersTabletRunningModelProperty() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java"));

        assertTrue(source.contains("tablet_running"));
        assertTrue(source.contains("ModItems.TABLET.get()"));
        assertTrue(source.contains("tabletRunningModelProperty"));
        assertTrue(source.contains("tablet.hasData(stack)"));
        assertTrue(source.contains("tablet.isRunning(stack) ? 1F : 0F"));
    }
}
