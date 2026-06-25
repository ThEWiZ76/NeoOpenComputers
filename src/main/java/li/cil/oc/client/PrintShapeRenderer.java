package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.common.item.data.PrintRenderModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

final class PrintShapeRenderer {
    private PrintShapeRenderer() {
    }

    static void renderShapes(
        final Iterable<PrintRenderModel.RenderShape> shapes,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight,
        final int packedOverlay) {
        for (PrintRenderModel.RenderShape shape : shapes) {
            final TextureAtlasSprite sprite = sprite(shape.texture());
            final VertexConsumer consumer = sprite.wrap(bufferSource.getBuffer(RenderType.cutoutMipped()));
            renderBox(consumer, poseStack.last(), shape.bounds(), 0xFF000000 | shape.tint(), packedLight, packedOverlay);
        }
    }

    private static TextureAtlasSprite sprite(final String texture) {
        final ResourceLocation location = ResourceLocation.tryParse(texture);
        final ResourceLocation safeLocation = location == null ? ResourceLocation.withDefaultNamespace("block/white_wool") : location;
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(safeLocation);
    }

    private static void renderBox(
        final VertexConsumer consumer,
        final PoseStack.Pose pose,
        final AABB bounds,
        final int color,
        final int packedLight,
        final int packedOverlay) {
        final float minX = (float) bounds.minX;
        final float minY = (float) bounds.minY;
        final float minZ = (float) bounds.minZ;
        final float maxX = (float) bounds.maxX;
        final float maxY = (float) bounds.maxY;
        final float maxZ = (float) bounds.maxZ;

        face(consumer, pose, color, packedLight, packedOverlay, 0F, 0F, -1F, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ);
        face(consumer, pose, color, packedLight, packedOverlay, 0F, 0F, 1F, maxX, minY, maxZ, minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ);
        face(consumer, pose, color, packedLight, packedOverlay, -1F, 0F, 0F, minX, minY, maxZ, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ);
        face(consumer, pose, color, packedLight, packedOverlay, 1F, 0F, 0F, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ);
        face(consumer, pose, color, packedLight, packedOverlay, 0F, 1F, 0F, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ);
        face(consumer, pose, color, packedLight, packedOverlay, 0F, -1F, 0F, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, minX, minY, minZ);
    }

    private static void face(
        final VertexConsumer consumer,
        final PoseStack.Pose pose,
        final int color,
        final int packedLight,
        final int packedOverlay,
        final float normalX,
        final float normalY,
        final float normalZ,
        final float x1,
        final float y1,
        final float z1,
        final float x2,
        final float y2,
        final float z2,
        final float x3,
        final float y3,
        final float z3,
        final float x4,
        final float y4,
        final float z4) {
        vertex(consumer, pose, color, packedLight, packedOverlay, normalX, normalY, normalZ, x1, y1, z1);
        vertex(consumer, pose, color, packedLight, packedOverlay, normalX, normalY, normalZ, x2, y2, z2);
        vertex(consumer, pose, color, packedLight, packedOverlay, normalX, normalY, normalZ, x3, y3, z3);
        vertex(consumer, pose, color, packedLight, packedOverlay, normalX, normalY, normalZ, x4, y4, z4);
    }

    private static void vertex(
        final VertexConsumer consumer,
        final PoseStack.Pose pose,
        final int color,
        final int packedLight,
        final int packedOverlay,
        final float normalX,
        final float normalY,
        final float normalZ,
        final float x,
        final float y,
        final float z) {
        final float[] uv = cubeMappedUv(normalX, normalY, normalZ, x, y, z);
        consumer.addVertex(pose, x, y, z)
            .setColor(color)
            .setUv(uv[0], uv[1])
            .setOverlay(packedOverlay)
            .setLight(packedLight)
            .setNormal(pose, normalX, normalY, normalZ);
    }

    private static float[] cubeMappedUv(
        final float normalX,
        final float normalY,
        final float normalZ,
        final float x,
        final float y,
        final float z) {
        if (normalY < 0F) {
            return new float[]{x, 1F - z};
        }
        if (normalY > 0F) {
            return new float[]{x, z};
        }
        if (normalZ < 0F) {
            return new float[]{1F - x, 1F - y};
        }
        if (normalZ > 0F) {
            return new float[]{x, 1F - y};
        }
        if (normalX < 0F) {
            return new float[]{z, 1F - y};
        }
        return new float[]{1F - z, 1F - y};
    }
}
