package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import li.cil.oc.common.blockentity.HologramBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class HologramBlockEntityRenderer implements BlockEntityRenderer<HologramBlockEntity> {
    private static final float BASE_ALPHA = 0.75F;

    public HologramBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        final HologramBlockEntity hologram,
        final float partialTick,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight,
        final int packedOverlay) {
        if (!hologram.hasPower()) {
            return;
        }

        final VertexConsumer consumer = bufferSource.getBuffer(RenderType.debugQuads());
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        applyRotation(hologram, partialTick, poseStack);
        final double scale = hologram.renderScale();
        poseStack.scale((float) (scale / 16D), (float) (scale / 16D), (float) (scale / 16D));
        poseStack.translate(
            hologram.renderTranslationX() * hologram.renderWidth() / 16D - 1.5D,
            hologram.renderTranslationY() * hologram.renderHeight() / 16D,
            hologram.renderTranslationZ() * hologram.renderWidth() / 16D - 1.5D);

        for (int x = 0; x < hologram.renderWidth(); x++) {
            for (int z = 0; z < hologram.renderWidth(); z++) {
                for (int y = 0; y < hologram.renderHeight(); y++) {
                    final int value = hologram.renderColor(x, y, z);
                    if (value <= 0) {
                        continue;
                    }
                    final int color = colorWithAlpha(hologram.renderPaletteColor(value - 1), BASE_ALPHA);
                    for (final Direction face : visibleFaces(x, y, z, hologram::renderColor)) {
                        renderFace(poseStack, consumer, x, y, z, face, color);
                    }
                }
            }
        }
        poseStack.popPose();
    }

    static List<Direction> visibleFaces(final int x, final int y, final int z, final VoxelAccess access) {
        final List<Direction> faces = new ArrayList<>(6);
        for (final Direction face : Direction.values()) {
            if (access.color(x + face.getStepX(), y + face.getStepY(), z + face.getStepZ()) == 0) {
                faces.add(face);
            }
        }
        return faces;
    }

    static int colorWithAlpha(final int rgb, final float alpha) {
        final int a = Math.max(0, Math.min(255, Math.round(alpha * 255F)));
        if (a == 0) {
            return 0;
        }
        return a << 24 | rgb & 0xFFFFFF;
    }

    private static void applyRotation(final HologramBlockEntity hologram, final float partialTick, final PoseStack poseStack) {
        final float angle = hologram.renderRotationAngle();
        if (angle != 0F) {
            poseStack.mulPose(Axis.of(new Vector3f(
                hologram.renderRotationX(),
                hologram.renderRotationY(),
                hologram.renderRotationZ())).rotationDegrees(angle));
        }
        final float speed = hologram.renderRotationSpeed();
        if (speed != 0F && hologram.getLevel() != null) {
            final float dynamicAngle = speed * (hologram.getLevel().getGameTime() % (360 * 20 - 1) + partialTick) / 20F;
            poseStack.mulPose(Axis.of(new Vector3f(
                hologram.renderRotationSpeedX(),
                hologram.renderRotationSpeedY(),
                hologram.renderRotationSpeedZ())).rotationDegrees(dynamicAngle));
        }
    }

    private static void renderFace(
        final PoseStack poseStack,
        final VertexConsumer consumer,
        final int x,
        final int y,
        final int z,
        final Direction face,
        final int color) {
        final float minX = x;
        final float minY = y;
        final float minZ = z;
        final float maxX = x + 1F;
        final float maxY = y + 1F;
        final float maxZ = z + 1F;
        final float red = ((color >>> 16) & 0xFF) / 255F;
        final float green = ((color >>> 8) & 0xFF) / 255F;
        final float blue = (color & 0xFF) / 255F;
        final float alpha = ((color >>> 24) & 0xFF) / 255F;

        switch (face) {
            case SOUTH -> quad(poseStack, consumer, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, minY, maxZ, minX, minY, maxZ, red, green, blue, alpha);
            case NORTH -> quad(poseStack, consumer, maxX, maxY, minZ, minX, maxY, minZ, minX, minY, minZ, maxX, minY, minZ, red, green, blue, alpha);
            case EAST -> quad(poseStack, consumer, maxX, maxY, maxZ, maxX, maxY, minZ, maxX, minY, minZ, maxX, minY, maxZ, red, green, blue, alpha);
            case WEST -> quad(poseStack, consumer, minX, maxY, minZ, minX, maxY, maxZ, minX, minY, maxZ, minX, minY, minZ, red, green, blue, alpha);
            case UP -> quad(poseStack, consumer, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, red, green, blue, alpha);
            case DOWN -> quad(poseStack, consumer, maxX, minY, maxZ, maxX, minY, minZ, minX, minY, minZ, minX, minY, maxZ, red, green, blue, alpha);
        }
    }

    private static void quad(
        final PoseStack poseStack,
        final VertexConsumer consumer,
        final float x1,
        final float y1,
        final float z1,
        final float x2,
        final float y2,
        final float z2,
        final float x3,
        final float y3,
        final float z3,
        final float x4,
        final float y4,
        final float z4,
        final float red,
        final float green,
        final float blue,
        final float alpha) {
        consumer.addVertex(poseStack.last().pose(), x1, y1, z1).setColor(red, green, blue, alpha);
        consumer.addVertex(poseStack.last().pose(), x2, y2, z2).setColor(red, green, blue, alpha);
        consumer.addVertex(poseStack.last().pose(), x3, y3, z3).setColor(red, green, blue, alpha);
        consumer.addVertex(poseStack.last().pose(), x4, y4, z4).setColor(red, green, blue, alpha);
    }

    @FunctionalInterface
    interface VoxelAccess {
        int color(int x, int y, int z);
    }
}
