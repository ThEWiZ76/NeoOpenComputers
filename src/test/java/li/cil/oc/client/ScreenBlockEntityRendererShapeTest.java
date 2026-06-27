package li.cil.oc.client;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
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
import java.util.List;

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
    void screenRendererUsesUpstreamConnectedScreenOriginAndDimensions() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/ScreenBlockEntityRenderer.java"));
        final String screen = Files.readString(Path.of("src/main/java/li/cil/oc/common/blockentity/ScreenBlockEntity.java"));

        assertTrue(renderer.contains("isRenderOrigin()"), "Only the connected screen origin should render text");
        assertTrue(renderer.contains("renderBlockWidth()"), "Renderer should scale text to connected screen width");
        assertTrue(renderer.contains("renderBlockHeight()"), "Renderer should scale text to connected screen height");
        assertTrue(screen.contains("isRenderOrigin()"), "Screen block entity should expose connected origin state");
        assertTrue(screen.contains("renderBlockWidth()"), "Screen block entity should expose connected block width");
        assertTrue(screen.contains("renderBlockHeight()"), "Screen block entity should expose connected block height");
        assertTrue(screen.contains("localBlockX()"), "Screen block entity should expose connected local x");
        assertTrue(screen.contains("localBlockY()"), "Screen block entity should expose connected local y");
    }

    @Test
    void screenRendererSelectsSourceFrontTextureForConnectedScreens() {
        assertEquals("neoopencomputers:block/screen/f", ScreenBlockEntityRenderer.screenFrontTexture(false, 1, 1, 0, 0).toString());
        assertEquals("neoopencomputers:block/screen/f2", ScreenBlockEntityRenderer.screenFrontTexture(true, 1, 1, 0, 0).toString());
        assertEquals("neoopencomputers:block/screen/ftr", ScreenBlockEntityRenderer.screenFrontTexture(false, 3, 3, 2, 2).toString());
        assertEquals("neoopencomputers:block/screen/fmm", ScreenBlockEntityRenderer.screenFrontTexture(false, 3, 3, 1, 1).toString());
        assertEquals("neoopencomputers:block/screen/fbl2", ScreenBlockEntityRenderer.screenFrontTexture(false, 3, 3, 0, 0).toString());
        assertEquals("neoopencomputers:block/screen/fbl", ScreenBlockEntityRenderer.screenFrontTexture(true, 3, 3, 0, 0).toString());
        assertEquals("neoopencomputers:block/screen/fvm", ScreenBlockEntityRenderer.screenFrontTexture(false, 1, 3, 0, 1).toString());
        assertEquals("neoopencomputers:block/screen/fhm", ScreenBlockEntityRenderer.screenFrontTexture(true, 3, 1, 1, 0).toString());
    }

    @Test
    void screenRendererDrawsConnectedFrontOverlayClearlyInFrontOfStaticModel() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/ScreenBlockEntityRenderer.java"));

        assertTrue(renderer.contains("SCREEN_FRONT_Z"));
        assertTrue(renderer.contains("0.53F"));
        assertTrue(!renderer.contains("-0.506F"));
    }

    @Test
    void screenRendererDrawsTerminalTextInFrontOfScreenFace() {
        assertTrue(ScreenBlockEntityRenderer.screenTextZ() > ScreenBlockEntityRenderer.screenFrontZ());
    }

    @Test
    void screenRendererCentersNarrowGlyphsInsideFixedCells() {
        assertEquals(2, ScreenBlockEntityRenderer.centeredCellOffset(1));
        assertEquals(0, ScreenBlockEntityRenderer.centeredCellOffset(6));
    }

    @Test
    void screenRendererKeepsNorthFrontOnNorthFace() {
        assertEquals(0, ScreenBlockEntityRenderer.yawRotationDegrees(Direction.NORTH));
        assertEquals(180, ScreenBlockEntityRenderer.yawRotationDegrees(Direction.SOUTH));
        assertEquals(90, ScreenBlockEntityRenderer.yawRotationDegrees(Direction.EAST));
        assertEquals(-90, ScreenBlockEntityRenderer.yawRotationDegrees(Direction.WEST));
    }

    @Test
    void screenRendererSplitsTextIntoFixedCellsForWorldAlignment() {
        assertEquals(List.of("W", "i", "."), ScreenBlockEntityRenderer.lineCells("Wi.", 3));
        assertEquals(List.of("W", " ", " "), ScreenBlockEntityRenderer.lineCells("W", 3));
    }

    @Test
    void screenRendererExpandsBoundsForMultiblockText() {
        assertEquals(
            new AABB(10, 20, 30, 13, 21, 31),
            ScreenBlockEntityRenderer.renderBounds(new BlockPos(10, 20, 30), Direction.EAST, Direction.UP, 3, 1));
        assertEquals(
            new AABB(8, 17, 30, 11, 21, 31),
            ScreenBlockEntityRenderer.renderBounds(new BlockPos(10, 20, 30), Direction.WEST, Direction.DOWN, 3, 4));
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
