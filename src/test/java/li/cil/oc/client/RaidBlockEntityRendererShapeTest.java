package li.cil.oc.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RaidBlockEntityRendererShapeTest {
    @Test
    void raidRendererClassExists() throws ClassNotFoundException {
        assertEquals("li.cil.oc.client.RaidBlockEntityRenderer", Class.forName("li.cil.oc.client.RaidBlockEntityRenderer").getName());
    }

    @Test
    void clientRegistersRaidRenderer() throws IOException {
        final String client = Files.readString(Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java"));

        assertTrue(client.contains("ModBlockEntities.RAID.get()"));
        assertTrue(client.contains("RaidBlockEntityRenderer::new"));
    }

    @Test
    void rendererUsesUpstreamRaidOverlayTextures() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/RaidBlockEntityRenderer.java"));

        assertTrue(renderer.contains("block/overlay/raid_front_error"));
        assertTrue(renderer.contains("block/overlay/raid_front_activity"));
        assertTrue(renderer.contains("TextureAtlas.LOCATION_BLOCKS"));
        assertTrue(renderer.contains("RenderType.cutout()"));
    }

    @Test
    void rendererDrawsPerSlotFrontOverlaysLikeUpstream() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/RaidBlockEntityRenderer.java"));

        assertTrue(renderer.contains("raid.visualPresenceMask()"));
        assertTrue(renderer.contains("raid.visualActiveSlot()"));
        assertTrue(renderer.contains("renderSlotOverlay"));
        assertTrue(renderer.contains("2F / 16F"));
        assertTrue(renderer.contains("4F / 16F"));
    }
}
