package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class PrintShapeRendererTest {
    @Test
    void printFacesUseCubeMappedUvsLikeUpstreamBakedBoxes() throws ReflectiveOperationException {
        final Method face = PrintShapeRenderer.class.getDeclaredMethod(
            "face",
            VertexConsumer.class,
            PoseStack.Pose.class,
            int.class,
            int.class,
            int.class,
            float.class,
            float.class,
            float.class,
            float.class,
            float.class,
            float.class,
            float.class,
            float.class,
            float.class,
            float.class,
            float.class,
            float.class,
            float.class,
            float.class,
            float.class);
        face.setAccessible(true);
        final CollectingVertexConsumer consumer = new CollectingVertexConsumer();
        final PoseStack poseStack = new PoseStack();

        face.invoke(
            null,
            consumer,
            poseStack.last(),
            0xFFFFFFFF,
            0,
            0,
            0F,
            1F,
            0F,
            0.25F,
            0.75F,
            0.125F,
            0.5F,
            0.75F,
            0.125F,
            0.5F,
            0.75F,
            0.625F,
            0.25F,
            0.75F,
            0.625F);

        assertEquals(0.25F, consumer.uvs.getFirst()[0], 0.0001F);
        assertEquals(0.125F, consumer.uvs.getFirst()[1], 0.0001F);
    }

    private static final class CollectingVertexConsumer implements VertexConsumer {
        private final List<float[]> uvs = new ArrayList<>();

        @Override
        public VertexConsumer addVertex(final float x, final float y, final float z) {
            return this;
        }

        @Override
        public VertexConsumer setColor(final int red, final int green, final int blue, final int alpha) {
            return this;
        }

        @Override
        public VertexConsumer setUv(final float u, final float v) {
            uvs.add(new float[]{u, v});
            return this;
        }

        @Override
        public VertexConsumer setUv1(final int u, final int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(final int u, final int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(final float normalX, final float normalY, final float normalZ) {
            return this;
        }
    }
}
