package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.blockentity.AdapterBlockEntity;
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

public final class AdapterBlockEntityRenderer implements BlockEntityRenderer<AdapterBlockEntity> {
    private static final float MIN = -0.00125F;
    private static final float MAX = 1.00125F;
    private static final ResourceLocation OPEN_SIDE_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "block/overlay/adapter_on");

    public AdapterBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        final AdapterBlockEntity adapter,
        final float partialTick,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight,
        final int packedOverlay) {
        final TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(OPEN_SIDE_TEXTURE);
        final VertexConsumer consumer = sprite.wrap(bufferSource.getBuffer(RenderType.cutout()));
        final PoseStack.Pose pose = poseStack.last();
        for (Direction side : Direction.values()) {
            if (adapter.isSideOpen(side)) {
                renderSide(consumer, pose, side, packedLight, packedOverlay);
            }
        }
    }

    static Direction sideNormal(final Direction side) {
        return side;
    }

    private static void renderSide(
        final VertexConsumer consumer,
        final PoseStack.Pose pose,
        final Direction side,
        final int packedLight,
        final int packedOverlay) {
        switch (side) {
            case DOWN -> {
                vertex(consumer, pose, MIN, MIN, MIN, 1F, 0F, side, packedLight, packedOverlay);
                vertex(consumer, pose, MAX, MIN, MIN, 0F, 0F, side, packedLight, packedOverlay);
                vertex(consumer, pose, MAX, MIN, MAX, 0F, 1F, side, packedLight, packedOverlay);
                vertex(consumer, pose, MIN, MIN, MAX, 1F, 1F, side, packedLight, packedOverlay);
            }
            case UP -> {
                vertex(consumer, pose, MIN, MAX, MIN, 1F, 1F, side, packedLight, packedOverlay);
                vertex(consumer, pose, MIN, MAX, MAX, 1F, 0F, side, packedLight, packedOverlay);
                vertex(consumer, pose, MAX, MAX, MAX, 0F, 0F, side, packedLight, packedOverlay);
                vertex(consumer, pose, MAX, MAX, MIN, 0F, 1F, side, packedLight, packedOverlay);
            }
            case NORTH -> {
                vertex(consumer, pose, MAX, MAX, MIN, 0F, 1F, side, packedLight, packedOverlay);
                vertex(consumer, pose, MIN, MAX, MIN, 1F, 1F, side, packedLight, packedOverlay);
                vertex(consumer, pose, MIN, MIN, MIN, 1F, 0F, side, packedLight, packedOverlay);
                vertex(consumer, pose, MAX, MIN, MIN, 0F, 0F, side, packedLight, packedOverlay);
            }
            case SOUTH -> {
                vertex(consumer, pose, MIN, MAX, MAX, 0F, 1F, side, packedLight, packedOverlay);
                vertex(consumer, pose, MAX, MAX, MAX, 1F, 1F, side, packedLight, packedOverlay);
                vertex(consumer, pose, MAX, MIN, MAX, 1F, 0F, side, packedLight, packedOverlay);
                vertex(consumer, pose, MIN, MIN, MAX, 0F, 0F, side, packedLight, packedOverlay);
            }
            case WEST -> {
                vertex(consumer, pose, MIN, MAX, MIN, 0F, 1F, side, packedLight, packedOverlay);
                vertex(consumer, pose, MIN, MAX, MAX, 1F, 1F, side, packedLight, packedOverlay);
                vertex(consumer, pose, MIN, MIN, MAX, 1F, 0F, side, packedLight, packedOverlay);
                vertex(consumer, pose, MIN, MIN, MIN, 0F, 0F, side, packedLight, packedOverlay);
            }
            case EAST -> {
                vertex(consumer, pose, MAX, MAX, MAX, 0F, 1F, side, packedLight, packedOverlay);
                vertex(consumer, pose, MAX, MAX, MIN, 1F, 1F, side, packedLight, packedOverlay);
                vertex(consumer, pose, MAX, MIN, MIN, 1F, 0F, side, packedLight, packedOverlay);
                vertex(consumer, pose, MAX, MIN, MAX, 0F, 0F, side, packedLight, packedOverlay);
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
        final int packedLight,
        final int packedOverlay) {
        consumer.addVertex(pose, x, y, z)
            .setColor(0xFFFFFFFF)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(packedLight)
            .setNormal(pose, normal.getStepX(), normal.getStepY(), normal.getStepZ());
    }
}
