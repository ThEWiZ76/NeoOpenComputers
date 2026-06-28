package li.cil.oc.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AssemblerBlockEntityRendererShapeTest {
    @Test
    void assemblerRendererClassExists() throws ClassNotFoundException {
        assertEquals("li.cil.oc.client.AssemblerBlockEntityRenderer", Class.forName("li.cil.oc.client.AssemblerBlockEntityRenderer").getName());
    }

    @Test
    void clientRegistersAssemblerRenderer() throws IOException {
        final String client = Files.readString(Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java"));

        assertTrue(client.contains("ModBlockEntities.ASSEMBLER.get()"));
        assertTrue(client.contains("AssemblerBlockEntityRenderer::new"));
    }

    @Test
    void rendererUsesUpstreamAssemblerOverlayTextures() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/AssemblerBlockEntityRenderer.java"));

        assertTrue(renderer.contains("block/overlay/assembler_top_on"));
        assertTrue(renderer.contains("block/overlay/assembler_side_on"));
        assertTrue(renderer.contains("block/overlay/assembler_side_assembling"));
        assertTrue(renderer.contains("TextureAtlas.LOCATION_BLOCKS"));
    }

    @Test
    void rendererDrawsAlwaysOnAndAssemblingOverlaysLikeUpstream() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/AssemblerBlockEntityRenderer.java"));

        assertTrue(renderer.contains("renderTopOverlay"));
        assertTrue(renderer.contains("renderSideOnOverlays"));
        assertTrue(renderer.contains("isVisuallyAssembling()"));
        assertTrue(renderer.contains("renderAssemblingOverlays"));
    }
}
