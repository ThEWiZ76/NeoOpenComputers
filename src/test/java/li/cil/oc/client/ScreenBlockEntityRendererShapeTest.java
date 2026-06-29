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
    void screenRendererFlipsConnectedTexturePartsLikeUpstreamForDownFaces() {
        assertTrue(ScreenBlockEntityRenderer.shouldFlipTextureParts(Direction.DOWN, Direction.SOUTH));
        assertTrue(ScreenBlockEntityRenderer.shouldFlipTextureParts(Direction.EAST, Direction.DOWN));
        assertTrue(!ScreenBlockEntityRenderer.shouldFlipTextureParts(Direction.DOWN, Direction.DOWN));
        assertEquals("neoopencomputers:block/screen/bml", ScreenBlockEntityRenderer.screenTexture(false, 3, 3, 0, 1, Direction.NORTH, true).toString());
    }

    @Test
    void screenRendererRotatesUpAndDownTextureUvsLikeUpstream() {
        assertEquals(0, ScreenBlockEntityRenderer.screenTextureRotationSteps(Direction.UP, Direction.SOUTH));
        assertEquals(1, ScreenBlockEntityRenderer.screenTextureRotationSteps(Direction.UP, Direction.WEST));
        assertEquals(2, ScreenBlockEntityRenderer.screenTextureRotationSteps(Direction.UP, Direction.NORTH));
        assertEquals(3, ScreenBlockEntityRenderer.screenTextureRotationSteps(Direction.UP, Direction.EAST));
        assertEquals(-3, ScreenBlockEntityRenderer.screenTextureRotationSteps(Direction.DOWN, Direction.EAST));
        assertEquals(0, ScreenBlockEntityRenderer.screenTextureRotationSteps(Direction.NORTH, Direction.EAST));
        assertEquals(new ScreenBlockEntityRenderer.TextureUv(0F, 1F), ScreenBlockEntityRenderer.rotatedUv(0F, 0F, 1));
        assertEquals(new ScreenBlockEntityRenderer.TextureUv(1F, 0F), ScreenBlockEntityRenderer.rotatedUv(0F, 0F, -1));
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
    void screenRendererLeavesOpaqueBodyFacesToStaticBlockModel() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/ScreenBlockEntityRenderer.java"));

        assertTrue(!renderer.contains("for (final Direction face : Direction.values())"),
            "Block-entity renderer must not redraw side/back/top/bottom cutout overlays over the static opaque model.");
        assertTrue(renderer.contains("Direction.SOUTH"),
            "Block-entity renderer should only draw the connected front overlay; screen_panel.json owns opaque body faces.");
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
    void screenRendererAppliesWorldTextZOnlyOnce() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/ScreenBlockEntityRenderer.java"));

        assertTrue(renderer.contains("poseStack.translate(layout.x(), layout.y(), SCREEN_TEXT_Z)"));
        assertTrue(renderer.contains("TerminalFont.drawWorldCell(poseStack, bufferSource, cell, column, row, textColor, 0F)"),
            "World glyph quads should use local Z after the text pose has been moved to the screen face.");
        assertTrue(renderer.contains("localBackgroundZ(layout.scale())"),
            "World background quads should compensate for text scale instead of collapsing onto glyph depth.");
    }

    @Test
    void screenRendererKeepsReadableDepthGapAfterScaling() {
        final float singleScale = ScreenBlockEntityRenderer.textScale(1, 1, 50, 16);
        final float wallScale = ScreenBlockEntityRenderer.textScale(3, 2, 80, 25);

        assertEquals(0.002F, -ScreenBlockEntityRenderer.localBackgroundZ(singleScale) * singleScale, 0.000_001F);
        assertEquals(0.002F, -ScreenBlockEntityRenderer.localBackgroundZ(wallScale) * wallScale, 0.000_001F);
        assertTrue(ScreenBlockEntityRenderer.screenTextZ() + ScreenBlockEntityRenderer.localBackgroundZ(singleScale) * singleScale > ScreenBlockEntityRenderer.screenFrontZ());
        assertTrue(ScreenBlockEntityRenderer.screenTextZ() + ScreenBlockEntityRenderer.localBackgroundZ(wallScale) * wallScale > ScreenBlockEntityRenderer.screenFrontZ());
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
        assertTrue(ScreenBlockEntityRenderer.shouldRenderCellBackground(0xFF000000), "World terminal must paint black background cells over the screen block texture");
        assertTrue(ScreenBlockEntityRenderer.shouldRenderCellBackground(0xFF112233), "World terminal must paint colored background cells");
        assertTrue(!ScreenBlockEntityRenderer.shouldRenderCellBackground(0x00000000), "Fully transparent background cells should be skipped");
        assertEquals(0x00000000, ScreenBlockEntityRenderer.textColorWithAlpha(0x112233, 0F));
    }

    @Test
    void screenRendererScalesTerminalTextToInnerScreenArea() {
        final float singleScale = ScreenBlockEntityRenderer.textScale(1, 1, 50, 16);
        assertTrue(50 * 4 * singleScale < 0.75F, "Single-screen text should fit inside the screen border");

        final float wallScale = ScreenBlockEntityRenderer.textScale(3, 2, 80, 25);
        assertTrue(80 * 4 * wallScale < 2.75F, "Multiblock text should fit inside the wall border");
        assertTrue(25 * 8 * wallScale < 1.75F, "Multiblock text should fit vertically inside the wall border");
    }

    @Test
    void screenRendererPlacesTerminalTextInsideScreenFace() {
        final ScreenBlockEntityRenderer.TextLayout single = ScreenBlockEntityRenderer.textLayout(1, 1, 50, 16);
        assertTrue(single.x() > -0.5F && single.x() < 0.5F);
        assertTrue(single.y() > -0.5F && single.y() < 0.5F);
        assertTrue(single.y() - 16 * 8 * single.scale() > -0.5F);

        final ScreenBlockEntityRenderer.TextLayout wall = ScreenBlockEntityRenderer.textLayout(3, 2, 80, 25);
        assertTrue(wall.x() > -0.5F && wall.x() < 2.5F);
        assertTrue(wall.y() > -0.5F && wall.y() < 1.5F);
        assertTrue(wall.y() - 25 * 8 * wall.scale() > -0.5F);
    }

    @Test
    void screenRendererDrawsGlyphsAtFixedCellOriginForMonospaceAlignment() {
        assertEquals(0, ScreenBlockEntityRenderer.centeredCellOffset(1));
        assertEquals(0, ScreenBlockEntityRenderer.centeredCellOffset(6));
    }

    @Test
    void screenRendererUsesUpstreamHexFontCellMetricsForWorldText() throws IOException {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/ScreenBlockEntityRenderer.java"));

        assertTrue(Files.exists(Path.of("src/main/resources/assets/neoopencomputers/font.hex")));
        assertTrue(TerminalFont.hasGlyph('i'));
        assertTrue(TerminalFont.hasGlyph('W'));
        assertTrue(TerminalFont.hasGlyph(0x754C));
        assertEquals(4, TerminalFont.cellWidth());
        assertEquals(8, TerminalFont.cellHeight());
        assertEquals(0.5F, TerminalFont.worldPixelScale());
        assertEquals(4, TerminalFont.glyphCellWidth('i'));
        assertEquals(8, TerminalFont.glyphCellWidth(0x754C));
        assertEquals(1, Integer.bitCount(TerminalFont.rowMask('i', 1)), "Thin glyphs should stay sparse after 8x16 to 4x8 scaling");
        assertTrue(Integer.bitCount(TerminalFont.rowMask('i', 6)) >= 1, "Lower pixels in thin glyphs should not be center-sampled away");
        assertTrue(Integer.bitCount(TerminalFont.rowMask('i', 3)) <= 2, "Thin glyphs should not be expanded into blocky coverage buckets");
        assertTrue(renderer.contains("TerminalFont.drawWorldCell"), "World screen text should render fixed bitmap cells, not proportional Minecraft glyphs");
        assertTrue(renderer.contains("renderTerminalBackgrounds"), "World screen text should draw all cell backgrounds before wide glyphs");
        assertTrue(renderer.contains("renderTerminalGlyphs"), "World screen text should draw all glyphs after cell backgrounds");

        final String font = Files.readString(Path.of("src/main/java/li/cil/oc/client/TerminalFont.java"));
        assertTrue(font.contains("py < CELL_HEIGHT"), "World text should render stable 4x8 terminal cells, not tiny source-pixel quads");
        assertTrue(font.contains("px < glyphCellWidth(codePoint)"), "World text should use the same fixed cell raster as the GUI terminal");
        assertTrue(!font.contains("baseX + sourceX * scale"),
            "Source-pixel quads become sub-pixel world geometry and make close screen text unreadable.");
    }

    @Test
    void texturedWorldAsciiAtlasUsesTerminalCellRaster() throws IOException {
        final String font = Files.readString(Path.of("src/main/java/li/cil/oc/client/TerminalFont.java"));

        assertTrue(font.contains("ATLAS_CELL_WIDTH = CELL_WIDTH"),
            "World ASCII atlas must store the same fixed-width terminal cells as the GUI renderer.");
        assertTrue(font.contains("ATLAS_CELL_HEIGHT = CELL_HEIGHT"),
            "World ASCII atlas must store downsampled 4x8 cells, not 8x16 source glyphs.");
        assertTrue(font.contains("image.setPixelRGBA(atlasX + x, atlasY + y, 0xFFFFFFFF)"));
        assertTrue(font.contains("pixel(codePoint, x, y)"),
            "The atlas should be rasterized through the GUI terminal sampling path to avoid GPU row dropping.");
        assertTrue(!font.contains("SOURCE_HEIGHT * worldPixelScale()"),
            "Textured world glyph quads should not squeeze 8x16 source glyphs into 4x8 cells at render time.");
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
    void printItemRendererFailsClosedOnLinkageErrors() throws IOException {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/client/PrintItemRenderer.java"));

        assertTrue(source.contains("catch (final LinkageError"), "Print item renderer should not crash creative tabs if print model linkage fails");
    }

    @Test
    void clientHandlesClientTickForNanomachineParticles() throws NoSuchMethodException {
        Method method = NeoOpenComputersClient.class.getDeclaredMethod("onClientTick", ClientTickEvent.Post.class);

        assertTrue(Modifier.isStatic(method.getModifiers()));
    }
}
