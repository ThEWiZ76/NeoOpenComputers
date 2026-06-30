package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.util.FontWidths;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

final class TerminalFont {
    private static final int HEX_SOURCE_WIDTH = 8;
    private static final int HEX_SOURCE_HEIGHT = 16;
    private static final int TEXTURE_SOURCE_WIDTH = 10;
    private static final int CELL_WIDTH = HEX_SOURCE_WIDTH;
    private static final int CELL_HEIGHT = HEX_SOURCE_HEIGHT;
    private static final int WORLD_ATLAS_COLUMNS = 16;
    private static final int WORLD_ATLAS_ROWS = 16;
    private static final int WORLD_ATLAS_WIDTH = WORLD_ATLAS_COLUMNS * CELL_WIDTH;
    private static final int WORLD_ATLAS_HEIGHT = WORLD_ATLAS_ROWS * CELL_HEIGHT;
    private static final ResourceLocation ASCII_GLYPH_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "textures/font/terminal_hex_cp437.png");
    private static final int WORLD_GLYPH_LIGHT = LightTexture.FULL_BRIGHT;
    private static final Map<Integer, Glyph> GLYPHS = loadGlyphs();
    private static final Map<Integer, Integer> WORLD_TEXTURE_GLYPHS = loadWorldTextureGlyphs();

    private TerminalFont() {
    }

    static int cellWidth() {
        return CELL_WIDTH;
    }

    static int cellHeight() {
        return CELL_HEIGHT;
    }

    static float worldPixelScale() {
        return CELL_WIDTH / (float) TEXTURE_SOURCE_WIDTH;
    }

    static boolean hasGlyph(final int codePoint) {
        return GLYPHS.containsKey(codePoint);
    }

    static int glyphCellWidth(final int codePoint) {
        final Glyph glyph = glyph(codePoint);
        return targetWidth(glyph);
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
        final Integer atlasIndex = WORLD_TEXTURE_GLYPHS.get(codePoint);
        if (atlasIndex != null && glyphCellWidth(codePoint) == CELL_WIDTH) {
            drawWorldTexturedCell(poseStack, bufferSource, atlasIndex, column, row, color, z);
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

    private static void drawWorldTexturedCell(
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int atlasIndex,
        final int column,
        final int row,
        final int color,
        final float z) {
        final VertexConsumer consumer = bufferSource.getBuffer(RenderType.text(ASCII_GLYPH_TEXTURE));
        final PoseStack.Pose pose = poseStack.last();
        final float x = column * CELL_WIDTH;
        final float y = row * CELL_HEIGHT;
        final float u0 = (atlasIndex % WORLD_ATLAS_COLUMNS) * CELL_WIDTH / (float) WORLD_ATLAS_WIDTH;
        final float v0 = (atlasIndex / WORLD_ATLAS_COLUMNS) * CELL_HEIGHT / (float) WORLD_ATLAS_HEIGHT;
        final float u1 = u0 + CELL_WIDTH / (float) WORLD_ATLAS_WIDTH;
        final float v1 = v0 + CELL_HEIGHT / (float) WORLD_ATLAS_HEIGHT;
        texturedQuad(consumer, pose, x, y, x + CELL_WIDTH, y + CELL_HEIGHT, z, color, u0, v0, u1, v1);
    }

    private static void texturedQuad(
        final VertexConsumer consumer,
        final PoseStack.Pose pose,
        final float x0,
        final float y0,
        final float x1,
        final float y1,
        final float z,
        final int color,
        final float u0,
        final float v0,
        final float u1,
        final float v1) {
        consumer.addVertex(pose, x0, y1, z).setColor(color).setUv(u0, v1).setUv2(WORLD_GLYPH_LIGHT & 0xFFFF, WORLD_GLYPH_LIGHT >> 16 & 0xFFFF);
        consumer.addVertex(pose, x1, y1, z).setColor(color).setUv(u1, v1).setUv2(WORLD_GLYPH_LIGHT & 0xFFFF, WORLD_GLYPH_LIGHT >> 16 & 0xFFFF);
        consumer.addVertex(pose, x1, y0, z).setColor(color).setUv(u1, v0).setUv2(WORLD_GLYPH_LIGHT & 0xFFFF, WORLD_GLYPH_LIGHT >> 16 & 0xFFFF);
        consumer.addVertex(pose, x0, y0, z).setColor(color).setUv(u0, v0).setUv2(WORLD_GLYPH_LIGHT & 0xFFFF, WORLD_GLYPH_LIGHT >> 16 & 0xFFFF);
    }

    private static void quad(final VertexConsumer consumer, final PoseStack.Pose pose, final float x, final float y, final float z, final int color) {
        quad(consumer, pose, x, y, z, color, 1F);
    }

    private static void quad(final VertexConsumer consumer, final PoseStack.Pose pose, final float x, final float y, final float z, final int color, final float size) {
        consumer.addVertex(pose, x, y + size, z).setColor(color).setUv2(WORLD_GLYPH_LIGHT & 0xFFFF, WORLD_GLYPH_LIGHT >> 16 & 0xFFFF);
        consumer.addVertex(pose, x + size, y + size, z).setColor(color).setUv2(WORLD_GLYPH_LIGHT & 0xFFFF, WORLD_GLYPH_LIGHT >> 16 & 0xFFFF);
        consumer.addVertex(pose, x + size, y, z).setColor(color).setUv2(WORLD_GLYPH_LIGHT & 0xFFFF, WORLD_GLYPH_LIGHT >> 16 & 0xFFFF);
        consumer.addVertex(pose, x, y, z).setColor(color).setUv2(WORLD_GLYPH_LIGHT & 0xFFFF, WORLD_GLYPH_LIGHT >> 16 & 0xFFFF);
    }

    private static boolean pixel(final int codePoint, final int x, final int y) {
        final Glyph glyph = glyph(codePoint);
        if (glyph == null) {
            return false;
        }
        final int width = targetWidth(glyph);
        final int sourceX0 = x * glyph.sourceWidth() / width;
        final int sourceX1 = Math.min(glyph.sourceWidth() - 1, ((x + 1) * glyph.sourceWidth() + width - 1) / width - 1);
        final int sourceY0 = y * glyph.sourceHeight() / CELL_HEIGHT;
        final int sourceY1 = Math.min(glyph.sourceHeight() - 1, ((y + 1) * glyph.sourceHeight() + CELL_HEIGHT - 1) / CELL_HEIGHT - 1);
        for (int sourceY = sourceY0; sourceY <= sourceY1; sourceY++) {
            for (int sourceX = sourceX0; sourceX <= sourceX1; sourceX++) {
                if (sourcePixel(glyph, sourceX, sourceY)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int targetWidth(final Glyph glyph) {
        return Math.max(CELL_WIDTH, (glyph.sourceWidth() / glyph.sourceCellWidth()) * CELL_WIDTH);
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
        loadHexGlyphs(glyphs);
        loadTextureGlyphs(glyphs);
        return Map.copyOf(glyphs);
    }

    private static Map<Integer, Integer> loadWorldTextureGlyphs() {
        try (InputStream stream = TerminalFont.class.getResourceAsStream("/assets/neoopencomputers/textures/font/chars.txt")) {
            if (stream == null) {
                return Map.of();
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                final String chars = reader.readLine();
                if (chars == null) {
                    return Map.of();
                }
                final Map<Integer, Integer> glyphs = new HashMap<>();
                int index = 0;
                for (int offset = 0; offset < chars.length() && index < WORLD_ATLAS_COLUMNS * WORLD_ATLAS_ROWS; offset = chars.offsetByCodePoints(offset, 1), index++) {
                    final int codePoint = chars.codePointAt(offset);
                    if (hasGlyph(codePoint) && glyphCellWidth(codePoint) == CELL_WIDTH) {
                        glyphs.putIfAbsent(codePoint, index);
                    }
                }
                return Map.copyOf(glyphs);
            }
        } catch (final IOException ignored) {
            return Map.of();
        }
    }

    private static void loadTextureGlyphs(final Map<Integer, Glyph> glyphs) {
        try (
            InputStream metadataStream = TerminalFont.class.getResourceAsStream("/assets/neoopencomputers/textures/font/chars.txt");
            InputStream imageStream = TerminalFont.class.getResourceAsStream("/assets/neoopencomputers/textures/font/chars_aliased.png")) {
            if (metadataStream == null || imageStream == null) {
                return;
            }
            final BufferedImage image = ImageIO.read(imageStream);
            if (image == null) {
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(metadataStream, StandardCharsets.UTF_8))) {
                final String chars = reader.readLine();
                final String size = reader.readLine();
                if (chars == null || size == null) {
                    return;
                }
                final String[] parts = size.trim().split(" ", 2);
                final int sourceWidth = Integer.parseInt(parts[0]);
                final int sourceHeight = Integer.parseInt(parts[1]);
                final int columns = Math.max(1, image.getWidth() / sourceWidth);
                int index = 0;
                for (int offset = 0; offset < chars.length(); offset = chars.offsetByCodePoints(offset, 1), index++) {
                    final int codePoint = chars.codePointAt(offset);
                    final int atlasX = index % columns * sourceWidth;
                    final int atlasY = index / columns * (sourceHeight + 1);
                    if (atlasX + sourceWidth > image.getWidth() || atlasY + sourceHeight > image.getHeight()) {
                        break;
                    }
                    final int[] rows = new int[sourceHeight];
                    for (int row = 0; row < sourceHeight; row++) {
                        int mask = 0;
                        for (int x = 0; x < sourceWidth; x++) {
                            final int alpha = image.getRGB(atlasX + x, atlasY + row) >>> 24;
                            if (alpha > 0) {
                                mask |= 1 << (sourceWidth - 1 - x);
                            }
                        }
                        rows[row] = mask;
                    }
                    if (hasPixels(rows)) {
                        glyphs.putIfAbsent(codePoint, new Glyph(sourceWidth, sourceHeight, sourceWidth, rows));
                    }
                }
            }
        } catch (final IOException | NumberFormatException ignored) {
            // Fall back to the bundled hex font below.
        }
    }

    private static void loadHexGlyphs(final Map<Integer, Glyph> glyphs) {
        try (InputStream stream = TerminalFont.class.getResourceAsStream("/assets/neoopencomputers/font.hex")) {
            if (stream == null) {
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    final int separator = line.indexOf(':');
                    if (separator <= 0) {
                        continue;
                    }
                    final String hex = line.substring(separator + 1).trim();
                    if (hex.length() < HEX_SOURCE_HEIGHT * 2) {
                        continue;
                    }
                    final int codePoint = Integer.parseInt(line.substring(0, separator), 16);
                    final int bytesPerRow = (hex.length() / 2) / HEX_SOURCE_HEIGHT;
                    if (bytesPerRow < 1 || bytesPerRow > 2 || bytesPerRow != FontWidths.wcwidth(codePoint)) {
                        continue;
                    }
                    final int[] rows = new int[HEX_SOURCE_HEIGHT];
                    for (int row = 0; row < HEX_SOURCE_HEIGHT; row++) {
                        final int start = row * bytesPerRow * 2;
                        final int end = start + bytesPerRow * 2;
                        rows[row] = Integer.parseInt(hex.substring(start, end), 16);
                    }
                    glyphs.putIfAbsent(codePoint, new Glyph(bytesPerRow * HEX_SOURCE_WIDTH, HEX_SOURCE_HEIGHT, HEX_SOURCE_WIDTH, rows));
                }
            }
        } catch (final IOException | NumberFormatException ignored) {
            // Keep any texture glyphs loaded above.
        }
    }

    private static boolean hasPixels(final int[] rows) {
        for (final int row : rows) {
            if (row != 0) {
                return true;
            }
        }
        return false;
    }

    private record Glyph(int sourceWidth, int sourceHeight, int sourceCellWidth, int[] rows) {
    }
}
