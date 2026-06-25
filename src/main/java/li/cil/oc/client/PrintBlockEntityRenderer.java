package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.common.block.PrintBlock;
import li.cil.oc.common.blockentity.PrintBlockEntity;
import li.cil.oc.common.item.data.PrintRenderModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public final class PrintBlockEntityRenderer implements BlockEntityRenderer<PrintBlockEntity> {
    public PrintBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(final PrintBlockEntity print, final float partialTick, final PoseStack poseStack, final MultiBufferSource bufferSource, final int packedLight, final int packedOverlay) {
        final Direction facing = facing(print.getBlockState());
        PrintShapeRenderer.renderShapes(
            PrintRenderModel.blockShapes(print.data(), print.isActiveState(), facing),
            poseStack,
            bufferSource,
            packedLight,
            packedOverlay);
    }

    private static Direction facing(final BlockState state) {
        return state.hasProperty(PrintBlock.FACING) ? state.getValue(PrintBlock.FACING) : Direction.SOUTH;
    }
}
