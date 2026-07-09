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
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class RobotBlockEntityRenderer implements BlockEntityRenderer<RobotBlockEntity> {
    private static final float CHASSIS_HALF = 0.40F;
    private static final float CHASSIS_MIN = -CHASSIS_HALF;
    private static final float CHASSIS_MAX = CHASSIS_HALF;
    private static final float TOP_SEAM_Y = 0.54F;
    private static final float BOTTOM_SEAM_Y = 0.46F;
    private static final float TOP_APEX_Y = 0.96F;
    private static final float BOTTOM_APEX_Y = 0.04F;
    private static final int COLOR_CHASSIS = 0xFF555555;
    private static final int COLOR_RUNNING_LIGHT = 0xAA30F230;
    private static final double STOPPED_Y_OFFSET = -0.03D;
    private static final double RUNNING_HOVER_OFFSET = 0.03D;

    private final ItemRenderer itemRenderer;

    public RobotBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
        itemRenderer = context.getItemRenderer();
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
        poseStack.translate(0.5D, robot.machine().isRunning() ? RUNNING_HOVER_OFFSET : STOPPED_Y_OFFSET, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(yawRotation(robot.getBlockState().getValue(RobotBlock.FACING))));
        renderLegacyChassis(robot, poseStack, bufferSource, packedLight);
        renderSelectedStack(robot, poseStack, bufferSource, packedLight);
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

    private static void renderLegacyChassis(
        final RobotBlockEntity robot,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight) {
        final VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutout(RobotModel.TEXTURE));
        final PoseStack.Pose pose = poseStack.last();
        renderBottomChassis(pose, buffer, packedLight);
        renderTopChassis(pose, buffer, packedLight);
        if (robot.machine().isRunning()) {
            renderRunningLight(pose, buffer, packedLight);
        }
    }

    private static void renderTopChassis(final PoseStack.Pose pose, final VertexConsumer consumer, final int packedLight) {
        triangleAsQuad(consumer, pose, 0F, TOP_APEX_Y, 0F, CHASSIS_MIN, TOP_SEAM_Y, CHASSIS_MAX, CHASSIS_MAX, TOP_SEAM_Y, CHASSIS_MAX, COLOR_CHASSIS, 0.25F, 0.25F, 0F, 0.5F, 0.5F, 0.5F, 0F, 0.2F, 1F, packedLight);
        triangleAsQuad(consumer, pose, 0F, TOP_APEX_Y, 0F, CHASSIS_MAX, TOP_SEAM_Y, CHASSIS_MAX, CHASSIS_MAX, TOP_SEAM_Y, CHASSIS_MIN, COLOR_CHASSIS, 0.25F, 0.25F, 0.5F, 0.5F, 0.5F, 0F, 1F, 0.2F, 0F, packedLight);
        triangleAsQuad(consumer, pose, 0F, TOP_APEX_Y, 0F, CHASSIS_MAX, TOP_SEAM_Y, CHASSIS_MIN, CHASSIS_MIN, TOP_SEAM_Y, CHASSIS_MIN, COLOR_CHASSIS, 0.25F, 0.25F, 0.5F, 0F, 0F, 0F, 0F, 0.2F, -1F, packedLight);
        triangleAsQuad(consumer, pose, 0F, TOP_APEX_Y, 0F, CHASSIS_MIN, TOP_SEAM_Y, CHASSIS_MIN, CHASSIS_MIN, TOP_SEAM_Y, CHASSIS_MAX, COLOR_CHASSIS, 0.25F, 0.25F, 0F, 0F, 0F, 0.5F, -1F, 0.2F, 0F, packedLight);
        quad(consumer, pose, CHASSIS_MIN, TOP_SEAM_Y, CHASSIS_MAX, CHASSIS_MIN, TOP_SEAM_Y, CHASSIS_MIN, CHASSIS_MAX, TOP_SEAM_Y, CHASSIS_MIN, CHASSIS_MAX, TOP_SEAM_Y, CHASSIS_MAX, COLOR_CHASSIS, 0F, 1F, 0F, packedLight, 0F, 1F, 0F, 0.5F, 0.5F, 0.5F, 0.5F, 1F);
    }

    private static void renderBottomChassis(final PoseStack.Pose pose, final VertexConsumer consumer, final int packedLight) {
        triangleAsQuad(consumer, pose, 0F, BOTTOM_APEX_Y, 0F, CHASSIS_MIN, BOTTOM_SEAM_Y, CHASSIS_MIN, CHASSIS_MAX, BOTTOM_SEAM_Y, CHASSIS_MIN, COLOR_CHASSIS, 0.75F, 0.25F, 0.5F, 0F, 1F, 0F, 0F, -0.2F, -1F, packedLight);
        triangleAsQuad(consumer, pose, 0F, BOTTOM_APEX_Y, 0F, CHASSIS_MAX, BOTTOM_SEAM_Y, CHASSIS_MIN, CHASSIS_MAX, BOTTOM_SEAM_Y, CHASSIS_MAX, COLOR_CHASSIS, 0.75F, 0.25F, 1F, 0F, 1F, 0.5F, 1F, -0.2F, 0F, packedLight);
        triangleAsQuad(consumer, pose, 0F, BOTTOM_APEX_Y, 0F, CHASSIS_MAX, BOTTOM_SEAM_Y, CHASSIS_MAX, CHASSIS_MIN, BOTTOM_SEAM_Y, CHASSIS_MAX, COLOR_CHASSIS, 0.75F, 0.25F, 1F, 0.5F, 0.5F, 0.5F, 0F, -0.2F, 1F, packedLight);
        triangleAsQuad(consumer, pose, 0F, BOTTOM_APEX_Y, 0F, CHASSIS_MIN, BOTTOM_SEAM_Y, CHASSIS_MAX, CHASSIS_MIN, BOTTOM_SEAM_Y, CHASSIS_MIN, COLOR_CHASSIS, 0.75F, 0.25F, 0.5F, 0.5F, 0.5F, 0F, -1F, -0.2F, 0F, packedLight);
        quad(consumer, pose, CHASSIS_MIN, BOTTOM_SEAM_Y, CHASSIS_MIN, CHASSIS_MIN, BOTTOM_SEAM_Y, CHASSIS_MAX, CHASSIS_MAX, BOTTOM_SEAM_Y, CHASSIS_MAX, CHASSIS_MAX, BOTTOM_SEAM_Y, CHASSIS_MIN, COLOR_CHASSIS, 0F, -1F, 0F, packedLight, 0F, 0.5F, 0F, 1F, 0.5F, 1F, 0.5F, 0.5F);
    }

    private static void renderRunningLight(final PoseStack.Pose pose, final VertexConsumer consumer, final int packedLight) {
        final float inset = 0.30F;
        quad(consumer, pose, -CHASSIS_HALF, TOP_SEAM_Y, -CHASSIS_HALF, -CHASSIS_HALF, BOTTOM_SEAM_Y, -CHASSIS_HALF, -CHASSIS_HALF, BOTTOM_SEAM_Y, inset, -CHASSIS_HALF, TOP_SEAM_Y, inset, COLOR_RUNNING_LIGHT, -1F, 0F, 0F, packedLight, 0.5F, 0.5F, 1F, 0.5F, 1F, 0.53125F, 0.5F, 0.53125F);
        quad(consumer, pose, -CHASSIS_HALF, TOP_SEAM_Y, CHASSIS_HALF, -CHASSIS_HALF, BOTTOM_SEAM_Y, CHASSIS_HALF, CHASSIS_HALF, BOTTOM_SEAM_Y, CHASSIS_HALF, CHASSIS_HALF, TOP_SEAM_Y, CHASSIS_HALF, COLOR_RUNNING_LIGHT, 0F, 0F, 1F, packedLight, 0.5F, 0.5F, 1F, 0.5F, 1F, 0.53125F, 0.5F, 0.53125F);
    }

    private void renderSelectedStack(
        final RobotBlockEntity robot,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight) {
        final int slot = robot.selectedSlot();
        if (slot < 0 || slot >= robot.getContainerSize()) {
            return;
        }
        final ItemStack stack = robot.getItem(slot);
        if (stack.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0D, 0.22D, 0.61D);
        poseStack.scale(0.45F, 0.45F, 0.45F);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, robot.getLevel(), 0);
        poseStack.popPose();
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
        quad(consumer, pose, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, color, normalX, normalY, normalZ, packedLight, 0F, 1F, 0F, 0F, 1F, 0F, 1F, 1F);
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
        final int packedLight,
        final float u0,
        final float v0,
        final float u1,
        final float v1,
        final float u2,
        final float v2,
        final float u3,
        final float v3) {
        vertex(consumer, pose, x0, y0, z0, u0, v0, color, normalX, normalY, normalZ, packedLight);
        vertex(consumer, pose, x1, y1, z1, u1, v1, color, normalX, normalY, normalZ, packedLight);
        vertex(consumer, pose, x2, y2, z2, u2, v2, color, normalX, normalY, normalZ, packedLight);
        vertex(consumer, pose, x3, y3, z3, u3, v3, color, normalX, normalY, normalZ, packedLight);
    }

    private static void triangleAsQuad(
        final VertexConsumer consumer,
        final PoseStack.Pose pose,
        final float apexX,
        final float apexY,
        final float apexZ,
        final float leftX,
        final float leftY,
        final float leftZ,
        final float rightX,
        final float rightY,
        final float rightZ,
        final int color,
        final float apexU,
        final float apexV,
        final float leftU,
        final float leftV,
        final float rightU,
        final float rightV,
        final float normalX,
        final float normalY,
        final float normalZ,
        final int packedLight) {
        vertex(consumer, pose, apexX, apexY, apexZ, apexU, apexV, color, normalX, normalY, normalZ, packedLight);
        vertex(consumer, pose, leftX, leftY, leftZ, leftU, leftV, color, normalX, normalY, normalZ, packedLight);
        vertex(consumer, pose, rightX, rightY, rightZ, rightU, rightV, color, normalX, normalY, normalZ, packedLight);
        vertex(consumer, pose, apexX, apexY, apexZ, apexU, apexV, color, normalX, normalY, normalZ, packedLight);
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
        final int alpha = (color >>> 24) & 0xFF;
        final int red = (color >>> 16) & 0xFF;
        final int green = (color >>> 8) & 0xFF;
        final int blue = color & 0xFF;
        consumer.addVertex(pose, x, y, z)
            .setColor(red, green, blue, alpha)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(packedLight)
            .setNormal(pose, normalX, normalY, normalZ);
    }
}
