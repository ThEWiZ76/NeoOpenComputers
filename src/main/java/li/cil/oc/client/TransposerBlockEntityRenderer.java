package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.blockentity.TransposerBlockEntity;
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

public final class TransposerBlockEntityRenderer implements BlockEntityRenderer<TransposerBlockEntity> {
    private static final float MIN = -0.00125F;
    private static final float MAX = 1.00125F;
    private static final ResourceLocation ACTIVITY_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "block/overlay/transposer_on");

    public TransposerBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        final TransposerBlockEntity transposer,
        final float partialTick,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight,
        final int packedOverlay) {
        final float alpha = (float) Mth.clamp(transposer.visualActivity(), 0D, 1D);
        if (alpha <= 0F) {
            return;
        }
        renderActivityOverlays(alpha, poseStack, bufferSource, packedLight);
    }

    private static void renderActivityOverlays(
        final float alpha,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight) {
        final VertexConsumer consumer = consumer(bufferSource);
        final PoseStack.Pose pose = poseStack.last();
        for (final Direction side : Direction.values()) {
            renderSideOverlay(consumer, pose, side, alpha, packedLight);
        }
    }

    private static void renderSideOverlay(
        final VertexConsumer consumer,
        final PoseStack.Pose pose,
        final Direction side,
        final float alpha,
        final int packedLight) {
        switch (side) {
            case DOWN -> {
                vertex(consumer, pose, MIN, MIN, MIN, 1F, 0F, side, alpha, packedLight);
                vertex(consumer, pose, MAX, MIN, MIN, 0F, 0F, side, alpha, packedLight);
                vertex(consumer, pose, MAX, MIN, MAX, 0F, 1F, side, alpha, packedLight);
                vertex(consumer, pose, MIN, MIN, MAX, 1F, 1F, side, alpha, packedLight);
            }
            case UP -> {
                vertex(consumer, pose, MIN, MAX, MIN, 1F, 1F, side, alpha, packedLight);
                vertex(consumer, pose, MIN, MAX, MAX, 1F, 0F, side, alpha, packedLight);
                vertex(consumer, pose, MAX, MAX, MAX, 0F, 0F, side, alpha, packedLight);
                vertex(consumer, pose, MAX, MAX, MIN, 0F, 1F, side, alpha, packedLight);
            }
            case NORTH -> {
                vertex(consumer, pose, MAX, MAX, MIN, 0F, 1F, side, alpha, packedLight);
                vertex(consumer, pose, MIN, MAX, MIN, 1F, 1F, side, alpha, packedLight);
                vertex(consumer, pose, MIN, MIN, MIN, 1F, 0F, side, alpha, packedLight);
                vertex(consumer, pose, MAX, MIN, MIN, 0F, 0F, side, alpha, packedLight);
            }
            case SOUTH -> {
                vertex(consumer, pose, MIN, MAX, MAX, 0F, 1F, side, alpha, packedLight);
                vertex(consumer, pose, MAX, MAX, MAX, 1F, 1F, side, alpha, packedLight);
                vertex(consumer, pose, MAX, MIN, MAX, 1F, 0F, side, alpha, packedLight);
                vertex(consumer, pose, MIN, MIN, MAX, 0F, 0F, side, alpha, packedLight);
            }
            case WEST -> {
                vertex(consumer, pose, MIN, MAX, MIN, 0F, 1F, side, alpha, packedLight);
                vertex(consumer, pose, MIN, MAX, MAX, 1F, 1F, side, alpha, packedLight);
                vertex(consumer, pose, MIN, MIN, MAX, 1F, 0F, side, alpha, packedLight);
                vertex(consumer, pose, MIN, MIN, MIN, 0F, 0F, side, alpha, packedLight);
            }
            case EAST -> {
                vertex(consumer, pose, MAX, MAX, MAX, 0F, 1F, side, alpha, packedLight);
                vertex(consumer, pose, MAX, MAX, MIN, 1F, 1F, side, alpha, packedLight);
                vertex(consumer, pose, MAX, MIN, MIN, 1F, 0F, side, alpha, packedLight);
                vertex(consumer, pose, MAX, MIN, MAX, 0F, 0F, side, alpha, packedLight);
            }
        }
    }

    private static VertexConsumer consumer(final MultiBufferSource bufferSource) {
        final TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ACTIVITY_TEXTURE);
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
