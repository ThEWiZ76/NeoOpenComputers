package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.entity.DroneEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class DroneEntityRenderer extends EntityRenderer<DroneEntity> {
    private final DroneModel model;

    public DroneEntityRenderer(final EntityRendererProvider.Context context) {
        super(context);
        model = new DroneModel(context.bakeLayer(DroneModel.LAYER_LOCATION));
        shadowRadius = 0.35F;
    }

    @Override
    public void render(
        final DroneEntity entity,
        final float entityYaw,
        final float partialTick,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight
    ) {
        poseStack.pushPose();
        poseStack.translate(0D, 0.125D + hoverOffset(entity, partialTick), 0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot())));
        poseStack.scale(1F, -1F, -1F);
        model.setupAnim(entity, 0F, 0F, entity.tickCount + partialTick, entityYaw, 0F);
        final VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutout(DroneModel.TEXTURE));
        model.renderToBuffer(poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static double hoverOffset(final DroneEntity entity, final float partialTick) {
        if (!entity.machine().isRunning()) {
            return 0D;
        }
        return Math.sin((entity.tickCount + partialTick + (entity.getId() ^ 0xFF)) / 20D) / 16D;
    }

    @Override
    public ResourceLocation getTextureLocation(final DroneEntity entity) {
        return DroneModel.TEXTURE;
    }
}
