package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class TerminalFontBatchTest {
    @Test
    void guiGlyphsQueueTheSameBitmapWithoutImmediatePixelDraws() {
        final PoseStack pose = new PoseStack();
        final RecordingVertices consumer = new RecordingVertices();
        pose.translate(3, 5, 0);
        pose.scale(2, 2, 1);
        for (final String text : List.of("A", "界", " ")) {
            consumer.vertices.clear();
            TerminalFont.drawGuiGlyph(consumer, pose.last(), text.codePointAt(0), 7, 11, 0xFF123456);
            final List<List<Float>> expected = new ArrayList<>();
            final int cp = text.codePointAt(0);
            final int width = TerminalFont.glyphCellWidth(cp);
            for (int y = 0; y < TerminalFont.cellHeight(); y++) {
                for (int x = 0; x < width; x++) {
                    if ((TerminalFont.rowMask(cp, y) & (1 << (width - 1 - x))) == 0) continue;
                    final float x0 = 3 + 2 * (7 + x);
                    final float y0 = 5 + 2 * (11 + y);
                    expected.add(List.of(x0 + 2, y0 + 2, 0F));
                    expected.add(List.of(x0 + 2, y0, 0F));
                    expected.add(List.of(x0, y0, 0F));
                    expected.add(List.of(x0, y0 + 2, 0F));
                }
            }
            assertEquals(expected, consumer.vertices, text);
        }
    }

    private static final class RecordingVertices implements VertexConsumer {
        private final List<List<Float>> vertices = new ArrayList<>();
        @Override public VertexConsumer addVertex(float x, float y, float z) {
            vertices.add(List.of(x, y, z));
            return this;
        }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) {
            assertEquals(List.of(0x12, 0x34, 0x56, 0xFF), List.of(r, g, b, a));
            return this;
        }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}
