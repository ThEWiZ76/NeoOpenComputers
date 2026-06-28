package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.blockentity.GeolyzerBlockEntity;
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

public final class GeolyzerBlockEntityRenderer implements BlockEntityRenderer<GeolyzerBlockEntity> {
    private static final float MIN = -0.00125F;
    private static final float MAX = 1.00125F;
    private static final float TOP_Y = 1.00125F;
    private static final ResourceLocation TOP_ON_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "block/overlay/geolyzer_top_on");

    public GeolyzerBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        final GeolyzerBlockEntity geolyzer,
        final float partialTick,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight,
        final int packedOverlay) {
        renderTopOverlay(poseStack, bufferSource, packedLight);
    }

    private static void renderTopOverlay(final PoseStack poseStack, final MultiBufferSource bufferSource, final int packedLight) {
        final TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(TOP_ON_TEXTURE);
        final VertexConsumer consumer = sprite.wrap(bufferSource.getBuffer(RenderType.cutout()));
        final PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, MIN, TOP_Y, MAX, 0F, 1F, packedLight);
        vertex(consumer, pose, MAX, TOP_Y, MAX, 1F, 1F, packedLight);
        vertex(consumer, pose, MAX, TOP_Y, MIN, 1F, 0F, packedLight);
        vertex(consumer, pose, MIN, TOP_Y, MIN, 0F, 0F, packedLight);
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
            .setNormal(pose, Direction.UP.getStepX(), Direction.UP.getStepY(), Direction.UP.getStepZ());
    }
}
