package li.cil.oc.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PowerDistributorBlockEntityRendererShapeTest {
    @Test
    void powerDistributorRendererClassExists() throws ClassNotFoundException {
        assertEquals("li.cil.oc.client.PowerDistributorBlockEntityRenderer", Class.forName("li.cil.oc.client.PowerDistributorBlockEntityRenderer").getName());
    }

    @Test
    void clientRegistersPowerDistributorRenderer() throws IOException {
        final String client = Files.readString(Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java"));

        assertTrue(client.contains("ModBlockEntities.POWER_DISTRIBUTOR.get()"));
        assertTrue(client.contains("PowerDistributorBlockEntityRenderer::new"));
    }

    @Test
    void rendererUsesUpstreamPowerDistributorOverlayTextures() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/PowerDistributorBlockEntityRenderer.java"));

        assertTrue(renderer.contains("block/overlay/powerdistributor_top_on"));
        assertTrue(renderer.contains("block/overlay/powerdistributor_side_on"));
        assertTrue(renderer.contains("TextureAtlas.LOCATION_BLOCKS"));
    }

    @Test
    void rendererDrawsOverlayWithBufferAlphaLikeUpstream() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/PowerDistributorBlockEntityRenderer.java"));

        assertTrue(renderer.contains("visualBufferRatio()"));
        assertTrue(renderer.contains("RenderType.translucent()"));
        assertTrue(renderer.contains("renderTopOverlay"));
        assertTrue(renderer.contains("renderSideOverlays"));
    }
}
