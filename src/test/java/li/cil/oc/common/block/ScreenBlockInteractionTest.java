package li.cil.oc.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class ScreenBlockInteractionTest {
    @Test
    void mapsNorthFaceHitToScreenCell() {
        BlockHitResult hit = new BlockHitResult(new Vec3(0.25D, 0.75D, 0.0D), Direction.NORTH, BlockPos.ZERO, false);

        ScreenHitMapper.ScreenClick click = ScreenHitMapper.screenCoordinates(Direction.NORTH, BlockPos.ZERO, hit, 40, 16);

        assertArrayEquals(new int[]{10, 4}, new int[]{click.x(), click.y()});
    }

    @Test
    void ignoresHitsOutsideScreenFace() {
        BlockHitResult hit = new BlockHitResult(new Vec3(0.25D, 0.75D, 1.0D), Direction.SOUTH, BlockPos.ZERO, false);

        assertNull(ScreenHitMapper.screenCoordinates(Direction.NORTH, BlockPos.ZERO, hit, 40, 16));
    }
}
