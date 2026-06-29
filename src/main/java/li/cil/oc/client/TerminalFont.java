package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.common.util.FontWidths;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

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
    private static final Map<Integer, Glyph> GLYPHS = loadGlyphs();

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
