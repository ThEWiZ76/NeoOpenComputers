package li.cil.oc.client;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class RaidRendererGeometryTest {
    private static Vertices overlay(PoseStack stack, int slot) throws Exception {
        final var method = RaidBlockEntityRenderer.class.getDeclaredMethod("renderSlotOverlay", VertexConsumer.class, PoseStack.Pose.class, int.class, int.class, int.class);
        method.setAccessible(true); final var vertices = new Vertices(); method.invoke(null, vertices, stack.last(), slot, 0, 0); return vertices;
    }
    private static void outward(Vertices vertices, Vector3f normal) {
        assertEquals(4, vertices.positions.size());
        final var a = new Vector3f(vertices.positions.get(1)).sub(vertices.positions.get(0));
        final var b = new Vector3f(vertices.positions.get(2)).sub(vertices.positions.get(0));
        assertTrue(a.cross(b).dot(normal) > 0, "Overlay quad winding faces into the block and is culled from outside");
        for (var value : vertices.normals) assertTrue(value.dot(normal) > 0.999F, "Lighting normal disagrees with visible face");
    }
    @Test
    void slotQuadsFaceOutwards() throws Exception {
        for (int slot = 0; slot < 3; slot++) outward(overlay(new PoseStack(), slot), new Vector3f(0, 0, 1));
    }
    @Test
    void modelHasAllHorizontalFrontVariants() throws Exception {
        final var json = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/neoopencomputers/blockstates/raid.json"))).getAsJsonObject().getAsJsonObject("variants");
        assertEquals(java.util.Set.of("facing=north", "facing=east", "facing=south", "facing=west"), json.keySet());
        final String[] faces = {"north", "east", "south", "west"};
        for (int index = 0; index < faces.length; index++) {
            final var variant = json.getAsJsonObject("facing=" + faces[index]);
            assertEquals("neoopencomputers:block/raid", variant.get("model").getAsString());
            assertEquals(index * 90, variant.has("y") ? variant.get("y").getAsInt() : 0);
        }
    }
    @Test
    void transformedSlotsStayOnTheModelFrontWithMatchingUvs() throws Exception {
        for (var face : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            final var pose = new PoseStack(); RaidBlockEntityRenderer.orientToFront(face, pose);
            final var normal = new Vector3f(face.getStepX(), 0, face.getStepZ());
            for (int slot = 0; slot < 3; slot++) {
                final var vertices = overlay(pose, slot); outward(vertices, normal);
                for (var position : vertices.positions) {
                    final var centered = new Vector3f(position).sub(0.5F, 0.5F, 0.5F);
                    assertEquals(0.50125F, centered.dot(normal), 0.00001F, "Overlay moved off the selected outer model face");
                    assertTrue(position.y >= -0.0001F && position.y <= 1.0001F);
                }
                assertEquals(2F / 16F + slot * 4F / 16F, vertices.uvs.stream().mapToDouble(uv -> uv[0]).min().orElseThrow(), 0.00001);
                assertEquals(2F / 16F + (slot + 1) * 4F / 16F, vertices.uvs.stream().mapToDouble(uv -> uv[0]).max().orElseThrow(), 0.00001);
            }
        }
    }
    private static final class Vertices implements VertexConsumer {
        final List<Vector3f> positions = new ArrayList<>();
        final List<Vector3f> normals = new ArrayList<>();
        final List<float[]> uvs = new ArrayList<>();
        @Override public VertexConsumer addVertex(float x, float y, float z) { positions.add(new Vector3f(x, y, z)); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { normals.add(new Vector3f(x, y, z)); return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { uvs.add(new float[]{u, v}); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
    }
}
