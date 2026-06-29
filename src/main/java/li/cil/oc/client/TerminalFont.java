package li.cil.oc.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.util.FontWidths;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

final class TerminalFont {
    private static final int SOURCE_WIDTH = 8;
    private static final int SOURCE_HEIGHT = 16;
    private static final int CELL_WIDTH = 4;
    private static final int CELL_HEIGHT = 8;
    private static final int ATLAS_COLUMNS = 16;
    private static final int ATLAS_ROWS = 16;
    private static final int ATLAS_CELL_WIDTH = CELL_WIDTH;
    private static final int ATLAS_CELL_HEIGHT = CELL_HEIGHT;
    private static final int ATLAS_WIDTH = ATLAS_COLUMNS * ATLAS_CELL_WIDTH;
    private static final int ATLAS_HEIGHT = ATLAS_ROWS * ATLAS_CELL_HEIGHT;
    private static final ResourceLocation ASCII_GLYPH_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "dynamic/ascii_terminal_font");
    private static final int WORLD_GLYPH_LIGHT = LightTexture.FULL_BRIGHT;
    private static final Map<Integer, Glyph> GLYPHS = loadGlyphs();
    private static boolean asciiGlyphTextureRegistered;

    private TerminalFont() {
    }

    static int cellWidth() {
        return CELL_WIDTH;
    }

    static int cellHeight() {
        return CELL_HEIGHT;
    }

    static float worldPixelScale() {
        return CELL_WIDTH / (float) SOURCE_WIDTH;
    }

    static boolean hasGlyph(final int codePoint) {
        return GLYPHS.containsKey(codePoint);
    }

    static int glyphCellWidth(final int codePoint) {
        final Glyph glyph = glyph(codePoint);
        return Math.max(CELL_WIDTH, (glyph.sourceWidth() / SOURCE_WIDTH) * CELL_WIDTH);
    }

    static int rowMask(final int codePoint, final int row) {
        int mask = 0;
        for (int x = 0; x < glyphCellWidth(codePoint); x++) {
            if (pixel(codePoint, x, row)) {
                mask |= 1 << (glyphCellWidth(codePoint) - 1 - x);
            }
        }
        return mask;
    }

    static void drawGuiCell(final GuiGraphics graphics, final String text, final int x, final int y, final int color) {
        final int codePoint = codePoint(text);
        if (codePoint == ' ') {
            return;
        }
        for (int py = 0; py < CELL_HEIGHT; py++) {
            for (int px = 0; px < glyphCellWidth(codePoint); px++) {
                if (pixel(codePoint, px, py)) {
                    graphics.fill(x + px, y + py, x + px + 1, y + py + 1, color);
                }
            }
        }
    }

    static void drawWorldCell(
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final String text,
        final int column,
        final int row,
        final int color,
        final float z) {
        if ((color & 0xFF000000) == 0) {
            return;
        }
        final int codePoint = codePoint(text);
        if (codePoint == ' ') {
            return;
        }
        if (drawWorldTexturedAsciiCell(poseStack, bufferSource, codePoint, column, row, color, z)) {
            return;
        }
        final VertexConsumer consumer = bufferSource.getBuffer(RenderType.gui());
        final PoseStack.Pose pose = poseStack.last();
        final float baseX = column * CELL_WIDTH;
        final float baseY = row * CELL_HEIGHT;
        for (int py = 0; py < CELL_HEIGHT; py++) {
            for (int px = 0; px < glyphCellWidth(codePoint); px++) {
                if (pixel(codePoint, px, py)) {
                    quad(consumer, pose, baseX + px, baseY + py, z, color);
                }
            }
        }
    }

    static boolean usesWorldTexturedAsciiGlyphs() {
        return true;
    }

    private static boolean drawWorldTexturedAsciiCell(
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int codePoint,
        final int column,
        final int row,
        final int color,
        final float z) {
        final Glyph glyph = glyph(codePoint);
        if (glyph == null || codePoint < 0 || codePoint > 0xFF) {
            return false;
        }
        registerAsciiGlyphTexture();
        final int atlasColumn = codePoint % ATLAS_COLUMNS;
        final int atlasRow = codePoint / ATLAS_COLUMNS;
        final float minU = (atlasColumn * ATLAS_CELL_WIDTH) / (float) ATLAS_WIDTH;
        final float maxU = (atlasColumn * ATLAS_CELL_WIDTH + glyphCellWidth(codePoint)) / (float) ATLAS_WIDTH;
        final float minV = (atlasRow * ATLAS_CELL_HEIGHT) / (float) ATLAS_HEIGHT;
        final float maxV = (atlasRow * ATLAS_CELL_HEIGHT + CELL_HEIGHT) / (float) ATLAS_HEIGHT;
        final float x = column * CELL_WIDTH;
        final float y = row * CELL_HEIGHT;
        final float width = glyphCellWidth(codePoint);
        final float height = CELL_HEIGHT;
        final VertexConsumer consumer = bufferSource.getBuffer(RenderType.text(ASCII_GLYPH_TEXTURE));
        final PoseStack.Pose pose = poseStack.last();
        texturedQuadVertex(consumer, pose, x, y + height, z, color, minU, maxV);
        texturedQuadVertex(consumer, pose, x + width, y + height, z, color, maxU, maxV);
        texturedQuadVertex(consumer, pose, x + width, y, z, color, maxU, minV);
        texturedQuadVertex(consumer, pose, x, y, z, color, minU, minV);
        return true;
    }

    private static void texturedQuadVertex(
        final VertexConsumer consumer,
        final PoseStack.Pose pose,
        final float x,
        final float y,
        final float z,
        final int color,
        final float u,
        final float v) {
        consumer.addVertex(pose, x, y, z)
            .setColor(color)
            .setUv(u, v)
            .setUv2(WORLD_GLYPH_LIGHT & 0xFFFF, WORLD_GLYPH_LIGHT >> 16 & 0xFFFF);
    }

    private static void registerAsciiGlyphTexture() {
        if (asciiGlyphTextureRegistered) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        final NativeImage image = new NativeImage(ATLAS_WIDTH, ATLAS_HEIGHT, true);
        for (int codePoint = 0; codePoint <= 0xFF; codePoint++) {
            final Glyph glyph = glyph(codePoint);
            if (glyph == null) {
                continue;
            }
            final int atlasX = (codePoint % ATLAS_COLUMNS) * ATLAS_CELL_WIDTH;
            final int atlasY = (codePoint / ATLAS_COLUMNS) * ATLAS_CELL_HEIGHT;
            for (int y = 0; y < CELL_HEIGHT; y++) {
                for (int x = 0; x < glyphCellWidth(codePoint); x++) {
                    if (pixel(codePoint, x, y)) {
                        image.setPixelRGBA(atlasX + x, atlasY + y, 0xFFFFFFFF);
                    }
                }
            }
        }
        final DynamicTexture texture = new DynamicTexture(image);
        texture.setFilter(false, false);
        texture.upload();
        minecraft.getTextureManager().register(ASCII_GLYPH_TEXTURE, texture);
        asciiGlyphTextureRegistered = true;
    }

    private static void quad(final VertexConsumer consumer, final PoseStack.Pose pose, final float x, final float y, final float z, final int color) {
        quad(consumer, pose, x, y, z, color, 1F);
    }

    private static void quad(final VertexConsumer consumer, final PoseStack.Pose pose, final float x, final float y, final float z, final int color, final float size) {
        consumer.addVertex(pose, x, y + size, z).setColor(color);
        consumer.addVertex(pose, x + size, y + size, z).setColor(color);
        consumer.addVertex(pose, x + size, y, z).setColor(color);
        consumer.addVertex(pose, x, y, z).setColor(color);
    }

    private static boolean pixel(final int codePoint, final int x, final int y) {
        final Glyph glyph = glyph(codePoint);
        if (glyph == null) {
            return false;
        }
        final int targetWidth = Math.max(CELL_WIDTH, (glyph.sourceWidth() / SOURCE_WIDTH) * CELL_WIDTH);
        final int sourceX = Math.min(glyph.sourceWidth() - 1, (x * glyph.sourceWidth()) / targetWidth);
        final int sourceY = Math.min(SOURCE_HEIGHT - 1, (y * SOURCE_HEIGHT) / CELL_HEIGHT);
        return sourcePixel(glyph, sourceX, sourceY);
    }

    private static boolean sourcePixel(final Glyph glyph, final int sourceX, final int sourceY) {
        return (glyph.rows()[sourceY] & (1 << (glyph.sourceWidth() - 1 - sourceX))) != 0;
    }

    private static int codePoint(final String text) {
        if (text == null || text.isEmpty()) {
            return ' ';
        }
        return text.codePointAt(0);
    }

    private static Glyph glyph(final int codePoint) {
        return GLYPHS.getOrDefault(codePoint, GLYPHS.get((int) '?'));
    }

    private static Map<Integer, Glyph> loadGlyphs() {
        final Map<Integer, Glyph> glyphs = new HashMap<>();
        try (InputStream stream = TerminalFont.class.getResourceAsStream("/assets/neoopencomputers/font.hex")) {
            if (stream == null) {
                return glyphs;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    final int separator = line.indexOf(':');
                    if (separator <= 0) {
                        continue;
                    }
                    final String hex = line.substring(separator + 1).trim();
                    if (hex.length() < SOURCE_HEIGHT * 2) {
                        continue;
                    }
                    final int codePoint = Integer.parseInt(line.substring(0, separator), 16);
                    final int bytesPerRow = (hex.length() / 2) / SOURCE_HEIGHT;
                    if (bytesPerRow < 1 || bytesPerRow > 2 || bytesPerRow != FontWidths.wcwidth(codePoint)) {
                        continue;
                    }
                    final int[] rows = new int[SOURCE_HEIGHT];
                    for (int row = 0; row < SOURCE_HEIGHT; row++) {
                        final int start = row * bytesPerRow * 2;
                        final int end = start + bytesPerRow * 2;
                        rows[row] = Integer.parseInt(hex.substring(start, end), 16);
                    }
                    glyphs.putIfAbsent(codePoint, new Glyph(bytesPerRow * SOURCE_WIDTH, rows));
                }
            }
        } catch (final IOException | NumberFormatException ignored) {
            return Map.of();
        }
        return Map.copyOf(glyphs);
    }

    private record Glyph(int sourceWidth, int[] rows) {
    }
}
