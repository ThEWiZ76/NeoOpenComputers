package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HoverBootsModelTest {
    @Test
    void wornBootsHavePlatformsAndWingsWithoutVanillaLegCubes() {
        final var layer = layer(false);
        final var root = layer.bakeRoot();
        assertTrue(root.getChild("left_leg").isEmpty());
        assertTrue(root.getChild("right_leg").isEmpty());
        final var vertices = new Vertices();
        root.render(new PoseStack(), vertices, 0, 0);
        assertEquals(336, vertices.count, "Two three-box platforms and four two-box wings");
        assertEquals(-6.9F / 16, vertices.minX, 0.00001);
        assertEquals(6.9F / 16, vertices.maxX, 0.00001);
        assertEquals(21.1F / 16, vertices.minY, 0.00001);
        assertEquals(24.11F / 16, vertices.maxY, 0.00001);
    }

    @Test
    void glowContainsOnlyFourFlapsAtTheSameFootOffsets() {
        final var vertices = new Vertices();
        layer(true).bakeRoot().render(new PoseStack(), vertices, 0, 0);
        assertEquals(96, vertices.count);
        assertEquals(22.1F / 16, vertices.minY, 0.00001);
        assertEquals(23.11F / 16, vertices.maxY, 0.00001);
        assertTrue(vertices.minU >= 24F / 64 && vertices.maxU <= 48F / 64, "Glow must use the original light texture region");
    }

    @Test
    void glowStaysOnTheWingSurfacesAfterCopyingWalkingAndCrouchingPoses() {
        final var body = new HoverBootsModel<>(layer(false).bakeRoot());
        final var lights = new HoverBootsModel<>(layer(true).bakeRoot());
        body.young = false;
        body.crouching = true;
        body.leftLeg.xRot = 0.6F;
        body.rightLeg.xRot = -0.6F;
        body.leftLeg.y = body.rightLeg.y = 13;
        body.leftLeg.z = body.rightLeg.z = 4;
        body.copyPropertiesTo(lights);
        final var solidVertices = new Vertices();
        final var glowVertices = new Vertices();
        body.renderToBuffer(new PoseStack(), solidVertices, 0, 0);
        lights.renderToBuffer(new PoseStack(), glowVertices, 0, 0);
        assertEquals(96, glowVertices.count);
        for (final var glow : glowVertices.positions) {
            assertTrue(solidVertices.positions.stream().anyMatch(solid ->
                Math.abs(solid[0] - glow[0]) < 0.00001 && Math.abs(solid[1] - glow[1]) < 0.00001
                    && Math.abs(solid[2] - glow[2]) < 0.00001), "Glow moved away from its wing surface");
        }
    }

    private static LayerDefinition layer(boolean lights) {
        return HoverBootsModel.createLayer(lights);
    }

    private static final class Vertices implements VertexConsumer {
        int count;
        final java.util.List<float[]> positions = new java.util.ArrayList<>();
        float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        float minU = Float.POSITIVE_INFINITY, maxU = Float.NEGATIVE_INFINITY;
        public VertexConsumer addVertex(float x, float y, float z) {
            count++;
            positions.add(new float[]{x, y, z});
            minX = Math.min(minX, x); maxX = Math.max(maxX, x);
            minY = Math.min(minY, y); maxY = Math.max(maxY, y);
            return this;
        }
        public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        public VertexConsumer setUv(float u, float v) { minU = Math.min(minU, u); maxU = Math.max(maxU, u); return this; }
        public VertexConsumer setUv1(int u, int v) { return this; }
        public VertexConsumer setUv2(int u, int v) { return this; }
        public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}
