package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.common.item.data.PrintData;
import li.cil.oc.common.item.data.PrintRenderModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class PrintItemRenderer extends BlockEntityWithoutLevelRenderer {
    public PrintItemRenderer(final BlockEntityRenderDispatcher blockEntityRenderDispatcher, final EntityModelSet entityModelSet) {
        super(blockEntityRenderDispatcher, entityModelSet);
    }

    @Override
    public void renderByItem(
        final ItemStack stack,
        final ItemDisplayContext displayContext,
        final PoseStack poseStack,
        final MultiBufferSource buffer,
        final int packedLight,
        final int packedOverlay) {
        final boolean activePreview = Minecraft.getInstance().options.advancedItemTooltips;
        PrintShapeRenderer.renderShapes(
            PrintRenderModel.itemShapes(new PrintData(stack), activePreview),
            poseStack,
            buffer,
            packedLight,
            packedOverlay);
    }
}
