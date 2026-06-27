package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.block.DiskDriveBlock;
import li.cil.oc.common.blockentity.DiskDriveBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class DiskDriveBlockEntityRenderer implements BlockEntityRenderer<DiskDriveBlockEntity> {
    private static final long ACTIVITY_VISIBLE_MILLIS = 400L;
    private static final ResourceLocation ACTIVITY_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "block/overlay/diskdrive_front_activity");

    private final ItemRenderer itemRenderer;

    public DiskDriveBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
        itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(
        final DiskDriveBlockEntity diskDrive,
        final float partialTick,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight,
        final int packedOverlay) {
        final ItemStack stack = diskDrive.getItem(DiskDriveBlockEntity.SLOT_FLOPPY);
        poseStack.pushPose();
        orientToDriveFront(diskDrive.getBlockState(), poseStack);
        if (!stack.isEmpty()) {
            renderInsertedMedia(diskDrive, stack, poseStack, bufferSource, packedLight);
        }
        if (System.currentTimeMillis() - diskDrive.getLastAccess() < ACTIVITY_VISIBLE_MILLIS) {
            renderActivityOverlay(poseStack, bufferSource, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private void renderInsertedMedia(
        final DiskDriveBlockEntity diskDrive,
        final ItemStack stack,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0D, 3.5D / 16D, 6D / 16D);
        poseStack.mulPose(Axis.XP.rotationDegrees(90F));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, diskDrive.getLevel(), 0);
        poseStack.popPose();
    }

    private static void renderActivityOverlay(
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight,
        final int packedOverlay) {
        final TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ACTIVITY_TEXTURE);
        final VertexConsumer consumer = sprite.wrap(bufferSource.getBuffer(RenderType.cutout()));
        final PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, -0.5F, -0.5F, 0.505F, 0F, 1F, packedLight, packedOverlay);
        vertex(consumer, pose, 0.5F, -0.5F, 0.505F, 1F, 1F, packedLight, packedOverlay);
        vertex(consumer, pose, 0.5F, 0.5F, 0.505F, 1F, 0F, packedLight, packedOverlay);
        vertex(consumer, pose, -0.5F, 0.5F, 0.505F, 0F, 0F, packedLight, packedOverlay);
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
            .setOverlay(packedOverlay)
            .setLight(packedLight)
            .setNormal(pose, 0F, 0F, 1F);
    }

    private static void orientToDriveFront(final BlockState state, final PoseStack poseStack) {
        final Direction facing = state.hasProperty(DiskDriveBlock.FACING) ? state.getValue(DiskDriveBlock.FACING) : Direction.NORTH;
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
