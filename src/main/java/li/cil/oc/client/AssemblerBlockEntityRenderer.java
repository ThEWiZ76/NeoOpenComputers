package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.blockentity.AssemblerBlockEntity;
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

public final class AssemblerBlockEntityRenderer implements BlockEntityRenderer<AssemblerBlockEntity> {
    private static final float MIN = -0.005F;
    private static final float MAX = 1.005F;
    private static final float TOP_Y = 1.005F;
    private static final float SIDE_OFFSET = 0.005F;
    private static final ResourceLocation TOP_ON_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "block/overlay/assembler_top_on");
    private static final ResourceLocation SIDE_ON_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "block/overlay/assembler_side_on");
    private static final ResourceLocation SIDE_ASSEMBLING_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "block/overlay/assembler_side_assembling");

    public AssemblerBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        final AssemblerBlockEntity assembler,
        final float partialTick,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight,
        final int packedOverlay) {
        renderTopOverlay(poseStack, bufferSource, packedLight);
        renderSideOnOverlays(poseStack, bufferSource, packedLight);
        if (assembler.isVisuallyAssembling()) {
            renderAssemblingOverlays(poseStack, bufferSource, packedLight);
        }
    }

    static ResourceLocation topOnTexture() {
        return TOP_ON_TEXTURE;
    }

    static ResourceLocation sideOnTexture() {
        return SIDE_ON_TEXTURE;
    }

    static ResourceLocation sideAssemblingTexture() {
        return SIDE_ASSEMBLING_TEXTURE;
    }

    private static void renderTopOverlay(final PoseStack poseStack, final MultiBufferSource bufferSource, final int packedLight) {
        final VertexConsumer consumer = consumer(bufferSource, TOP_ON_TEXTURE);
        final PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, MIN, TOP_Y, MAX, 0F, 1F, Direction.UP, packedLight);
        vertex(consumer, pose, MAX, TOP_Y, MAX, 1F, 1F, Direction.UP, packedLight);
        vertex(consumer, pose, MAX, TOP_Y, MIN, 1F, 0F, Direction.UP, packedLight);
        vertex(consumer, pose, MIN, TOP_Y, MIN, 0F, 0F, Direction.UP, packedLight);
    }

    private static void renderSideOnOverlays(final PoseStack poseStack, final MultiBufferSource bufferSource, final int packedLight) {
        renderSideOverlay(poseStack, bufferSource, SIDE_ON_TEXTURE, Direction.NORTH, packedLight);
        renderSideOverlay(poseStack, bufferSource, SIDE_ON_TEXTURE, Direction.EAST, packedLight);
        renderSideOverlay(poseStack, bufferSource, SIDE_ON_TEXTURE, Direction.SOUTH, packedLight);
        renderSideOverlay(poseStack, bufferSource, SIDE_ON_TEXTURE, Direction.WEST, packedLight);
    }

    private static void renderAssemblingOverlays(final PoseStack poseStack, final MultiBufferSource bufferSource, final int packedLight) {
        renderSideOverlay(poseStack, bufferSource, SIDE_ASSEMBLING_TEXTURE, Direction.NORTH, packedLight);
        renderSideOverlay(poseStack, bufferSource, SIDE_ASSEMBLING_TEXTURE, Direction.EAST, packedLight);
        renderSideOverlay(poseStack, bufferSource, SIDE_ASSEMBLING_TEXTURE, Direction.SOUTH, packedLight);
        renderSideOverlay(poseStack, bufferSource, SIDE_ASSEMBLING_TEXTURE, Direction.WEST, packedLight);
    }

    private static void renderSideOverlay(
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final ResourceLocation texture,
        final Direction side,
        final int packedLight) {
        final VertexConsumer consumer = consumer(bufferSource, texture);
        final PoseStack.Pose pose = poseStack.last();
        switch (side) {
            case NORTH -> {
                vertex(consumer, pose, MAX, MAX, -SIDE_OFFSET, 0F, 0F, side, packedLight);
                vertex(consumer, pose, MIN, MAX, -SIDE_OFFSET, 1F, 0F, side, packedLight);
                vertex(consumer, pose, MIN, MIN, -SIDE_OFFSET, 1F, 1F, side, packedLight);
                vertex(consumer, pose, MAX, MIN, -SIDE_OFFSET, 0F, 1F, side, packedLight);
            }
            case SOUTH -> {
                vertex(consumer, pose, MIN, MAX, 1F + SIDE_OFFSET, 0F, 0F, side, packedLight);
                vertex(consumer, pose, MAX, MAX, 1F + SIDE_OFFSET, 1F, 0F, side, packedLight);
                vertex(consumer, pose, MAX, MIN, 1F + SIDE_OFFSET, 1F, 1F, side, packedLight);
                vertex(consumer, pose, MIN, MIN, 1F + SIDE_OFFSET, 0F, 1F, side, packedLight);
            }
            case WEST -> {
                vertex(consumer, pose, -SIDE_OFFSET, MAX, MIN, 0F, 0F, side, packedLight);
                vertex(consumer, pose, -SIDE_OFFSET, MAX, MAX, 1F, 0F, side, packedLight);
                vertex(consumer, pose, -SIDE_OFFSET, MIN, MAX, 1F, 1F, side, packedLight);
                vertex(consumer, pose, -SIDE_OFFSET, MIN, MIN, 0F, 1F, side, packedLight);
            }
            case EAST -> {
                vertex(consumer, pose, 1F + SIDE_OFFSET, MAX, MAX, 0F, 0F, side, packedLight);
                vertex(consumer, pose, 1F + SIDE_OFFSET, MAX, MIN, 1F, 0F, side, packedLight);
                vertex(consumer, pose, 1F + SIDE_OFFSET, MIN, MIN, 1F, 1F, side, packedLight);
                vertex(consumer, pose, 1F + SIDE_OFFSET, MIN, MAX, 0F, 1F, side, packedLight);
            }
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
        final Direction normal,
        final int packedLight) {
        consumer.addVertex(pose, x, y, z)
            .setColor(0xFFFFFFFF)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(packedLight)
            .setNormal(pose, normal.getStepX(), normal.getStepY(), normal.getStepZ());
    }
}
