package li.cil.oc.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HologramBlockEntityRendererShapeTest {
    @Test
    void hologramRendererClassExists() throws ClassNotFoundException {
        assertEquals("li.cil.oc.client.HologramBlockEntityRenderer", Class.forName("li.cil.oc.client.HologramBlockEntityRenderer").getName());
    }

    @Test
    void clientRegistersHologramRenderer() throws IOException {
        final String client = Files.readString(Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java"));

        assertTrue(client.contains("ModBlockEntities.HOLOGRAM.get()"), "Hologram block entity should have a client renderer");
        assertTrue(client.contains("HologramBlockEntityRenderer::new"), "Hologram renderer should be registered client-side");
    }

    @Test
    void rendererUsesVisibleVoxelFacesAndUpstreamAlpha() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/HologramBlockEntityRenderer.java"));

        assertTrue(renderer.contains("visibleFaces"), "Renderer should skip internal voxel faces like upstream VBO indexing");
        assertTrue(renderer.contains("0.75F"), "Upstream hologram base alpha is 0.75");
        assertTrue(renderer.contains("RenderType.debugQuads()"), "Renderer should use position/color translucent quads");
        assertEquals(6, HologramBlockEntityRenderer.visibleFaces(0, 0, 0, (x, y, z) -> 0).size());
        assertEquals(5, HologramBlockEntityRenderer.visibleFaces(0, 0, 0, (x, y, z) -> x == 1 && y == 0 && z == 0 ? 1 : 0).size());
    }

    @Test
    void rendererConvertsPaletteColorToArgbWithAlpha() {
        assertEquals(0xBF112233, HologramBlockEntityRenderer.colorWithAlpha(0x112233, 0.75F));
        assertEquals(0x00000000, HologramBlockEntityRenderer.colorWithAlpha(0x112233, 0F));
    }
}
