package li.cil.oc.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TransposerBlockEntityRendererShapeTest {
    @Test
    void transposerRendererClassExists() throws ClassNotFoundException {
        assertEquals("li.cil.oc.client.TransposerBlockEntityRenderer", Class.forName("li.cil.oc.client.TransposerBlockEntityRenderer").getName());
    }

    @Test
    void clientRegistersTransposerRenderer() throws IOException {
        final String client = Files.readString(Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java"));

        assertTrue(client.contains("ModBlockEntities.TRANSPOSER.get()"));
        assertTrue(client.contains("TransposerBlockEntityRenderer::new"));
    }

    @Test
    void rendererUsesUpstreamTransposerActivityTexture() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/TransposerBlockEntityRenderer.java"));

        assertTrue(renderer.contains("block/overlay/transposer_on"));
        assertTrue(renderer.contains("TextureAtlas.LOCATION_BLOCKS"));
        assertTrue(renderer.contains("RenderType.translucent()"));
    }

    @Test
    void rendererFadesActivityLikeUpstream() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/TransposerBlockEntityRenderer.java"));

        assertTrue(renderer.contains("visualActivity()"));
        assertTrue(renderer.contains("renderActivityOverlays"));
        assertTrue(renderer.contains("Direction.values()"));
    }
}
