package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.blockentity.RaidBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

public final class RaidBlockEntityRenderer implements BlockEntityRenderer<RaidBlockEntity> {
    private static final float FRONT_Z = 1.00125F;
    private static final float U1 = 2F / 16F;
    private static final float SLOT_SIZE = 4F / 16F;
    private static final ResourceLocation ERROR_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "block/overlay/raid_front_error");
    private static final ResourceLocation ACTIVITY_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "block/overlay/raid_front_activity");

    public RaidBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        final RaidBlockEntity raid,
        final float partialTick,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight,
        final int packedOverlay) {
        final int presenceMask = raid.visualPresenceMask();
        final PoseStack.Pose pose = poseStack.last();
        final VertexConsumer errorConsumer = consumer(bufferSource, ERROR_TEXTURE);
        for (int slot = 0; slot < RaidBlockEntity.CONTAINER_SIZE; slot++) {
            if ((presenceMask & (1 << slot)) == 0) {
                renderSlotOverlay(errorConsumer, pose, slot, packedLight, packedOverlay);
            }
        }

        final int activeSlot = raid.visualActiveSlot();
        if (activeSlot >= 0 && activeSlot < RaidBlockEntity.CONTAINER_SIZE) {
            renderSlotOverlay(consumer(bufferSource, ACTIVITY_TEXTURE), pose, activeSlot, packedLight, packedOverlay);
        }
    }

    private static VertexConsumer consumer(final MultiBufferSource bufferSource, final ResourceLocation texture) {
        final TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
        return sprite.wrap(bufferSource.getBuffer(RenderType.cutout()));
    }

    private static void renderSlotOverlay(
        final VertexConsumer consumer,
        final PoseStack.Pose pose,
        final int slot,
        final int packedLight,
        final int packedOverlay) {
        final float left = U1 + slot * SLOT_SIZE;
        final float right = U1 + (slot + 1) * SLOT_SIZE;
        vertex(consumer, pose, left, 1F, FRONT_Z, left, 0F, packedLight, packedOverlay);
        vertex(consumer, pose, right, 1F, FRONT_Z, right, 0F, packedLight, packedOverlay);
        vertex(consumer, pose, right, 0F, FRONT_Z, right, 1F, packedLight, packedOverlay);
        vertex(consumer, pose, left, 0F, FRONT_Z, left, 1F, packedLight, packedOverlay);
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
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(packedLight)
            .setNormal(pose, 0F, 0F, 1F);
    }
}
