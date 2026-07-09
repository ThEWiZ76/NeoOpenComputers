package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.block.RobotBlock;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public final class RobotBlockEntityRenderer implements BlockEntityRenderer<RobotBlockEntity> {
    private static final ResourceLocation ROBOT_RENDER_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "textures/block/white.png");
    private static final float PYRAMID_HALF = 0.60F;
    private static final float PYRAMID_BASE_Y = 0.30F;
    private static final float PYRAMID_APEX_Y = 0.74F;
    private static final float CHEST_MIN_Z = 0.28F;
    private static final int COLOR_TOP = 0xFF30343A;
    private static final int COLOR_LEFT = 0xFF3F464D;
    private static final int COLOR_RIGHT = 0xFF181B1F;
    private static final int COLOR_FRONT = 0xFF24282D;
    private static final int COLOR_CHEST = 0xFF8A5B32;
    private static final int COLOR_CHEST_DARK = 0xFF3A2414;
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
        final VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutout(ROBOT_RENDER_TEXTURE));
        renderPyramid(poseStack.last(), buffer, packedLight);
        renderFrontChest(poseStack.last(), buffer, packedLight);
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
        quad(consumer, pose, -PYRAMID_HALF, PYRAMID_BASE_Y, -PYRAMID_HALF, PYRAMID_HALF, PYRAMID_BASE_Y, -PYRAMID_HALF, PYRAMID_HALF, PYRAMID_BASE_Y, PYRAMID_HALF, -PYRAMID_HALF, PYRAMID_BASE_Y, PYRAMID_HALF, COLOR_RIGHT, 0F, -1F, 0F, packedLight);
        quad(consumer, pose, -PYRAMID_HALF, PYRAMID_BASE_Y, -PYRAMID_HALF, 0F, PYRAMID_APEX_Y, 0F, 0F, PYRAMID_APEX_Y, 0F, PYRAMID_HALF, PYRAMID_BASE_Y, -PYRAMID_HALF, COLOR_TOP, 0F, 0.7F, -0.7F, packedLight);
        quad(consumer, pose, PYRAMID_HALF, PYRAMID_BASE_Y, -PYRAMID_HALF, 0F, PYRAMID_APEX_Y, 0F, 0F, PYRAMID_APEX_Y, 0F, PYRAMID_HALF, PYRAMID_BASE_Y, PYRAMID_HALF, COLOR_RIGHT, 0.7F, 0.7F, 0F, packedLight);
        quad(consumer, pose, PYRAMID_HALF, PYRAMID_BASE_Y, PYRAMID_HALF, 0F, PYRAMID_APEX_Y, 0F, 0F, PYRAMID_APEX_Y, 0F, -PYRAMID_HALF, PYRAMID_BASE_Y, PYRAMID_HALF, COLOR_FRONT, 0F, 0.7F, 0.7F, packedLight);
        quad(consumer, pose, -PYRAMID_HALF, PYRAMID_BASE_Y, PYRAMID_HALF, 0F, PYRAMID_APEX_Y, 0F, 0F, PYRAMID_APEX_Y, 0F, -PYRAMID_HALF, PYRAMID_BASE_Y, -PYRAMID_HALF, COLOR_LEFT, -0.7F, 0.7F, 0F, packedLight);
    }

    private static void renderFrontChest(final PoseStack.Pose pose, final VertexConsumer consumer, final int packedLight) {
        final float minX = -0.14F;
        final float maxX = 0.14F;
        final float minY = 0F;
        final float maxY = 0.34F;
        final float minZ = CHEST_MIN_Z;
        final float maxZ = 0.56F;
        quad(consumer, pose, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, COLOR_CHEST_DARK, 0F, 0F, -1F, packedLight);
        quad(consumer, pose, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, minY, maxZ, COLOR_CHEST, 0F, 0F, 1F, packedLight);
        quad(consumer, pose, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, minX, minY, minZ, COLOR_CHEST_DARK, -1F, 0F, 0F, packedLight);
        quad(consumer, pose, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, COLOR_CHEST, 1F, 0F, 0F, packedLight);
        quad(consumer, pose, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, COLOR_CHEST, 0F, 1F, 0F, packedLight);
        quad(consumer, pose, minX, minY, maxZ, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, COLOR_CHEST_DARK, 0F, -1F, 0F, packedLight);
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
