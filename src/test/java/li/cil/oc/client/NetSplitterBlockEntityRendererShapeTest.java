package li.cil.oc.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NetSplitterBlockEntityRendererShapeTest {
    @Test
    void netSplitterRendererClassExists() throws ClassNotFoundException {
        assertEquals("li.cil.oc.client.NetSplitterBlockEntityRenderer", Class.forName("li.cil.oc.client.NetSplitterBlockEntityRenderer").getName());
    }

    @Test
    void clientRegistersNetSplitterRenderer() throws IOException {
        final String client = Files.readString(Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java"));

        assertTrue(client.contains("ModBlockEntities.NET_SPLITTER.get()"));
        assertTrue(client.contains("NetSplitterBlockEntityRenderer::new"));
    }

    @Test
    void rendererUsesUpstreamOpenSideTexture() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/NetSplitterBlockEntityRenderer.java"));

        assertTrue(renderer.contains("block/overlay/netsplitter_on"));
        assertTrue(renderer.contains("TextureAtlas.LOCATION_BLOCKS"));
        assertTrue(renderer.contains("RenderType.translucent()"));
    }

    @Test
    void rendererDrawsOnlyOpenSidesLikeUpstream() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/NetSplitterBlockEntityRenderer.java"));

        assertTrue(renderer.contains("splitter.isSideOpen(side)"));
        assertTrue(renderer.contains("Direction.values()"));
        assertTrue(renderer.contains("renderSideOverlay"));
    }
}
