package li.cil.oc.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DisassemblerBlockEntityRendererShapeTest {
    @Test
    void disassemblerRendererClassExists() throws ClassNotFoundException {
        assertEquals("li.cil.oc.client.DisassemblerBlockEntityRenderer", Class.forName("li.cil.oc.client.DisassemblerBlockEntityRenderer").getName());
    }

    @Test
    void clientRegistersDisassemblerRenderer() throws IOException {
        final String client = Files.readString(Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java"));

        assertTrue(client.contains("ModBlockEntities.DISASSEMBLER.get()"));
        assertTrue(client.contains("DisassemblerBlockEntityRenderer::new"));
    }

    @Test
    void rendererUsesUpstreamDisassemblerOverlayTextures() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/DisassemblerBlockEntityRenderer.java"));

        assertTrue(renderer.contains("block/overlay/disassembler_top_on"));
        assertTrue(renderer.contains("block/overlay/disassembler_side_on"));
        assertTrue(renderer.contains("TextureAtlas.LOCATION_BLOCKS"));
    }

    @Test
    void rendererDrawsOverlaysOnlyWhenDisassemblerIsActiveLikeUpstream() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/DisassemblerBlockEntityRenderer.java"));

        assertTrue(renderer.contains("isVisuallyActive()"));
        assertTrue(renderer.contains("renderTopOverlay"));
        assertTrue(renderer.contains("renderSideOverlays"));
    }
}
