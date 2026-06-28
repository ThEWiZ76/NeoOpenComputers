package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.block.ComputerCaseBlock;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
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
import net.minecraft.Util;
import net.minecraft.world.level.block.state.BlockState;

public final class ComputerCaseBlockEntityRenderer implements BlockEntityRenderer<ComputerCaseBlockEntity> {
    private static final float FRONT_Z = 0.505F;
    private static final ResourceLocation ON_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "block/overlay/case_front_on");
    private static final ResourceLocation ACTIVITY_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "block/overlay/case_front_activity");
    private static final ResourceLocation ERROR_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "block/overlay/case_front_error");

    public ComputerCaseBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        final ComputerCaseBlockEntity computerCase,
        final float partialTick,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight,
        final int packedOverlay) {
        poseStack.pushPose();
        orientToCaseFront(computerCase.getBlockState(), poseStack);
        if (computerCase.isClientRunning()) {
            renderFrontOverlay(ON_TEXTURE, poseStack, bufferSource, packedLight, packedOverlay);
            if (computerCase.visualFileSystemActivity() > 0D) {
                renderFrontOverlay(ACTIVITY_TEXTURE, poseStack, bufferSource, packedLight, packedOverlay);
            }
        } else if (computerCase.isClientErrored() && shouldShowErrorLight(computerCase.hashCode(), Util.getMillis())) {
            renderFrontOverlay(ERROR_TEXTURE, poseStack, bufferSource, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    static boolean shouldShowErrorLight(final int seed, final long timeMillis) {
        return ((timeMillis / 500L + seed) & 1L) == 0L;
    }

    private static void renderFrontOverlay(
        final ResourceLocation texture,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight,
        final int packedOverlay) {
        final TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
        final VertexConsumer consumer = sprite.wrap(bufferSource.getBuffer(RenderType.cutout()));
        final PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, -0.5F, -0.5F, FRONT_Z, 0F, 1F, packedLight, packedOverlay);
        vertex(consumer, pose, 0.5F, -0.5F, FRONT_Z, 1F, 1F, packedLight, packedOverlay);
        vertex(consumer, pose, 0.5F, 0.5F, FRONT_Z, 1F, 0F, packedLight, packedOverlay);
        vertex(consumer, pose, -0.5F, 0.5F, FRONT_Z, 0F, 0F, packedLight, packedOverlay);
    }

    private static void vertex(
        final VertexConsumer consumer,
        final PoseStack.Pose pose,
        final float x,
        final float y,
        final float z,
        final float u,
        final float v,
        final int packedLight,
        final int packedOverlay) {
        consumer.addVertex(pose, x, y, z)
            .setColor(0xFFFFFFFF)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(packedLight)
            .setNormal(pose, 0F, 0F, 1F);
    }

    private static void orientToCaseFront(final BlockState state, final PoseStack poseStack) {
        final Direction facing = state.hasProperty(ComputerCaseBlock.FACING) ? state.getValue(ComputerCaseBlock.FACING) : Direction.NORTH;
        poseStack.translate(0.5D, 0.5D, 0.5D);
        switch (facing) {
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(-90F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90F));
            default -> {
            }
        }
    }
}
