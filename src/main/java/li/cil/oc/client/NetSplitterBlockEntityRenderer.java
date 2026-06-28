package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.blockentity.NetSplitterBlockEntity;
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

public final class NetSplitterBlockEntityRenderer implements BlockEntityRenderer<NetSplitterBlockEntity> {
    private static final float MIN = -0.00125F;
    private static final float MAX = 1.00125F;
    private static final ResourceLocation OPEN_SIDE_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "block/overlay/netsplitter_on");

    public NetSplitterBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        final NetSplitterBlockEntity splitter,
        final float partialTick,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight,
        final int packedOverlay) {
        final VertexConsumer consumer = consumer(bufferSource);
        final PoseStack.Pose pose = poseStack.last();
        for (final Direction side : Direction.values()) {
            if (splitter.isSideOpen(side)) {
                renderSideOverlay(consumer, pose, side, packedLight);
            }
        }
    }

    private static VertexConsumer consumer(final MultiBufferSource bufferSource) {
        final TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(OPEN_SIDE_TEXTURE);
        return sprite.wrap(bufferSource.getBuffer(RenderType.translucent()));
    }

    private static void renderSideOverlay(
        final VertexConsumer consumer,
        final PoseStack.Pose pose,
        final Direction side,
        final int packedLight) {
        switch (side) {
            case DOWN -> {
                vertex(consumer, pose, MIN, MIN, MIN, 1F, 0F, side, packedLight);
                vertex(consumer, pose, MAX, MIN, MIN, 0F, 0F, side, packedLight);
                vertex(consumer, pose, MAX, MIN, MAX, 0F, 1F, side, packedLight);
                vertex(consumer, pose, MIN, MIN, MAX, 1F, 1F, side, packedLight);
            }
            case UP -> {
                vertex(consumer, pose, MIN, MAX, MIN, 1F, 1F, side, packedLight);
                vertex(consumer, pose, MIN, MAX, MAX, 1F, 0F, side, packedLight);
                vertex(consumer, pose, MAX, MAX, MAX, 0F, 0F, side, packedLight);
                vertex(consumer, pose, MAX, MAX, MIN, 0F, 1F, side, packedLight);
            }
            case NORTH -> {
                vertex(consumer, pose, MAX, MAX, MIN, 0F, 1F, side, packedLight);
                vertex(consumer, pose, MIN, MAX, MIN, 1F, 1F, side, packedLight);
                vertex(consumer, pose, MIN, MIN, MIN, 1F, 0F, side, packedLight);
                vertex(consumer, pose, MAX, MIN, MIN, 0F, 0F, side, packedLight);
            }
            case SOUTH -> {
                vertex(consumer, pose, MIN, MAX, MAX, 0F, 1F, side, packedLight);
                vertex(consumer, pose, MAX, MAX, MAX, 1F, 1F, side, packedLight);
                vertex(consumer, pose, MAX, MIN, MAX, 1F, 0F, side, packedLight);
                vertex(consumer, pose, MIN, MIN, MAX, 0F, 0F, side, packedLight);
            }
            case WEST -> {
                vertex(consumer, pose, MIN, MAX, MIN, 0F, 1F, side, packedLight);
                vertex(consumer, pose, MIN, MAX, MAX, 1F, 1F, side, packedLight);
                vertex(consumer, pose, MIN, MIN, MAX, 1F, 0F, side, packedLight);
                vertex(consumer, pose, MIN, MIN, MIN, 0F, 0F, side, packedLight);
            }
            case EAST -> {
                vertex(consumer, pose, MAX, MAX, MAX, 0F, 1F, side, packedLight);
                vertex(consumer, pose, MAX, MAX, MIN, 1F, 1F, side, packedLight);
                vertex(consumer, pose, MAX, MIN, MIN, 1F, 0F, side, packedLight);
                vertex(consumer, pose, MAX, MIN, MAX, 0F, 0F, side, packedLight);
            }
        }
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
        final int packedLight) {
        consumer.addVertex(pose, x, y, z)
            .setColor(0xFFFFFFFF)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(packedLight)
            .setNormal(pose, normal.getStepX(), normal.getStepY(), normal.getStepZ());
    }
}
