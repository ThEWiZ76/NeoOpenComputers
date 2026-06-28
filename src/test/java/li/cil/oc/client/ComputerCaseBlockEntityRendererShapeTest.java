package li.cil.oc.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ComputerCaseBlockEntityRendererShapeTest {
    @Test
    void caseRendererClassExists() throws ClassNotFoundException {
        assertEquals("li.cil.oc.client.ComputerCaseBlockEntityRenderer", Class.forName("li.cil.oc.client.ComputerCaseBlockEntityRenderer").getName());
    }

    @Test
    void clientRegistersCaseRenderer() throws IOException {
        final String client = Files.readString(Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java"));

        assertTrue(client.contains("ModBlockEntities.COMPUTER_CASE.get()"));
        assertTrue(client.contains("ComputerCaseBlockEntityRenderer::new"));
    }

    @Test
    void rendererUsesUpstreamCaseFrontOverlays() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/ComputerCaseBlockEntityRenderer.java"));

        assertTrue(renderer.contains("block/overlay/case_front_on"));
        assertTrue(renderer.contains("block/overlay/case_front_activity"));
        assertTrue(renderer.contains("block/overlay/case_front_error"));
        assertTrue(renderer.contains("TextureAtlas.LOCATION_BLOCKS"));
        assertTrue(renderer.contains("RenderType.cutout()"));
    }

    @Test
    void rendererDrawsRunningActivityAndErrorLikeUpstream() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/ComputerCaseBlockEntityRenderer.java"));

        assertTrue(renderer.contains("computerCase.isClientRunning()"));
        assertTrue(renderer.contains("computerCase.isClientErrored()"));
        assertTrue(renderer.contains("computerCase.visualFileSystemActivity()"));
        assertTrue(renderer.contains("shouldShowErrorLight"));
        assertTrue(renderer.contains("renderFrontOverlay"));
    }
}
