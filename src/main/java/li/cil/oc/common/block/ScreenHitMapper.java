package li.cil.oc.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class ScreenHitMapper {
    static ScreenClick screenCoordinates(final BlockState state, final BlockPos pos, final BlockHitResult hitResult, final int width, final int height) {
        return screenCoordinates(ScreenBlock.facing(state), ScreenBlock.up(state), pos, hitResult, width, height);
    }

    static ScreenClick screenCoordinates(
        final BlockState state,
        final BlockPos pos,
        final BlockHitResult hitResult,
        final int width,
        final int height,
        final int blockWidth,
        final int blockHeight,
        final int localBlockX,
        final int localBlockY) {
        return screenCoordinates(
            ScreenBlock.facing(state),
            ScreenBlock.up(state),
            pos,
            hitResult,
            width,
            height,
            blockWidth,
            blockHeight,
            localBlockX,
            localBlockY);
    }

    static ScreenClick screenCoordinates(final Direction facing, final BlockPos pos, final BlockHitResult hitResult, final int width, final int height) {
        return screenCoordinates(facing, Direction.UP, pos, hitResult, width, height);
    }

    static ScreenClick screenCoordinates(final Direction facing, final Direction up, final BlockPos pos, final BlockHitResult hitResult, final int width, final int height) {
        if (width <= 0 || height <= 0 || hitResult.getDirection() != facing) {
            return null;
        }

        final Vec3 relative = hitResult.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        final Direction right = cross(facing, up);
        final double horizontal = localCoordinate(relative, right);
        final double vertical = 1.0D - localCoordinate(relative, up);
        if (horizontal < 0.0D || horizontal >= 1.0D || vertical < 0.0D || vertical >= 1.0D) {
            return null;
        }

        return new ScreenClick(
            Math.min(width - 1, (int) Math.floor(horizontal * width)),
            Math.min(height - 1, (int) Math.floor(vertical * height))
        );
    }

    static ScreenClick screenCoordinates(
        final Direction facing,
        final Direction up,
        final BlockPos pos,
        final BlockHitResult hitResult,
        final int width,
        final int height,
        final int blockWidth,
        final int blockHeight,
        final int localBlockX,
        final int localBlockY) {
        if (width <= 0 || height <= 0 || blockWidth <= 0 || blockHeight <= 0 || hitResult.getDirection() != facing) {
            return null;
        }

        final Vec3 relative = hitResult.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        final Direction right = cross(facing, up);
        final double horizontal = localCoordinate(relative, right);
        final double vertical = 1.0D - localCoordinate(relative, up);
        if (horizontal < 0.0D || horizontal >= 1.0D || vertical < 0.0D || vertical >= 1.0D) {
            return null;
        }

        final double screenX = (localBlockX + horizontal) / blockWidth;
        final double screenY = (blockHeight - 1 - localBlockY + vertical) / blockHeight;
        if (screenX < 0.0D || screenX >= 1.0D || screenY < 0.0D || screenY >= 1.0D) {
            return null;
        }

        return new ScreenClick(
            Math.min(width - 1, (int) Math.floor(screenX * width)),
            Math.min(height - 1, (int) Math.floor(screenY * height))
        );
    }

    record ScreenClick(int x, int y) {
    }

    private static double localCoordinate(final Vec3 relative, final Direction direction) {
        final double dot = direction.getStepX() * relative.x + direction.getStepY() * relative.y + direction.getStepZ() * relative.z;
        return dot < 0.0D ? 1.0D + dot : dot;
    }

    private static Direction cross(final Direction a, final Direction b) {
        final int x = a.getStepY() * b.getStepZ() - a.getStepZ() * b.getStepY();
        final int y = a.getStepZ() * b.getStepX() - a.getStepX() * b.getStepZ();
        final int z = a.getStepX() * b.getStepY() - a.getStepY() * b.getStepX();
        return Direction.fromDelta(x, y, z);
    }

    private ScreenHitMapper() {
    }
}
