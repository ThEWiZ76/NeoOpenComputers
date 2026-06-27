package li.cil.oc.client;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AdapterBlockEntityRendererShapeTest {
    @Test
    void adapterRendererClassExists() throws ClassNotFoundException {
        assertEquals("li.cil.oc.client.AdapterBlockEntityRenderer", Class.forName("li.cil.oc.client.AdapterBlockEntityRenderer").getName());
    }

    @Test
    void clientRegistersAdapterRenderer() throws IOException {
        final String client = Files.readString(Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java"));

        assertTrue(client.contains("ModBlockEntities.ADAPTER.get()"));
        assertTrue(client.contains("AdapterBlockEntityRenderer::new"));
    }

    @Test
    void rendererUsesUpstreamAdapterOverlayTexture() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/AdapterBlockEntityRenderer.java"));

        assertTrue(renderer.contains("block/overlay/adapter_on"));
        assertTrue(renderer.contains("TextureAtlas.LOCATION_BLOCKS"));
    }

    @Test
    void rendererExposesPerSideOverlayQuadsLikeUpstream() throws Exception {
        final Class<?> renderer = Class.forName("li.cil.oc.client.AdapterBlockEntityRenderer");
        final Method method = renderer.getDeclaredMethod("sideNormal", Direction.class);

        assertEquals(Direction.DOWN, method.invoke(null, Direction.DOWN));
        assertEquals(Direction.UP, method.invoke(null, Direction.UP));
        assertEquals(Direction.NORTH, method.invoke(null, Direction.NORTH));
        assertEquals(Direction.SOUTH, method.invoke(null, Direction.SOUTH));
        assertEquals(Direction.WEST, method.invoke(null, Direction.WEST));
        assertEquals(Direction.EAST, method.invoke(null, Direction.EAST));
    }
}
