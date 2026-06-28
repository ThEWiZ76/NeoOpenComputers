package li.cil.oc.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RelayBlockEntityRendererShapeTest {
    @Test
    void relayRendererClassExists() throws ClassNotFoundException {
        assertEquals("li.cil.oc.client.RelayBlockEntityRenderer", Class.forName("li.cil.oc.client.RelayBlockEntityRenderer").getName());
    }

    @Test
    void clientRegistersRelayRenderer() throws IOException {
        final String client = Files.readString(Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java"));

        assertTrue(client.contains("ModBlockEntities.RELAY.get()"));
        assertTrue(client.contains("RelayBlockEntityRenderer::new"));
    }

    @Test
    void rendererUsesUpstreamSwitchActivityTexture() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/RelayBlockEntityRenderer.java"));

        assertTrue(renderer.contains("block/overlay/switch_side_on"));
        assertTrue(renderer.contains("TextureAtlas.LOCATION_BLOCKS"));
        assertTrue(renderer.contains("RenderType.translucent()"));
    }

    @Test
    void rendererFadesSideActivityLikeUpstream() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/RelayBlockEntityRenderer.java"));

        assertTrue(renderer.contains("relay.visualActivity()"));
        assertTrue(renderer.contains("renderActivityOverlays"));
        assertTrue(renderer.contains("renderSideOverlay(Direction.NORTH"));
        assertTrue(renderer.contains("renderSideOverlay(Direction.SOUTH"));
        assertTrue(renderer.contains("renderSideOverlay(Direction.EAST"));
        assertTrue(renderer.contains("renderSideOverlay(Direction.WEST"));
    }
}
