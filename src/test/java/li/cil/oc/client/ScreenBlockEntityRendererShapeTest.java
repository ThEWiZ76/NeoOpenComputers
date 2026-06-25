package li.cil.oc.client;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScreenBlockEntityRendererShapeTest {
    @Test
    void screenRendererClassExists() throws ClassNotFoundException {
        assertEquals("li.cil.oc.client.ScreenBlockEntityRenderer", Class.forName("li.cil.oc.client.ScreenBlockEntityRenderer").getName());
    }

    @Test
    void printItemRendererClassExists() throws ClassNotFoundException {
        Class<?> renderer = Class.forName("li.cil.oc.client.PrintItemRenderer", false, getClass().getClassLoader());

        assertEquals("li.cil.oc.client.PrintItemRenderer", renderer.getName());
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

    @Test
    void clientRegistersPrintItemExtensions() throws NoSuchMethodException {
        Method method = NeoOpenComputersClient.class.getDeclaredMethod("registerClientExtensions", RegisterClientExtensionsEvent.class);

        assertTrue(Modifier.isStatic(method.getModifiers()));
    }

    @Test
    void printItemModelUsesCustomRenderer() throws IOException {
        try (Reader reader = Files.newBufferedReader(Path.of("src/main/resources/assets/neoopencomputers/models/item/print.json"))) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            assertEquals("builtin/entity", json.get("parent").getAsString());
        }
    }

    @Test
    void clientHandlesClientTickForNanomachineParticles() throws NoSuchMethodException {
        Method method = NeoOpenComputersClient.class.getDeclaredMethod("onClientTick", ClientTickEvent.Post.class);

        assertTrue(Modifier.isStatic(method.getModifiers()));
    }
}
