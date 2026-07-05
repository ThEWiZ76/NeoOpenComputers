package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import li.cil.oc.common.block.RobotBlock;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;

public final class RobotBlockEntityRenderer implements BlockEntityRenderer<RobotBlockEntity> {
    private final RobotModel model;

    public RobotBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
        model = new RobotModel(context.bakeLayer(RobotModel.LAYER_LOCATION));
    }

    @Override
    public void render(
        final RobotBlockEntity robot,
        final float partialTick,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight,
        final int packedOverlay
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5D, robot.machine().isRunning() ? 0.56D : 0.50D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(yawRotation(robot.getBlockState().getValue(RobotBlock.FACING))));
        poseStack.scale(0.95F, -0.95F, -0.95F);
        final VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutout(RobotModel.TEXTURE));
        model.renderToBuffer(poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    public static float yawRotation(final Direction facing) {
        return switch (facing) {
            case WEST -> 90F;
            case NORTH -> 180F;
            case EAST -> -90F;
            default -> 0F;
        };
    }
}
