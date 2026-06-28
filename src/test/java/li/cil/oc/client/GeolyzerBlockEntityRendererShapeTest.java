package li.cil.oc.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GeolyzerBlockEntityRendererShapeTest {
    @Test
    void geolyzerRendererClassExists() throws ClassNotFoundException {
        assertEquals("li.cil.oc.client.GeolyzerBlockEntityRenderer", Class.forName("li.cil.oc.client.GeolyzerBlockEntityRenderer").getName());
    }

    @Test
    void clientRegistersGeolyzerRenderer() throws IOException {
        final String client = Files.readString(Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java"));

        assertTrue(client.contains("ModBlockEntities.GEOLYZER.get()"));
        assertTrue(client.contains("GeolyzerBlockEntityRenderer::new"));
    }

    @Test
    void rendererUsesUpstreamGeolyzerTopOverlayTexture() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/GeolyzerBlockEntityRenderer.java"));

        assertTrue(renderer.contains("block/overlay/geolyzer_top_on"));
        assertTrue(renderer.contains("TextureAtlas.LOCATION_BLOCKS"));
        assertTrue(renderer.contains("RenderType.cutout()"));
    }

    @Test
    void rendererDrawsSlightlyRaisedTopOverlayLikeUpstream() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/GeolyzerBlockEntityRenderer.java"));

        assertTrue(renderer.contains("TOP_Y"));
        assertTrue(renderer.contains("renderTopOverlay"));
        assertTrue(renderer.contains("Direction.UP"));
        assertTrue(renderer.contains("OverlayTexture.NO_OVERLAY"));
    }
}
