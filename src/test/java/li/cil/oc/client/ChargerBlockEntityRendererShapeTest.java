package li.cil.oc.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChargerBlockEntityRendererShapeTest {
    @Test
    void chargerRendererClassExists() throws ClassNotFoundException {
        assertEquals("li.cil.oc.client.ChargerBlockEntityRenderer", Class.forName("li.cil.oc.client.ChargerBlockEntityRenderer").getName());
    }

    @Test
    void clientRegistersChargerRenderer() throws IOException {
        final String client = Files.readString(Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java"));

        assertTrue(client.contains("ModBlockEntities.CHARGER.get()"));
        assertTrue(client.contains("ChargerBlockEntityRenderer::new"));
    }

    @Test
    void rendererUsesUpstreamChargerOverlayTextures() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/ChargerBlockEntityRenderer.java"));

        assertTrue(renderer.contains("block/overlay/charger_front_on"));
        assertTrue(renderer.contains("block/overlay/charger_side_on"));
        assertTrue(renderer.contains("TextureAtlas.LOCATION_BLOCKS"));
    }

    @Test
    void rendererDrawsSpeedAndPowerOverlaysLikeUpstream() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/ChargerBlockEntityRenderer.java"));

        assertTrue(renderer.contains("visualChargeSpeed()"));
        assertTrue(renderer.contains("isVisuallyPowered()"));
        assertTrue(renderer.contains("renderFrontOverlay"));
        assertTrue(renderer.contains("renderSideOverlays"));
        assertTrue(renderer.contains("orientToChargerFront"));
    }
}
