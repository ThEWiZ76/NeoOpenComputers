package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.block.ScreenBlock;
import li.cil.oc.common.blockentity.ScreenBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class ScreenBlockEntityRenderer implements BlockEntityRenderer<ScreenBlockEntity> {
    private static final float TEXT_SCALE = 0.0105F;
    private static final int LINE_HEIGHT = 9;
    private static final int DEFAULT_COLOR = 0xFFFFFF;
    private static final int SCREEN_TIER1_COLOR = 0xABABAB;
    private static final int SCREEN_TIER2_COLOR = 0xFFFF66;
    private static final int SCREEN_TIER3_COLOR = 0x66FFFF;
    private static final float SCREEN_FRONT_Z = -0.53F;
    private static final String[][] HORIZONTAL_FRONT = {
        {"fhb2", "fhm2", "fht2"},
        {"fhb", "fhm", "fht"}
    };
    private static final String[][] VERTICAL_FRONT = {
        {"fvt", "fvm", "fvb2"},
        {"fvt", "fvm", "fvb"}
    };
    private static final String[][][] MULTI_FRONT = {
        {
            {"ftr", "ftm", "ftl"},
            {"fmr", "fmm", "fml"},
            {"fbr2", "fbm2", "fbl2"}
        },
        {
            {"ftr", "ftm", "ftl"},
            {"fmr", "fmm", "fml"},
            {"fbr", "fbm", "fbl"}
        }
    };

    private final Font font;

    public ScreenBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
        font = context.getFont();
    }

    @Override
    public void render(final ScreenBlockEntity screen, final float partialTick, final PoseStack poseStack, final MultiBufferSource bufferSource, final int packedLight, final int packedOverlay) {
        poseStack.pushPose();
        orientToScreenBlockFace(screen, poseStack);
        renderScreenFront(screen, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();

        if (!screen.isRenderOrigin() || !screen.renderText() || !screen.getPowerState()) {
            return;
        }

        poseStack.pushPose();
        orientToScreenText(screen, poseStack);
        poseStack.scale(TEXT_SCALE * screen.renderBlockWidth(), -TEXT_SCALE * screen.renderBlockHeight(), TEXT_SCALE);
        for (int row = 0; row < screen.renderHeight(); row++) {
            final String line = line(screen, row);
            if (!line.isBlank()) {
                font.drawInBatch(line, 0, row * LINE_HEIGHT, DEFAULT_COLOR, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
            }
        }
        poseStack.popPose();
    }

    private static void renderScreenFront(
        final ScreenBlockEntity screen,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight,
        final int packedOverlay) {
        final TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(screenFrontTexture(screen));
        final VertexConsumer consumer = sprite.wrap(bufferSource.getBuffer(RenderType.cutout()));
        final PoseStack.Pose pose = poseStack.last();
        final int color = 0xFF000000 | screenTierColor(screen.getBlockState().getBlock());
        vertex(consumer, pose, -0.5F, -0.5F, SCREEN_FRONT_Z, 0F, 1F, color, packedLight, packedOverlay);
        vertex(consumer, pose, -0.5F, 0.5F, SCREEN_FRONT_Z, 0F, 0F, color, packedLight, packedOverlay);
        vertex(consumer, pose, 0.5F, 0.5F, SCREEN_FRONT_Z, 1F, 0F, color, packedLight, packedOverlay);
        vertex(consumer, pose, 0.5F, -0.5F, SCREEN_FRONT_Z, 1F, 1F, color, packedLight, packedOverlay);
    }

    static ResourceLocation screenFrontTexture(final boolean horizontalPitch, final int width, final int height, final int localX, final int localY) {
        final String texture;
        final int pitch = horizontalPitch ? 1 : 0;
        if (width <= 1 && height <= 1) {
            texture = horizontalPitch ? "f2" : "f";
        } else if (width <= 1) {
            texture = VERTICAL_FRONT[pitch][xy2part(localY, height - 1)];
        } else if (height <= 1) {
            texture = HORIZONTAL_FRONT[pitch][xy2part(localX, width - 1)];
        } else {
            texture = MULTI_FRONT[pitch][xy2part(localY, height - 1)][xy2part(localX, width - 1)];
        }
        return ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "block/screen/" + texture);
    }

    private static ResourceLocation screenFrontTexture(final ScreenBlockEntity screen) {
        final Direction pitch = ScreenBlock.pitch(screen.getBlockState());
        return screenFrontTexture(
            pitch != Direction.NORTH,
            screen.renderBlockWidth(),
            screen.renderBlockHeight(),
            screen.localBlockX(),
            screen.localBlockY());
    }

    private static int xy2part(final int value, final int high) {
        if (value <= 0) {
            return 2;
        }
        if (value >= high) {
            return 0;
        }
        return 1;
    }

    private static int screenTierColor(final Block block) {
        if (block == ModBlocks.SCREEN_TIER3.get()) {
            return SCREEN_TIER3_COLOR;
        }
        if (block == ModBlocks.SCREEN_TIER2.get()) {
            return SCREEN_TIER2_COLOR;
        }
        return SCREEN_TIER1_COLOR;
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
        final int packedLight,
        final int packedOverlay) {
        consumer.addVertex(pose, x, y, z)
            .setColor(color)
            .setUv(u, v)
            .setOverlay(packedOverlay)
            .setLight(packedLight)
            .setNormal(pose, 0F, 0F, -1F);
    }

    private static void orientToScreenText(final ScreenBlockEntity screen, final PoseStack poseStack) {
        orientToScreenBlockFace(screen, poseStack);
        poseStack.translate(-0.42D, -0.28D + screen.renderBlockHeight(), -0.505D);
    }

    private static void orientToScreenBlockFace(final ScreenBlockEntity screen, final PoseStack poseStack) {
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
    }

    private static String line(final ScreenBlockEntity screen, final int row) {
        final StringBuilder builder = new StringBuilder(screen.renderWidth());
        for (int column = 0; column < screen.renderWidth(); column++) {
            builder.appendCodePoint(screen.getCodePoint(column, row));
        }
        return builder.toString().stripTrailing();
    }
}
