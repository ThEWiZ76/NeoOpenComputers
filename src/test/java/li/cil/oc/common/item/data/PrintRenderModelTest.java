package li.cil.oc.common.item.data;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class PrintRenderModelTest {
    @Test
    void blockShapesUseActiveStateTexturesAndTintLikeUpstream() {
        final PrintData data = new PrintData();
        data.addStateOff(new PrintData.Shape(new AABB(0D, 0D, 0D, 1D, 0.25D, 1D), "minecraft:block/stone", 0x112233));
        data.addStateOn(new PrintData.Shape(new AABB(0D, 0D, 0D, 0.25D, 1D, 1D), "minecraft:block/redstone_block", 0x445566));
        data.addStateOn(new PrintData.Shape(new AABB(0.75D, 0D, 0D, 1D, 1D, 1D), "", 0x778899));

        final List<PrintRenderModel.RenderShape> shapes = PrintRenderModel.blockShapes(data, true, Direction.EAST);

        assertEquals(1, shapes.size());
        assertEquals("minecraft:block/redstone_block", shapes.getFirst().texture());
        assertEquals(0x445566, shapes.getFirst().tint());
        assertEquals(new AABB(0D, 0D, 0.75D, 1D, 1D, 1D), shapes.getFirst().bounds());
    }

    @Test
    void itemShapesUseOffStateAndFallbackWhenEmptyLikeUpstream() {
        final PrintData empty = new PrintData();
        final List<PrintRenderModel.RenderShape> fallback = PrintRenderModel.itemShapes(empty, false);

        assertEquals(1, fallback.size());
        assertEquals(PrintRenderModel.FALLBACK_TEXTURE, fallback.getFirst().texture());
        assertEquals(PrintRenderModel.FALLBACK_TINT, fallback.getFirst().tint());
        assertEquals(PrintRenderModel.UNIT_BOUNDS, fallback.getFirst().bounds());
    }
}
