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
    void screenRendererSelectsSourceSideTexturesForConnectedScreens() {
        assertEquals("neoopencomputers:block/screen/b2", ScreenBlockEntityRenderer.screenTexture(true, 1, 1, 0, 0, Direction.EAST).toString());
        assertEquals("neoopencomputers:block/screen/bhm", ScreenBlockEntityRenderer.screenTexture(false, 3, 1, 1, 0, Direction.DOWN).toString());
        assertEquals("neoopencomputers:block/screen/bvm", ScreenBlockEntityRenderer.screenTexture(false, 1, 3, 0, 1, Direction.WEST).toString());
        assertEquals("neoopencomputers:block/screen/bmm", ScreenBlockEntityRenderer.screenTexture(false, 3, 3, 1, 1, Direction.NORTH).toString());
        assertEquals("neoopencomputers:block/screen/bmr", ScreenBlockEntityRenderer.screenTexture(false, 3, 3, 0, 1, Direction.NORTH).toString());
    }

    @Test
    void screenRendererMapsLocalSideFacesToWorldDirectionsForCulling() {
        assertEquals(Direction.NORTH, ScreenBlockEntityRenderer.localFaceDirection(Direction.NORTH, Direction.EAST, Direction.EAST));
        assertEquals(Direction.SOUTH, ScreenBlockEntityRenderer.localFaceDirection(Direction.NORTH, Direction.EAST, Direction.WEST));
        assertEquals(Direction.UP, ScreenBlockEntityRenderer.localFaceDirection(Direction.NORTH, Direction.EAST, Direction.UP));
        assertEquals(Direction.DOWN, ScreenBlockEntityRenderer.localFaceDirection(Direction.NORTH, Direction.EAST, Direction.DOWN));
        assertEquals(Direction.EAST, ScreenBlockEntityRenderer.localFaceDirection(Direction.NORTH, Direction.EAST, Direction.SOUTH));
        assertEquals(Direction.WEST, ScreenBlockEntityRenderer.localFaceDirection(Direction.NORTH, Direction.EAST, Direction.NORTH));
    }

    @Test
    void screenRendererCullsOnlyInternalMultiblockSideFaces() {
        assertEquals(true, ScreenBlockEntityRenderer.canCullInternalMultiblockFace(Direction.EAST));
        assertEquals(true, ScreenBlockEntityRenderer.canCullInternalMultiblockFace(Direction.WEST));
        assertEquals(true, ScreenBlockEntityRenderer.canCullInternalMultiblockFace(Direction.UP));
        assertEquals(true, ScreenBlockEntityRenderer.canCullInternalMultiblockFace(Direction.DOWN));
        assertEquals(false, ScreenBlockEntityRenderer.canCullInternalMultiblockFace(Direction.SOUTH));
        assertEquals(false, ScreenBlockEntityRenderer.canCullInternalMultiblockFace(Direction.NORTH));
    }

    @Test
    void screenRendererOnlyDrawsDynamicFrontOverlayAndLeavesOpaqueBodyToBlockModel() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/ScreenBlockEntityRenderer.java"));

        assertTrue(!renderer.contains("Direction.values()"), "Block entity renderer must not bypass block-model culling for side/back screen faces");
        assertTrue(renderer.contains("renderScreenFace(screen, poseStack, bufferSource, packedLight, packedOverlay, Direction.SOUTH)"),
            "Only the visible front overlay should be drawn dynamically");
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
    void screenRendererOnlyDrawsTerminalTextForPlayersInFrontLikeUpstream() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/ScreenBlockEntityRenderer.java"));

        assertTrue(renderer.contains("shouldRenderTextForPlayer"), "Renderer should gate text visibility by player position");
        assertTrue(renderer.contains("playerIsInFrontOfScreen"), "Renderer should keep front-side visibility math testable");
        assertTrue(ScreenBlockEntityRenderer.playerIsInFrontOfScreen(Direction.EAST, new AABB(0, 0, 0, 1, 1, 1), 2, 0.5D, 0.5D));
        assertTrue(!ScreenBlockEntityRenderer.playerIsInFrontOfScreen(Direction.EAST, new AABB(0, 0, 0, 1, 1, 1), -2, 0.5D, 0.5D));
        assertTrue(ScreenBlockEntityRenderer.playerIsInFrontOfScreen(Direction.UP, new AABB(0, 0, 0, 1, 1, 1), 0.5D, 2, 0.5D));
        assertTrue(!ScreenBlockEntityRenderer.playerIsInFrontOfScreen(Direction.UP, new AABB(0, 0, 0, 1, 1, 1), 0.5D, -2, 0.5D));
    }

    @Test
    void screenRendererFadesWorldTextUsingUpstreamDistances() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/ScreenBlockEntityRenderer.java"));

        assertTrue(renderer.contains("screenTextAlpha"), "Renderer should compute alpha from upstream screen text fade settings");
        assertTrue(renderer.contains("screenTextDistanceSq"), "Renderer should use player distance to the connected screen bounds");
        assertTrue(renderer.contains("ModSettings.screenTextFadeStartDistance()"), "Renderer should use configured fade start distance");
        assertTrue(renderer.contains("ModSettings.maxScreenTextRenderDistance()"), "Renderer should use configured max render distance");
        assertEquals(1F, ScreenBlockEntityRenderer.screenTextAlpha(10D * 10D, 15D, 20D));
        assertEquals(0F, ScreenBlockEntityRenderer.screenTextAlpha(21D * 21D, 15D, 20D));
        final float faded = ScreenBlockEntityRenderer.screenTextAlpha(17D * 17D, 15D, 20D);
        assertTrue(faded > 0F && faded < 1F);
        assertEquals(4D, ScreenBlockEntityRenderer.screenTextDistanceSq(new AABB(0, 0, 0, 1, 1, 1), 3D, 0.5D, 0.5D), 0.000_001D);
    }

    @Test
    void screenRendererUsesPerCellTextBufferColorsLikeUpstream() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/ScreenBlockEntityRenderer.java"));

        assertTrue(renderer.contains("getForegroundColor(column, row)"), "World screen text should use each cell foreground color");
        assertTrue(renderer.contains("getBackgroundColor(column, row)"), "World screen text should render each cell background color");
        assertTrue(renderer.contains("renderCellBackground"), "Renderer should draw colored background cells before glyphs");
        assertEquals(0x80112233, ScreenBlockEntityRenderer.textColorWithAlpha(0x112233, 0.5F));
        assertEquals(0x00000000, ScreenBlockEntityRenderer.textColorWithAlpha(0x112233, 0F));
    }

    @Test
    void screenRendererScalesTerminalTextToInnerScreenArea() {
        final float singleScale = ScreenBlockEntityRenderer.textScale(1, 1, 50, 16);
        assertTrue(50 * 6 * singleScale < 0.75F, "Single-screen text should fit inside the screen border");

        final float wallScale = ScreenBlockEntityRenderer.textScale(3, 2, 80, 25);
        assertTrue(80 * 6 * wallScale < 2.75F, "Multiblock text should fit inside the wall border");
        assertTrue(25 * 9 * wallScale < 1.75F, "Multiblock text should fit vertically inside the wall border");
    }

    @Test
    void screenRendererPlacesTerminalTextInsideScreenFace() {
        final ScreenBlockEntityRenderer.TextLayout single = ScreenBlockEntityRenderer.textLayout(1, 1, 50, 16);
        assertTrue(single.x() > -0.5F && single.x() < 0.5F);
        assertTrue(single.y() > -0.5F && single.y() < 0.5F);
        assertTrue(single.y() - 16 * 9 * single.scale() > -0.5F);

        final ScreenBlockEntityRenderer.TextLayout wall = ScreenBlockEntityRenderer.textLayout(3, 2, 80, 25);
        assertTrue(wall.x() > -0.5F && wall.x() < 2.5F);
        assertTrue(wall.y() > -0.5F && wall.y() < 1.5F);
        assertTrue(wall.y() - 25 * 9 * wall.scale() > -0.5F);
    }

    @Test
    void screenRendererDrawsGlyphsAtFixedCellOriginForMonospaceAlignment() {
        assertEquals(0, ScreenBlockEntityRenderer.centeredCellOffset(1));
        assertEquals(0, ScreenBlockEntityRenderer.centeredCellOffset(6));
    }

    @Test
    void screenRendererUsesUpstreamTextPlaneYawConvention() {
        assertEquals(180, ScreenBlockEntityRenderer.yawRotationDegrees(Direction.NORTH));
        assertEquals(0, ScreenBlockEntityRenderer.yawRotationDegrees(Direction.SOUTH));
        assertEquals(90, ScreenBlockEntityRenderer.yawRotationDegrees(Direction.EAST));
        assertEquals(-90, ScreenBlockEntityRenderer.yawRotationDegrees(Direction.WEST));
    }

    @Test
    void screenRendererExposesFacingAndRightAxesForConnectedWall() {
        assertEquals(Direction.NORTH, ScreenBlockEntityRenderer.renderFrontDirection(Direction.NORTH));
        assertEquals(Direction.EAST, ScreenBlockEntityRenderer.renderFrontDirection(Direction.EAST));
        assertEquals(Direction.SOUTH, ScreenBlockEntityRenderer.renderFrontDirection(Direction.SOUTH));
        assertEquals(Direction.WEST, ScreenBlockEntityRenderer.renderFrontDirection(Direction.WEST));

        assertEquals(Direction.WEST, ScreenBlockEntityRenderer.renderRightDirection(Direction.NORTH));
        assertEquals(Direction.NORTH, ScreenBlockEntityRenderer.renderRightDirection(Direction.EAST));
        assertEquals(Direction.EAST, ScreenBlockEntityRenderer.renderRightDirection(Direction.SOUTH));
        assertEquals(Direction.SOUTH, ScreenBlockEntityRenderer.renderRightDirection(Direction.WEST));
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
