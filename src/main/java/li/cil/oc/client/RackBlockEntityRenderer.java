package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import li.cil.oc.api.event.RackMountableRenderEvent;
import li.cil.oc.common.block.RackBlock;
import li.cil.oc.common.blockentity.RackBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;

public final class RackBlockEntityRenderer implements BlockEntityRenderer<RackBlockEntity> {
    private static final float SLOT_V_OFFSET = 2F / 16F;
    private static final float SLOT_V_SIZE = 3F / 16F;

    public RackBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        final RackBlockEntity rack,
        final float partialTick,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight,
        final int packedOverlay) {
        poseStack.pushPose();
        orientToRackFront(rack.getBlockState(), poseStack);
        for (int slot = 0; slot < rack.getContainerSize(); slot++) {
            final ItemStack stack = rack.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            final CompoundTag data = rack.getMountableData(slot);
            final float v0 = slotV0(slot);
            final float v1 = slotV1(slot);
            final RackMountableRenderEvent.Block blockEvent =
                new RackMountableRenderEvent.Block(rack, slot, data, rack.facing());
            NeoForge.EVENT_BUS.post(blockEvent);
            if (blockEvent.getFrontTextureOverride() != null) {
                renderSlotTexture(blockEvent.getFrontTextureOverride(), poseStack, bufferSource, slot, packedLight);
            }
            final RackMountableRenderEvent.TileEntity tileEntityEvent =
                new RackMountableRenderEvent.TileEntity(rack, slot, data, v0, v1, poseStack, bufferSource, packedLight, packedOverlay);
            NeoForge.EVENT_BUS.post(tileEntityEvent);
        }
        poseStack.popPose();
    }

    static float slotV0(final int slot) {
        return SLOT_V_OFFSET + slot * SLOT_V_SIZE;
    }

    static float slotV1(final int slot) {
        return SLOT_V_OFFSET + (slot + 1) * SLOT_V_SIZE;
    }

    private static void renderSlotTexture(
        final TextureAtlasSprite sprite,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int slot,
        final int packedLight) {
        final VertexConsumer consumer = sprite.wrap(bufferSource.getBuffer(RenderType.cutout()));
        final PoseStack.Pose pose = poseStack.last();
        final float v0 = slotV0(slot);
        final float v1 = slotV1(slot);
        final float top = 0.5F - v0;
        final float bottom = 0.5F - v1;
        vertex(consumer, pose, -0.5F, bottom, 0.505F, 0F, v1, packedLight);
        vertex(consumer, pose, 0.5F, bottom, 0.505F, 1F, v1, packedLight);
        vertex(consumer, pose, 0.5F, top, 0.505F, 1F, v0, packedLight);
        vertex(consumer, pose, -0.5F, top, 0.505F, 0F, v0, packedLight);
    }

    private static void vertex(
        final VertexConsumer consumer,
        final PoseStack.Pose pose,
        final float x,
        final float y,
        final float z,
        final float u,
        final float v,
        final int packedLight) {
        consumer.addVertex(pose, x, y, z)
            .setColor(0xFFFFFFFF)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(packedLight)
            .setNormal(pose, 0F, 0F, 1F);
    }

    private static void orientToRackFront(final BlockState state, final PoseStack poseStack) {
        final Direction facing = state.hasProperty(RackBlock.FACING) ? state.getValue(RackBlock.FACING) : Direction.NORTH;
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
