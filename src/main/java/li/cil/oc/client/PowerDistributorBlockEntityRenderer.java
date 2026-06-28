package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.blockentity.PowerDistributorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class PowerDistributorBlockEntityRenderer implements BlockEntityRenderer<PowerDistributorBlockEntity> {
    private static final float MIN = -0.00125F;
    private static final float MAX = 1.00125F;
    private static final float TOP_Y = 1.00125F;
    private static final float SIDE_OFFSET = 0.00125F;
    private static final ResourceLocation TOP_ON_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "block/overlay/powerdistributor_top_on");
    private static final ResourceLocation SIDE_ON_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "block/overlay/powerdistributor_side_on");

    public PowerDistributorBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        final PowerDistributorBlockEntity distributor,
        final float partialTick,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight,
        final int packedOverlay) {
        final float alpha = (float) Mth.clamp(distributor.visualBufferRatio(), 0D, 1D);
        if (alpha <= 0F) {
            return;
        }
        renderTopOverlay(alpha, poseStack, bufferSource, packedLight);
        renderSideOverlays(alpha, poseStack, bufferSource, packedLight);
    }

    private static void renderTopOverlay(
        final float alpha,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight) {
        final VertexConsumer consumer = consumer(bufferSource, TOP_ON_TEXTURE);
        final PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, MIN, TOP_Y, MAX, 0F, 1F, Direction.UP, alpha, packedLight);
        vertex(consumer, pose, MAX, TOP_Y, MAX, 1F, 1F, Direction.UP, alpha, packedLight);
        vertex(consumer, pose, MAX, TOP_Y, MIN, 1F, 0F, Direction.UP, alpha, packedLight);
        vertex(consumer, pose, MIN, TOP_Y, MIN, 0F, 0F, Direction.UP, alpha, packedLight);
    }

    private static void renderSideOverlays(
        final float alpha,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight) {
        renderSideOverlay(Direction.NORTH, alpha, poseStack, bufferSource, packedLight);
        renderSideOverlay(Direction.SOUTH, alpha, poseStack, bufferSource, packedLight);
        renderSideOverlay(Direction.EAST, alpha, poseStack, bufferSource, packedLight);
        renderSideOverlay(Direction.WEST, alpha, poseStack, bufferSource, packedLight);
    }

    private static void renderSideOverlay(
        final Direction side,
        final float alpha,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight) {
        final VertexConsumer consumer = consumer(bufferSource, SIDE_ON_TEXTURE);
        final PoseStack.Pose pose = poseStack.last();
        switch (side) {
            case NORTH -> {
                vertex(consumer, pose, MAX, MAX, -SIDE_OFFSET, 0F, 0F, side, alpha, packedLight);
                vertex(consumer, pose, MIN, MAX, -SIDE_OFFSET, 1F, 0F, side, alpha, packedLight);
                vertex(consumer, pose, MIN, MIN, -SIDE_OFFSET, 1F, 1F, side, alpha, packedLight);
                vertex(consumer, pose, MAX, MIN, -SIDE_OFFSET, 0F, 1F, side, alpha, packedLight);
            }
            case SOUTH -> {
                vertex(consumer, pose, MIN, MAX, 1F + SIDE_OFFSET, 0F, 0F, side, alpha, packedLight);
                vertex(consumer, pose, MAX, MAX, 1F + SIDE_OFFSET, 1F, 0F, side, alpha, packedLight);
                vertex(consumer, pose, MAX, MIN, 1F + SIDE_OFFSET, 1F, 1F, side, alpha, packedLight);
                vertex(consumer, pose, MIN, MIN, 1F + SIDE_OFFSET, 0F, 1F, side, alpha, packedLight);
            }
            case WEST -> {
                vertex(consumer, pose, -SIDE_OFFSET, MAX, MIN, 0F, 0F, side, alpha, packedLight);
                vertex(consumer, pose, -SIDE_OFFSET, MAX, MAX, 1F, 0F, side, alpha, packedLight);
                vertex(consumer, pose, -SIDE_OFFSET, MIN, MAX, 1F, 1F, side, alpha, packedLight);
                vertex(consumer, pose, -SIDE_OFFSET, MIN, MIN, 0F, 1F, side, alpha, packedLight);
            }
            case EAST -> {
                vertex(consumer, pose, 1F + SIDE_OFFSET, MAX, MAX, 0F, 0F, side, alpha, packedLight);
                vertex(consumer, pose, 1F + SIDE_OFFSET, MAX, MIN, 1F, 0F, side, alpha, packedLight);
                vertex(consumer, pose, 1F + SIDE_OFFSET, MIN, MIN, 1F, 1F, side, alpha, packedLight);
                vertex(consumer, pose, 1F + SIDE_OFFSET, MIN, MAX, 0F, 1F, side, alpha, packedLight);
            }
            default -> {
            }
        }
    }

    private static VertexConsumer consumer(final MultiBufferSource bufferSource, final ResourceLocation texture) {
        final TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
        return sprite.wrap(bufferSource.getBuffer(RenderType.translucent()));
    }

    private static void vertex(
        final VertexConsumer consumer,
        final PoseStack.Pose pose,
        final float x,
        final float y,
        final float z,
        final float u,
        final float v,
        final Direction normal,
        final float alpha,
        final int packedLight) {
        consumer.addVertex(pose, x, y, z)
            .setColor(colorWithAlpha(alpha))
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(packedLight)
            .setNormal(pose, normal.getStepX(), normal.getStepY(), normal.getStepZ());
    }

    static int colorWithAlpha(final float alpha) {
        final int alphaByte = Mth.clamp(Math.round(alpha * 255F), 0, 255);
        return (alphaByte << 24) | 0x00FFFFFF;
    }
}
