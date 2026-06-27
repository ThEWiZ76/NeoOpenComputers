package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc.common.block.ScreenBlock;
import li.cil.oc.common.blockentity.ScreenBlockEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public final class ScreenBlockEntityRenderer implements BlockEntityRenderer<ScreenBlockEntity> {
    private static final float TEXT_SCALE = 0.0105F;
    private static final int LINE_HEIGHT = 9;
    private static final int DEFAULT_COLOR = 0xFFFFFF;

    private final Font font;

    public ScreenBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
        font = context.getFont();
    }

    @Override
    public void render(final ScreenBlockEntity screen, final float partialTick, final PoseStack poseStack, final MultiBufferSource bufferSource, final int packedLight, final int packedOverlay) {
        if (!screen.isRenderOrigin() || !screen.renderText() || !screen.getPowerState()) {
            return;
        }

        poseStack.pushPose();
        orientToScreenFace(screen, poseStack);
        poseStack.scale(TEXT_SCALE * screen.renderBlockWidth(), -TEXT_SCALE * screen.renderBlockHeight(), TEXT_SCALE);
        for (int row = 0; row < screen.renderHeight(); row++) {
            final String line = line(screen, row);
            if (!line.isBlank()) {
                font.drawInBatch(line, 0, row * LINE_HEIGHT, DEFAULT_COLOR, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
            }
        }
        poseStack.popPose();
    }

    private static void orientToScreenFace(final ScreenBlockEntity screen, final PoseStack poseStack) {
        final BlockState state = screen.getBlockState();
        final Direction pitch = ScreenBlock.pitch(state);
        final Direction yaw = ScreenBlock.yaw(state);
        poseStack.translate(0.5D, 0.5D, 0.5D);
        switch (yaw) {
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(-90));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            default -> {
            }
        }
        switch (pitch) {
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
            case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            default -> {
            }
        }
        poseStack.translate(-0.42D, -0.28D + screen.renderBlockHeight(), -0.505D);
    }

    private static String line(final ScreenBlockEntity screen, final int row) {
        final StringBuilder builder = new StringBuilder(screen.renderWidth());
        for (int column = 0; column < screen.renderWidth(); column++) {
            builder.appendCodePoint(screen.getCodePoint(column, row));
        }
        return builder.toString().stripTrailing();
    }
}
