package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.block.ScreenBlock;
import li.cil.oc.common.blockentity.ScreenBlockEntity;
import net.minecraft.client.Minecraft;
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
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public final class ScreenBlockEntityRenderer implements BlockEntityRenderer<ScreenBlockEntity> {
    private static final int LINE_HEIGHT = 9;
    private static final int CELL_WIDTH = 6;
    private static final float SCREEN_BORDER = 2.25F / 16F;
    private static final int SCREEN_TIER1_COLOR = 0xABABAB;
    private static final int SCREEN_TIER2_COLOR = 0xFFFF66;
    private static final int SCREEN_TIER3_COLOR = 0x66FFFF;
    private static final float SCREEN_FRONT_Z = 0.53F;
    private static final float SCREEN_TEXT_Z = 0.535F;
    private static final float SCREEN_SIDE_OFFSET = 0.531F;
    private static final String[] SINGLE_SIDE = {"b", "b", "b2", "b2", "b2", "b2"};
    private static final String[][] HORIZONTAL_FRONT = {
        {"fhb2", "fhm2", "fht2"},
        {"fhb", "fhm", "fht"}
    };
    private static final String[][][] HORIZONTAL_SIDE = {
        {
            {"bht", "bhb", "bht2", "bht2", "b2", "b2"},
            {"bhm", "bhm", "bhm2", "bhm2", "b", "b"},
            {"bhb", "bht", "bhb2", "bhb2", "b2", "b2"}
        },
        {
            {"bhb2", "bht2", "bht", "bhb", "b2", "b2"},
            {"bhm2", "bhm2", "bhm", "bhm", "b", "b"},
            {"bht2", "bhb2", "bhb", "bht", "b2", "b2"}
        }
    };
    private static final String[][] VERTICAL_FRONT = {
        {"fvt", "fvm", "fvb2"},
        {"fvt", "fvm", "fvb"}
    };
    private static final String[][][] VERTICAL_SIDE = {
        {
            {"b", "b", "bvt", "bvt", "bvt", "bvt"},
            {"b", "b", "bvm", "bvm", "bvm", "bvm"},
            {"b", "b", "bvb2", "bvb2", "bvb2", "bvb2"}
        },
        {
            {"b2", "b2", "bvt", "bvt", "bht2", "bhb2"},
            {"b", "b", "bvm", "bvm", "bhm2", "bhm2"},
            {"b2", "b2", "bvb", "bvb", "bhb2", "bht2"}
        }
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
    private static final String[][][][] MULTI_SIDE = {
        {
            {
                {"bht", "bhb", "btl", "btr", "bvb", "bvt"},
                {"bhm", "bhm", "btm", "btm", "b", "b"},
                {"bhb", "bht", "btr", "btl", "bvt", "bvb"}
            },
            {
                {"b", "b", "bml", "bmr", "bvm", "bvm"},
                {"b", "b", "bmm", "bmm", "b", "b"},
                {"b", "b", "bmr", "bml", "bvm", "bvt"}
            },
            {
                {"bht", "bhb", "bbl2", "bbr2", "bvt", "bvb2"},
                {"bhm", "bhm", "bbm2", "bbm2", "b", "b"},
                {"bhb", "bht", "bbr2", "bbl2", "bvb2", "bvt"}
            }
        },
        {
            {
                {"bhb2", "bht2", "btl", "btr", "bht2", "bhb2"},
                {"bhm2", "bhm2", "btm", "btm", "b", "b"},
                {"bht2", "bhb2", "btr", "btl", "bht2", "bhb2"}
            },
            {
                {"b", "b", "bml", "bml", "bhm2", "bhm2"},
                {"b", "b", "bmm", "bmm", "b", "b"},
                {"b", "b", "bmr", "bmr", "bhm2", "bhm2"}
            },
            {
                {"bhb2", "bht2", "bbl", "bbr", "bhb2", "bht2"},
                {"bhm2", "bhm2", "bbm", "bbm", "b", "b"},
                {"bht2", "bhb2", "bbr", "bbl", "bhb2", "bht2"}
            }
        }
    };

    public ScreenBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(final ScreenBlockEntity screen, final float partialTick, final PoseStack poseStack, final MultiBufferSource bufferSource, final int packedLight, final int packedOverlay) {
        poseStack.pushPose();
        orientToScreenBlockFace(screen, poseStack);
        renderScreenFaces(screen, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();

        if (!screen.isRenderOrigin() || !screen.renderText() || !screen.getPowerState()) {
            return;
        }
        final float textAlpha = screenTextAlphaForPlayer(screen);
        if (textAlpha <= 0F) {
            return;
        }

        poseStack.pushPose();
        orientToScreenText(screen, poseStack);
        final TextLayout layout = textLayout(screen.renderBlockWidth(), screen.renderBlockHeight(), screen.renderWidth(), screen.renderHeight());
        poseStack.translate(layout.x(), layout.y(), SCREEN_TEXT_Z);
        poseStack.scale(layout.scale(), -layout.scale(), layout.scale());
        for (int row = 0; row < screen.renderHeight(); row++) {
            final List<String> cells = lineCells(line(screen, row), screen.renderWidth());
            for (int column = 0; column < cells.size(); column++) {
                final String cell = cells.get(column);
                final int backgroundColor = textColorWithAlpha(screen.getBackgroundColor(column, row), textAlpha);
                renderCellBackground(poseStack, bufferSource, column, row, backgroundColor);
                if (!cell.isBlank()) {
                    final int textColor = textColorWithAlpha(screen.getForegroundColor(column, row), textAlpha);
                    TerminalFont.drawWorldCell(poseStack, bufferSource, cell, column, row, textColor, SCREEN_TEXT_Z);
                }
            }
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(final ScreenBlockEntity screen) {
        if (!screen.isRenderOrigin()) {
            return BlockEntityRenderer.super.getRenderBoundingBox(screen);
        }
        final BlockState state = screen.getBlockState();
        return renderBounds(
            screen.getBlockPos(),
            localRight(state),
            ScreenBlock.up(state),
            screen.renderBlockWidth(),
            screen.renderBlockHeight());
    }

    private static void renderScreenFaces(
        final ScreenBlockEntity screen,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight,
        final int packedOverlay) {
        renderScreenFace(screen, poseStack, bufferSource, packedLight, packedOverlay, Direction.SOUTH);
    }

    private static void renderScreenFace(
        final ScreenBlockEntity screen,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight,
        final int packedOverlay,
        final Direction face) {
        if (!shouldRenderScreenFace(screen, face)) {
            return;
        }
        final Direction pitch = ScreenBlock.pitch(screen.getBlockState());
        final TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(screenTexture(
            pitch != Direction.NORTH,
            screen.renderBlockWidth(),
            screen.renderBlockHeight(),
            screen.localBlockX(),
            screen.localBlockY(),
            face));
        final VertexConsumer consumer = sprite.wrap(bufferSource.getBuffer(RenderType.cutout()));
        final PoseStack.Pose pose = poseStack.last();
        final int color = 0xFF000000 | screen.getRenderColor();
        switch (face) {
            case DOWN -> {
                vertex(consumer, pose, -0.5F, -SCREEN_SIDE_OFFSET, 0.5F, 0F, 0F, color, packedLight, packedOverlay, face);
                vertex(consumer, pose, 0.5F, -SCREEN_SIDE_OFFSET, 0.5F, 1F, 0F, color, packedLight, packedOverlay, face);
                vertex(consumer, pose, 0.5F, -SCREEN_SIDE_OFFSET, -0.5F, 1F, 1F, color, packedLight, packedOverlay, face);
                vertex(consumer, pose, -0.5F, -SCREEN_SIDE_OFFSET, -0.5F, 0F, 1F, color, packedLight, packedOverlay, face);
            }
            case UP -> {
                vertex(consumer, pose, -0.5F, SCREEN_SIDE_OFFSET, -0.5F, 0F, 0F, color, packedLight, packedOverlay, face);
                vertex(consumer, pose, 0.5F, SCREEN_SIDE_OFFSET, -0.5F, 1F, 0F, color, packedLight, packedOverlay, face);
                vertex(consumer, pose, 0.5F, SCREEN_SIDE_OFFSET, 0.5F, 1F, 1F, color, packedLight, packedOverlay, face);
                vertex(consumer, pose, -0.5F, SCREEN_SIDE_OFFSET, 0.5F, 0F, 1F, color, packedLight, packedOverlay, face);
            }
            case NORTH -> {
                vertex(consumer, pose, 0.5F, -0.5F, -SCREEN_SIDE_OFFSET, 0F, 1F, color, packedLight, packedOverlay, face);
                vertex(consumer, pose, 0.5F, 0.5F, -SCREEN_SIDE_OFFSET, 0F, 0F, color, packedLight, packedOverlay, face);
                vertex(consumer, pose, -0.5F, 0.5F, -SCREEN_SIDE_OFFSET, 1F, 0F, color, packedLight, packedOverlay, face);
                vertex(consumer, pose, -0.5F, -0.5F, -SCREEN_SIDE_OFFSET, 1F, 1F, color, packedLight, packedOverlay, face);
            }
            case SOUTH -> {
                vertex(consumer, pose, -0.5F, -0.5F, SCREEN_FRONT_Z, 0F, 1F, color, packedLight, packedOverlay, face);
                vertex(consumer, pose, -0.5F, 0.5F, SCREEN_FRONT_Z, 0F, 0F, color, packedLight, packedOverlay, face);
                vertex(consumer, pose, 0.5F, 0.5F, SCREEN_FRONT_Z, 1F, 0F, color, packedLight, packedOverlay, face);
                vertex(consumer, pose, 0.5F, -0.5F, SCREEN_FRONT_Z, 1F, 1F, color, packedLight, packedOverlay, face);
            }
            case WEST -> {
                vertex(consumer, pose, -SCREEN_SIDE_OFFSET, -0.5F, -0.5F, 0F, 1F, color, packedLight, packedOverlay, face);
                vertex(consumer, pose, -SCREEN_SIDE_OFFSET, 0.5F, -0.5F, 0F, 0F, color, packedLight, packedOverlay, face);
                vertex(consumer, pose, -SCREEN_SIDE_OFFSET, 0.5F, 0.5F, 1F, 0F, color, packedLight, packedOverlay, face);
                vertex(consumer, pose, -SCREEN_SIDE_OFFSET, -0.5F, 0.5F, 1F, 1F, color, packedLight, packedOverlay, face);
            }
            case EAST -> {
                vertex(consumer, pose, SCREEN_SIDE_OFFSET, -0.5F, 0.5F, 0F, 1F, color, packedLight, packedOverlay, face);
                vertex(consumer, pose, SCREEN_SIDE_OFFSET, 0.5F, 0.5F, 0F, 0F, color, packedLight, packedOverlay, face);
                vertex(consumer, pose, SCREEN_SIDE_OFFSET, 0.5F, -0.5F, 1F, 0F, color, packedLight, packedOverlay, face);
                vertex(consumer, pose, SCREEN_SIDE_OFFSET, -0.5F, -0.5F, 1F, 1F, color, packedLight, packedOverlay, face);
            }
        }
    }

    private static boolean shouldRenderScreenFace(final ScreenBlockEntity screen, final Direction localFace) {
        if (!canCullInternalMultiblockFace(localFace) || screen.getLevel() == null) {
            return true;
        }
        final BlockState state = screen.getBlockState();
        final Direction worldFace = localFaceDirection(ScreenBlock.pitch(state), ScreenBlock.yaw(state), localFace);
        if (worldFace == null) {
            return true;
        }
        final BlockState neighborState = screen.getLevel().getBlockState(screen.getBlockPos().relative(worldFace));
        return !(neighborState.getBlock() instanceof ScreenBlock neighborBlock)
            || !(state.getBlock() instanceof ScreenBlock screenBlock)
            || neighborBlock.tier() != screenBlock.tier()
            || ScreenBlock.pitch(neighborState) != ScreenBlock.pitch(state)
            || ScreenBlock.yaw(neighborState) != ScreenBlock.yaw(state)
            || !(screen.getLevel().getBlockEntity(screen.getBlockPos().relative(worldFace)) instanceof ScreenBlockEntity neighbor)
            || neighbor.getRenderColor() != screen.getRenderColor();
    }

    static boolean canCullInternalMultiblockFace(final Direction localFace) {
        return localFace == Direction.EAST
            || localFace == Direction.WEST
            || localFace == Direction.UP
            || localFace == Direction.DOWN;
    }

    static Direction localFaceDirection(final Direction pitch, final Direction yaw, final Direction localFace) {
        return switch (localFace) {
            case EAST -> ScreenBlock.localRight(yaw);
            case WEST -> ScreenBlock.localRight(yaw).getOpposite();
            case UP -> pitch != null && pitch.getAxis().isVertical() ? yaw : Direction.UP;
            case DOWN -> (pitch != null && pitch.getAxis().isVertical() ? yaw : Direction.UP).getOpposite();
            case SOUTH -> pitch != null && pitch.getAxis().isVertical() ? pitch : yaw;
            case NORTH -> (pitch != null && pitch.getAxis().isVertical() ? pitch : yaw).getOpposite();
        };
    }

    static ResourceLocation screenFrontTexture(final boolean horizontalPitch, final int width, final int height, final int localX, final int localY) {
        return screenTexture(horizontalPitch, width, height, localX, localY, Direction.SOUTH);
    }

    static ResourceLocation screenTexture(final boolean horizontalPitch, final int width, final int height, final int localX, final int localY, final Direction localFace) {
        final String texture;
        final int pitch = horizontalPitch ? 1 : 0;
        final Direction face = localFace == null ? Direction.SOUTH : localFace;
        if (face == Direction.SOUTH && width <= 1 && height <= 1) {
            texture = horizontalPitch ? "f2" : "f";
        } else if (face == Direction.SOUTH && width <= 1) {
            texture = VERTICAL_FRONT[pitch][xy2part(localY, height - 1)];
        } else if (face == Direction.SOUTH && height <= 1) {
            texture = HORIZONTAL_FRONT[pitch][xy2part(localX, width - 1)];
        } else if (face == Direction.SOUTH) {
            texture = MULTI_FRONT[pitch][xy2part(localY, height - 1)][xy2part(localX, width - 1)];
        } else if (width <= 1 && height <= 1) {
            texture = SINGLE_SIDE[face.get3DDataValue()];
        } else if (width <= 1) {
            texture = VERTICAL_SIDE[pitch][xy2part(localY, height - 1)][face.get3DDataValue()];
        } else if (height <= 1) {
            texture = HORIZONTAL_SIDE[pitch][xy2part(localX, width - 1)][face.get3DDataValue()];
        } else {
            texture = MULTI_SIDE[pitch][xy2part(localY, height - 1)][xy2part(localX, width - 1)][face.get3DDataValue()];
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
        vertex(consumer, pose, x, y, z, u, v, color, packedLight, packedOverlay, Direction.SOUTH);
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
        final int packedOverlay,
        final Direction normal) {
        consumer.addVertex(pose, x, y, z)
            .setColor(color)
            .setUv(u, v)
            .setOverlay(packedOverlay)
            .setLight(packedLight)
            .setNormal(pose, normal.getStepX(), normal.getStepY(), normal.getStepZ());
    }

    private static void orientToScreenText(final ScreenBlockEntity screen, final PoseStack poseStack) {
        orientToScreenBlockFace(screen, poseStack);
    }

    private static void orientToScreenBlockFace(final ScreenBlockEntity screen, final PoseStack poseStack) {
        final BlockState state = screen.getBlockState();
        final Direction pitch = ScreenBlock.pitch(state);
        final Direction yaw = ScreenBlock.yaw(state);
        poseStack.translate(0.5D, 0.5D, 0.5D);
        final int yawRotation = yawRotationDegrees(yaw);
        if (yawRotation != 0) {
            poseStack.mulPose(Axis.YP.rotationDegrees(yawRotation));
        }
        switch (pitch) {
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
            case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            default -> {
            }
        }
    }

    static int yawRotationDegrees(final Direction yaw) {
        return switch (yaw) {
            case NORTH -> 180;
            case EAST -> 90;
            case WEST -> -90;
            default -> 0;
        };
    }

    static Direction renderFrontDirection(final Direction yaw) {
        return yaw == null ? Direction.SOUTH : yaw;
    }

    static Direction renderRightDirection(final Direction yaw) {
        return ScreenBlock.localRight(yaw);
    }

    static float screenFrontZ() {
        return SCREEN_FRONT_Z;
    }

    static float screenTextZ() {
        return SCREEN_TEXT_Z;
    }

    private static float screenTextAlphaForPlayer(final ScreenBlockEntity screen) {
        final var player = Minecraft.getInstance().player;
        if (player == null) {
            return 0F;
        }
        final BlockState state = screen.getBlockState();
        final AABB bounds = renderBounds(screen.getBlockPos(), localRight(state), ScreenBlock.up(state), screen.renderBlockWidth(), screen.renderBlockHeight());
        if (!playerIsInFrontOfScreen(ScreenBlock.facing(state), bounds, player.getX(), player.getEyeY(), player.getZ())) {
            return 0F;
        }
        final double distance = screenTextDistanceSq(bounds, player.getX(), player.getEyeY(), player.getZ())
            / Math.max(1D, Math.min(screen.renderBlockWidth(), screen.renderBlockHeight()));
        return screenTextAlpha(distance, ModSettings.screenTextFadeStartDistance(), ModSettings.maxScreenTextRenderDistance());
    }

    private static boolean shouldRenderTextForPlayer(final ScreenBlockEntity screen) {
        return screenTextAlphaForPlayer(screen) > 0F;
    }

    static float screenTextAlpha(final double distanceSq, final double fadeStartDistance, final double maxRenderDistance) {
        final double safeFadeStartDistance = Math.max(0D, fadeStartDistance);
        final double safeMaxRenderDistance = Math.max(0D, maxRenderDistance);
        final double fadeDistanceSq = safeFadeStartDistance * safeFadeStartDistance;
        final double maxRenderDistanceSq = safeMaxRenderDistance * safeMaxRenderDistance;
        if (distanceSq > maxRenderDistanceSq) {
            return 0F;
        }
        if (distanceSq <= fadeDistanceSq || maxRenderDistanceSq <= fadeDistanceSq) {
            return 1F;
        }
        return (float) Math.max(0D, 1D - ((distanceSq - fadeDistanceSq) / (maxRenderDistanceSq - fadeDistanceSq)));
    }

    static double screenTextDistanceSq(final AABB bounds, final double playerX, final double playerY, final double playerZ) {
        final double dx = outsideDistance(playerX, bounds.minX, bounds.maxX);
        final double dy = outsideDistance(playerY, bounds.minY, bounds.maxY);
        final double dz = outsideDistance(playerZ, bounds.minZ, bounds.maxZ);
        return dx * dx + dy * dy + dz * dz;
    }

    private static double outsideDistance(final double value, final double min, final double max) {
        if (value < min) {
            return min - value;
        }
        if (value > max) {
            return value - max;
        }
        return 0D;
    }

    static int textColorWithAlpha(final int color, final float alpha) {
        final int clampedAlpha = Math.max(0, Math.min(255, Math.round(alpha * 255F)));
        if (clampedAlpha == 0) {
            return 0;
        }
        return (clampedAlpha << 24) | (color & 0x00FFFFFF);
    }

    private static void renderCellBackground(
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int column,
        final int row,
        final int color) {
        if ((color & 0xFF000000) == 0 || (color & 0x00FFFFFF) == 0) {
            return;
        }
        final float x = column * CELL_WIDTH;
        final float y = row * LINE_HEIGHT;
        final float z = SCREEN_TEXT_Z - 0.001F;
        final VertexConsumer consumer = bufferSource.getBuffer(RenderType.gui());
        final PoseStack.Pose pose = poseStack.last();
        consumer.addVertex(pose, x, y + LINE_HEIGHT, z).setColor(color);
        consumer.addVertex(pose, x + CELL_WIDTH, y + LINE_HEIGHT, z).setColor(color);
        consumer.addVertex(pose, x + CELL_WIDTH, y, z).setColor(color);
        consumer.addVertex(pose, x, y, z).setColor(color);
    }

    static boolean playerIsInFrontOfScreen(final Direction front, final AABB bounds, final double playerX, final double playerY, final double playerZ) {
        final double centerX = (bounds.minX + bounds.maxX) * 0.5D;
        final double centerY = (bounds.minY + bounds.maxY) * 0.5D;
        final double centerZ = (bounds.minZ + bounds.maxZ) * 0.5D;
        return (playerX - centerX) * front.getStepX()
            + (playerY - centerY) * front.getStepY()
            + (playerZ - centerZ) * front.getStepZ() >= 0D;
    }

    static int centeredCellOffset(final int glyphWidth) {
        return 0;
    }

    static float textScale(final int blockWidth, final int blockHeight, final int renderWidth, final int renderHeight) {
        final int safeBlockWidth = Math.max(1, blockWidth);
        final int safeBlockHeight = Math.max(1, blockHeight);
        final int safeRenderWidth = Math.max(1, renderWidth);
        final int safeRenderHeight = Math.max(1, renderHeight);
        final float innerWidth = Math.max(0.01F, safeBlockWidth - SCREEN_BORDER * 2F);
        final float innerHeight = Math.max(0.01F, safeBlockHeight - SCREEN_BORDER * 2F);
        final float pixelWidth = safeRenderWidth * CELL_WIDTH;
        final float pixelHeight = safeRenderHeight * LINE_HEIGHT;
        return Math.min(innerWidth / pixelWidth, innerHeight / pixelHeight);
    }

    static TextLayout textLayout(final int blockWidth, final int blockHeight, final int renderWidth, final int renderHeight) {
        final int safeBlockWidth = Math.max(1, blockWidth);
        final int safeBlockHeight = Math.max(1, blockHeight);
        final int safeRenderWidth = Math.max(1, renderWidth);
        final int safeRenderHeight = Math.max(1, renderHeight);
        final float scale = textScale(safeBlockWidth, safeBlockHeight, safeRenderWidth, safeRenderHeight);
        final float innerWidth = Math.max(0.01F, safeBlockWidth - SCREEN_BORDER * 2F);
        final float innerHeight = Math.max(0.01F, safeBlockHeight - SCREEN_BORDER * 2F);
        final float usedWidth = safeRenderWidth * CELL_WIDTH * scale;
        final float usedHeight = safeRenderHeight * LINE_HEIGHT * scale;
        final float x = -0.5F + SCREEN_BORDER + (innerWidth - usedWidth) * 0.5F;
        final float y = -0.5F + SCREEN_BORDER + innerHeight - (innerHeight - usedHeight) * 0.5F;
        return new TextLayout(x, y, scale);
    }

    record TextLayout(float x, float y, float scale) {
    }

    static AABB renderBounds(final net.minecraft.core.BlockPos origin, final Direction right, final Direction up, final int width, final int height) {
        final int clampedWidth = Math.max(1, width);
        final int clampedHeight = Math.max(1, height);
        final net.minecraft.core.BlockPos oppositeCorner = origin
            .relative(right, clampedWidth - 1)
            .relative(up, clampedHeight - 1);
        final int minX = Math.min(origin.getX(), oppositeCorner.getX());
        final int minY = Math.min(origin.getY(), oppositeCorner.getY());
        final int minZ = Math.min(origin.getZ(), oppositeCorner.getZ());
        final int maxX = Math.max(origin.getX(), oppositeCorner.getX()) + 1;
        final int maxY = Math.max(origin.getY(), oppositeCorner.getY()) + 1;
        final int maxZ = Math.max(origin.getZ(), oppositeCorner.getZ()) + 1;
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static Direction localRight(final BlockState state) {
        return ScreenBlock.localRight(state);
    }

    private static String line(final ScreenBlockEntity screen, final int row) {
        final StringBuilder builder = new StringBuilder(screen.renderWidth());
        for (int column = 0; column < screen.renderWidth(); column++) {
            builder.appendCodePoint(screen.getCodePoint(column, row));
        }
        return builder.toString().stripTrailing();
    }

    static List<String> lineCells(final String line, final int width) {
        if (width <= 0) {
            return List.of();
        }
        final List<String> cells = new ArrayList<>(width);
        final String value = line == null ? "" : line;
        int offset = 0;
        for (int column = 0; column < width; column++) {
            if (offset < value.length()) {
                final int codePoint = value.codePointAt(offset);
                cells.add(new String(Character.toChars(codePoint)));
                offset += Character.charCount(codePoint);
            } else {
                cells.add(" ");
            }
        }
        return cells;
    }
}
