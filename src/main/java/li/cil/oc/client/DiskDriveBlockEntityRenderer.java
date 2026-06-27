package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc.common.block.DiskDriveBlock;
import li.cil.oc.common.blockentity.DiskDriveBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class DiskDriveBlockEntityRenderer implements BlockEntityRenderer<DiskDriveBlockEntity> {
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
        if (stack.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        orientToDriveFront(diskDrive.getBlockState(), poseStack);
        poseStack.translate(0D, 3.5D / 16D, -0.505D);
        poseStack.mulPose(Axis.XP.rotationDegrees(90F));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, diskDrive.getLevel(), 0);
        poseStack.popPose();
    }

    private static void orientToDriveFront(final BlockState state, final PoseStack poseStack) {
        final Direction facing = state.hasProperty(DiskDriveBlock.FACING) ? state.getValue(DiskDriveBlock.FACING) : Direction.NORTH;
        poseStack.translate(0.5D, 0.5D, 0.5D);
        switch (facing) {
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(-90F));
            default -> {
            }
        }
    }
}
