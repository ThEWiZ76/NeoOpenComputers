package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
    private static final int CELL_WIDTH = 6;
    private static final int CELL_HEIGHT = 9;
    private static final Map<Integer, int[]> GLYPHS = loadGlyphs();

    private TerminalFont() {
    }

    static int cellWidth() {
        return CELL_WIDTH;
    }

    static int cellHeight() {
        return CELL_HEIGHT;
    }

    static boolean hasGlyph(final int codePoint) {
        return GLYPHS.containsKey(codePoint);
    }

    static void drawGuiCell(final GuiGraphics graphics, final String text, final int x, final int y, final int color) {
        final int codePoint = codePoint(text);
        if (codePoint == ' ') {
            return;
        }
        for (int py = 0; py < CELL_HEIGHT; py++) {
            for (int px = 0; px < CELL_WIDTH; px++) {
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
            for (int px = 0; px < CELL_WIDTH; px++) {
                if (pixel(codePoint, px, py)) {
                    quad(consumer, pose, baseX + px, baseY + py, z, color);
                }
            }
        }
    }

    private static void quad(final VertexConsumer consumer, final PoseStack.Pose pose, final float x, final float y, final float z, final int color) {
        consumer.addVertex(pose, x, y + 1, z).setColor(color);
        consumer.addVertex(pose, x + 1, y + 1, z).setColor(color);
        consumer.addVertex(pose, x + 1, y, z).setColor(color);
        consumer.addVertex(pose, x, y, z).setColor(color);
    }

    private static boolean pixel(final int codePoint, final int x, final int y) {
        final int[] glyph = GLYPHS.getOrDefault(codePoint, GLYPHS.get((int) '?'));
        if (glyph == null) {
            return false;
        }
        final int sourceX = Math.min(SOURCE_WIDTH - 1, (x * SOURCE_WIDTH + SOURCE_WIDTH / 2) / CELL_WIDTH);
        final int sourceY = Math.min(SOURCE_HEIGHT - 1, (y * SOURCE_HEIGHT + SOURCE_HEIGHT / 2) / CELL_HEIGHT);
        return (glyph[sourceY] & (0x80 >> sourceX)) != 0;
    }

    private static int codePoint(final String text) {
        if (text == null || text.isEmpty()) {
            return ' ';
        }
        return text.codePointAt(0);
    }

    private static Map<Integer, int[]> loadGlyphs() {
        final Map<Integer, int[]> glyphs = new HashMap<>();
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
                    final int[] rows = new int[SOURCE_HEIGHT];
                    for (int row = 0; row < SOURCE_HEIGHT; row++) {
                        rows[row] = Integer.parseInt(hex.substring(row * 2, row * 2 + 2), 16);
                    }
                    glyphs.putIfAbsent(codePoint, rows);
                }
            }
        } catch (final IOException | NumberFormatException ignored) {
            return Map.of();
        }
        return Map.copyOf(glyphs);
    }
}
