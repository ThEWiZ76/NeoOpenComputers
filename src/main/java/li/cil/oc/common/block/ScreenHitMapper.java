package li.cil.oc.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class ScreenHitMapper {
    static ScreenClick screenCoordinates(final Direction facing, final BlockPos pos, final BlockHitResult hitResult, final int width, final int height) {
        if (width <= 0 || height <= 0 || hitResult.getDirection() != facing) {
            return null;
        }

        final Vec3 relative = hitResult.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        final double horizontal = switch (facing) {
            case NORTH -> relative.x;
            case SOUTH -> 1.0D - relative.x;
            case WEST -> 1.0D - relative.z;
            case EAST -> relative.z;
            default -> relative.x;
        };
        final double vertical = 1.0D - relative.y;
        if (horizontal < 0.0D || horizontal >= 1.0D || vertical < 0.0D || vertical >= 1.0D) {
            return null;
        }

        return new ScreenClick(
            Math.min(width - 1, (int) Math.floor(horizontal * width)),
            Math.min(height - 1, (int) Math.floor(vertical * height))
        );
    }

    record ScreenClick(int x, int y) {
    }

    private ScreenHitMapper() {
    }
}
