package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc.common.blockentity.PrinterBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class PrinterBlockEntityRenderer implements BlockEntityRenderer<PrinterBlockEntity> {
    private static final long ROTATION_PERIOD_MILLIS = 20_000L;

    private final ItemRenderer itemRenderer;

    public PrinterBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
        itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(
        final PrinterBlockEntity printer,
        final float partialTick,
        final PoseStack poseStack,
        final MultiBufferSource bufferSource,
        final int packedLight,
        final int packedOverlay) {
        final ItemStack stack = printer.previewStack();
        if (stack.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.8D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationDegrees()));
        poseStack.scale(0.75F, 0.75F, 0.75F);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, printer.getLevel(), 0);
        poseStack.popPose();
    }

    private static float rotationDegrees() {
        return (System.currentTimeMillis() % ROTATION_PERIOD_MILLIS) / (float) ROTATION_PERIOD_MILLIS * 360F;
    }
}
