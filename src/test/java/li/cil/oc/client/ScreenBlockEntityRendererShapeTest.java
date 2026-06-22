package li.cil.oc.client;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScreenBlockEntityRendererShapeTest {
    @Test
    void screenRendererClassExists() throws ClassNotFoundException {
        assertEquals("li.cil.oc.client.ScreenBlockEntityRenderer", Class.forName("li.cil.oc.client.ScreenBlockEntityRenderer").getName());
    }

    @Test
    void clientRegistersBlockEntityRenderers() throws NoSuchMethodException {
        Method method = NeoOpenComputersClient.class.getDeclaredMethod("registerEntityRenderers", EntityRenderersEvent.RegisterRenderers.class);

        assertTrue(Modifier.isStatic(method.getModifiers()));
    }

    @Test
    void clientRegistersGuiLayers() throws NoSuchMethodException {
        Method method = NeoOpenComputersClient.class.getDeclaredMethod("registerGuiLayers", RegisterGuiLayersEvent.class);

        assertTrue(Modifier.isStatic(method.getModifiers()));
    }
}
