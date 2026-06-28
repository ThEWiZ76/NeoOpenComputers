package li.cil.oc.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RackBlockEntityRendererShapeTest {
    @Test
    void rackRendererClassExists() throws ClassNotFoundException {
        assertEquals("li.cil.oc.client.RackBlockEntityRenderer", Class.forName("li.cil.oc.client.RackBlockEntityRenderer").getName());
    }

    @Test
    void clientRegistersRackRenderer() throws IOException {
        final String client = Files.readString(Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java"));

        assertTrue(client.contains("ModBlockEntities.RACK.get()"));
        assertTrue(client.contains("RackBlockEntityRenderer::new"));
    }

    @Test
    void rendererUsesUpstreamRackSlotVRange() throws Exception {
        final Class<?> renderer = Class.forName("li.cil.oc.client.RackBlockEntityRenderer");
        final Method v0 = renderer.getDeclaredMethod("slotV0", int.class);
        final Method v1 = renderer.getDeclaredMethod("slotV1", int.class);

        v0.setAccessible(true);
        v1.setAccessible(true);

        assertEquals(2F / 16F, (float) v0.invoke(null, 0));
        assertEquals(5F / 16F, (float) v1.invoke(null, 0));
        assertEquals(11F / 16F, (float) v0.invoke(null, 3));
        assertEquals(14F / 16F, (float) v1.invoke(null, 3));
    }

    @Test
    void rendererPostsRackMountableTileEntityEvents() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/RackBlockEntityRenderer.java"));

        assertTrue(renderer.contains("RackMountableRenderEvent.Block"));
        assertTrue(renderer.contains("RackMountableRenderEvent.TileEntity"));
        assertTrue(renderer.contains("NeoForge.EVENT_BUS.post"));
        assertTrue(renderer.contains("rack.getMountableData(slot)"));
        assertTrue(renderer.contains("rack.getItem(slot)"));
        assertTrue(renderer.contains("stack.isEmpty()"));
    }

    @Test
    void rendererEventCanDrawOverlayFromAtlas() throws IOException {
        final String event = Files.readString(Path.of("src/main/java/li/cil/oc/api/event/RackMountableRenderEvent.java"));

        assertTrue(event.contains("private final PoseStack"));
        assertTrue(event.contains("TextureAtlas.LOCATION_BLOCKS"));
        assertTrue(event.contains("sprite.wrap(bufferSource.getBuffer(RenderType.cutout()))"));
    }
}
