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
    private static final int COLOR_TOP = 0xFFE2E2E2;
    private static final int COLOR_SIDE = 0xFF7B8085;
    private static final int COLOR_FRONT = 0xFF50565C;
    private static final int COLOR_LEG = 0xFF7A4E28;
    private static final double ROBOT_Y_OFFSET = 0.08D;

    public RobotBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
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
        poseStack.translate(0.5D, ROBOT_Y_OFFSET + (robot.machine().isRunning() ? 0.06D : 0D), 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(yawRotation(robot.getBlockState().getValue(RobotBlock.FACING))));
        final VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutout(RobotModel.TEXTURE));
        renderPyramid(poseStack.last(), buffer, packedLight);
        renderLeg(poseStack.last(), buffer, packedLight);
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

    private static void renderPyramid(final PoseStack.Pose pose, final VertexConsumer consumer, final int packedLight) {
        final float half = 0.48F;
        final float baseY = 0.18F;
        final float apexY = 0.86F;
        quad(consumer, pose, -half, baseY, -half, half, baseY, -half, half, baseY, half, -half, baseY, half, COLOR_SIDE, 0F, -1F, 0F, packedLight);
        quad(consumer, pose, -half, baseY, -half, 0F, apexY, 0F, 0F, apexY, 0F, half, baseY, -half, COLOR_TOP, 0F, 0.7F, -0.7F, packedLight);
        quad(consumer, pose, half, baseY, -half, 0F, apexY, 0F, 0F, apexY, 0F, half, baseY, half, COLOR_SIDE, 0.7F, 0.7F, 0F, packedLight);
        quad(consumer, pose, half, baseY, half, 0F, apexY, 0F, 0F, apexY, 0F, -half, baseY, half, COLOR_FRONT, 0F, 0.7F, 0.7F, packedLight);
        quad(consumer, pose, -half, baseY, half, 0F, apexY, 0F, 0F, apexY, 0F, -half, baseY, -half, COLOR_SIDE, -0.7F, 0.7F, 0F, packedLight);
    }

    private static void renderLeg(final PoseStack.Pose pose, final VertexConsumer consumer, final int packedLight) {
        final float minX = -0.16F;
        final float maxX = 0.16F;
        final float minY = 0F;
        final float maxY = 0.22F;
        final float minZ = -0.16F;
        final float maxZ = 0.16F;
        quad(consumer, pose, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, COLOR_LEG, 0F, 0F, -1F, packedLight);
        quad(consumer, pose, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, minY, maxZ, COLOR_LEG, 0F, 0F, 1F, packedLight);
        quad(consumer, pose, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, minX, minY, minZ, COLOR_LEG, -1F, 0F, 0F, packedLight);
        quad(consumer, pose, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, COLOR_LEG, 1F, 0F, 0F, packedLight);
        quad(consumer, pose, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, COLOR_LEG, 0F, 1F, 0F, packedLight);
        quad(consumer, pose, minX, minY, maxZ, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, COLOR_LEG, 0F, -1F, 0F, packedLight);
    }

    private static void quad(
        final VertexConsumer consumer,
        final PoseStack.Pose pose,
        final float x0,
        final float y0,
        final float z0,
        final float x1,
        final float y1,
        final float z1,
        final float x2,
        final float y2,
        final float z2,
        final float x3,
        final float y3,
        final float z3,
        final int color,
        final float normalX,
        final float normalY,
        final float normalZ,
        final int packedLight) {
        vertex(consumer, pose, x0, y0, z0, 0F, 1F, color, normalX, normalY, normalZ, packedLight);
        vertex(consumer, pose, x1, y1, z1, 0F, 0F, color, normalX, normalY, normalZ, packedLight);
        vertex(consumer, pose, x2, y2, z2, 1F, 0F, color, normalX, normalY, normalZ, packedLight);
        vertex(consumer, pose, x3, y3, z3, 1F, 1F, color, normalX, normalY, normalZ, packedLight);
    }

    private static void vertex(
        final VertexConsumer consumer,
        final PoseStack.Pose pose,
        final float x,
        final float y,
        final float z,
        final float u,
        final float v,
        final int color,
        final float normalX,
        final float normalY,
        final float normalZ,
        final int packedLight) {
        consumer.addVertex(pose, x, y, z)
            .setColor(color)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(packedLight)
            .setNormal(pose, normalX, normalY, normalZ);
    }
}
