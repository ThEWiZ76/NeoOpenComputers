package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.common.block.PrintBlock;
import li.cil.oc.common.blockentity.PrintBlockEntity;
import li.cil.oc.common.item.data.PrintRenderModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class PrintBlockEntityRenderer implements BlockEntityRenderer<PrintBlockEntity> {
    public PrintBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(final PrintBlockEntity print, final float partialTick, final PoseStack poseStack, final MultiBufferSource bufferSource, final int packedLight, final int packedOverlay) {
        final Direction facing = facing(print.getBlockState());
        for (PrintRenderModel.RenderShape shape : PrintRenderModel.blockShapes(print.data(), print.isActiveState(), facing)) {
            final TextureAtlasSprite sprite = sprite(shape.texture());
            final VertexConsumer consumer = sprite.wrap(bufferSource.getBuffer(RenderType.cutoutMipped()));
            renderBox(consumer, poseStack.last(), shape.bounds(), 0xFF000000 | shape.tint(), packedLight, packedOverlay);
        }
    }

    private static Direction facing(final BlockState state) {
        return state.hasProperty(PrintBlock.FACING) ? state.getValue(PrintBlock.FACING) : Direction.SOUTH;
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
        vertex(consumer, pose, color, packedLight, packedOverlay, normalX, normalY, normalZ, x1, y1, z1, 0F, 1F);
        vertex(consumer, pose, color, packedLight, packedOverlay, normalX, normalY, normalZ, x2, y2, z2, 1F, 1F);
        vertex(consumer, pose, color, packedLight, packedOverlay, normalX, normalY, normalZ, x3, y3, z3, 1F, 0F);
        vertex(consumer, pose, color, packedLight, packedOverlay, normalX, normalY, normalZ, x4, y4, z4, 0F, 0F);
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
        final float z,
        final float u,
        final float v) {
        consumer.addVertex(pose, x, y, z)
            .setColor(color)
            .setUv(u, v)
            .setOverlay(packedOverlay)
            .setLight(packedLight)
            .setNormal(pose, normalX, normalY, normalZ);
    }
}
