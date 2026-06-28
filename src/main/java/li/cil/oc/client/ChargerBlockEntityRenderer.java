package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.block.ChargerBlock;
import li.cil.oc.common.blockentity.ChargerBlockEntity;
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
import net.minecraft.world.level.block.state.BlockState;

public final class ChargerBlockEntityRenderer implements BlockEntityRenderer<ChargerBlockEntity> {
    private static final float FRONT_Z = 0.505F;
    private static final float SIDE_OFFSET = 0.505F;
    private static final ResourceLocation FRONT_ON_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "block/overlay/charger_front_on");
    private static final ResourceLocation SIDE_ON_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "block/overlay/charger_side_on");

    public ChargerBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        final ChargerBlockEntity charger,
        final float partialTick,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight,
        final int packedOverlay) {
        final float chargeSpeed = (float) Mth.clamp(charger.visualChargeSpeed(), 0D, 1D);
        if (chargeSpeed <= 0F) {
            return;
        }

        poseStack.pushPose();
        orientToChargerFront(charger.getBlockState(), poseStack);
        renderFrontOverlay(chargeSpeed, poseStack, bufferSource, packedLight);
        if (charger.isVisuallyPowered()) {
            renderSideOverlays(poseStack, bufferSource, packedLight);
        }
        poseStack.popPose();
    }

    static ResourceLocation frontOnTexture() {
        return FRONT_ON_TEXTURE;
    }

    static ResourceLocation sideOnTexture() {
        return SIDE_ON_TEXTURE;
    }

    private static void renderFrontOverlay(
        final float chargeSpeed,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight) {
        final float inverse = 1F - chargeSpeed;
        final float bottom = inverse - 0.5F;
        final VertexConsumer consumer = consumer(bufferSource, FRONT_ON_TEXTURE);
        final PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, -0.5F, 0.5F, FRONT_Z, 0F, 1F, 0F, 0F, 1F, packedLight);
        vertex(consumer, pose, 0.5F, 0.5F, FRONT_Z, 1F, 1F, 0F, 0F, 1F, packedLight);
        vertex(consumer, pose, 0.5F, bottom, FRONT_Z, 1F, inverse, 0F, 0F, 1F, packedLight);
        vertex(consumer, pose, -0.5F, bottom, FRONT_Z, 0F, inverse, 0F, 0F, 1F, packedLight);
    }

    private static void renderSideOverlays(final PoseStack poseStack, final MultiBufferSource bufferSource, final int packedLight) {
        final VertexConsumer consumer = consumer(bufferSource, SIDE_ON_TEXTURE);
        final PoseStack.Pose pose = poseStack.last();

        vertex(consumer, pose, -SIDE_OFFSET, 0.5F, 0.5F, 0F, 0F, -1F, 0F, 0F, packedLight);
        vertex(consumer, pose, -SIDE_OFFSET, 0.5F, -0.5F, 1F, 0F, -1F, 0F, 0F, packedLight);
        vertex(consumer, pose, -SIDE_OFFSET, -0.5F, -0.5F, 1F, 1F, -1F, 0F, 0F, packedLight);
        vertex(consumer, pose, -SIDE_OFFSET, -0.5F, 0.5F, 0F, 1F, -1F, 0F, 0F, packedLight);

        vertex(consumer, pose, 0.5F, 0.5F, -SIDE_OFFSET, 0F, 0F, 0F, 0F, -1F, packedLight);
        vertex(consumer, pose, -0.5F, 0.5F, -SIDE_OFFSET, 1F, 0F, 0F, 0F, -1F, packedLight);
        vertex(consumer, pose, -0.5F, -0.5F, -SIDE_OFFSET, 1F, 1F, 0F, 0F, -1F, packedLight);
        vertex(consumer, pose, 0.5F, -0.5F, -SIDE_OFFSET, 0F, 1F, 0F, 0F, -1F, packedLight);

        vertex(consumer, pose, SIDE_OFFSET, 0.5F, -0.5F, 0F, 0F, 1F, 0F, 0F, packedLight);
        vertex(consumer, pose, SIDE_OFFSET, 0.5F, 0.5F, 1F, 0F, 1F, 0F, 0F, packedLight);
        vertex(consumer, pose, SIDE_OFFSET, -0.5F, 0.5F, 1F, 1F, 1F, 0F, 0F, packedLight);
        vertex(consumer, pose, SIDE_OFFSET, -0.5F, -0.5F, 0F, 1F, 1F, 0F, 0F, packedLight);
    }

    private static void orientToChargerFront(final BlockState state, final PoseStack poseStack) {
        final Direction facing = state.hasProperty(ChargerBlock.FACING) ? state.getValue(ChargerBlock.FACING) : Direction.NORTH;
        poseStack.translate(0.5D, 0.5D, 0.5D);
        switch (facing) {
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(-90F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90F));
            default -> {
            }
        }
    }

    private static VertexConsumer consumer(final MultiBufferSource bufferSource, final ResourceLocation texture) {
        final TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
        return sprite.wrap(bufferSource.getBuffer(RenderType.cutout()));
    }

    private static void vertex(
        final VertexConsumer consumer,
        final PoseStack.Pose pose,
        final float x,
        final float y,
        final float z,
        final float u,
        final float v,
        final float normalX,
        final float normalY,
        final float normalZ,
        final int packedLight) {
        consumer.addVertex(pose, x, y, z)
            .setColor(0xFFFFFFFF)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(packedLight)
            .setNormal(pose, normalX, normalY, normalZ);
    }
}
