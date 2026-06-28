package li.cil.oc.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class ScreenHitMapper {
    private static final double SCREEN_BORDER = 2.25D / 16D;
    private static final double CELL_WIDTH = 6D;
    private static final double LINE_HEIGHT = 9D;

    static ScreenClick screenCoordinates(final BlockState state, final BlockPos pos, final BlockHitResult hitResult, final int width, final int height) {
        return screenCoordinates(ScreenBlock.facing(state), ScreenBlock.up(state), ScreenBlock.localRight(state), pos, hitResult, width, height);
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
            ScreenBlock.localRight(state),
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

        return screenCoordinates(facing, up, cross(facing, up), pos, hitResult, width, height);
    }

    private static ScreenClick screenCoordinates(
        final Direction facing,
        final Direction up,
        final Direction right,
        final BlockPos pos,
        final BlockHitResult hitResult,
        final int width,
        final int height) {
        if (width <= 0 || height <= 0 || hitResult.getDirection() != facing) {
            return null;
        }

        return screenCoordinates(facing, up, right, pos, hitResult, width, height, 1, 1, 0, 0);
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

        return screenCoordinates(facing, up, cross(facing, up), pos, hitResult, width, height, blockWidth, blockHeight, localBlockX, localBlockY);
    }

    private static ScreenClick screenCoordinates(
        final Direction facing,
        final Direction up,
        final Direction right,
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
        final double horizontal = localCoordinate(relative, right);
        final double vertical = 1.0D - localCoordinate(relative, up);
        if (horizontal < 0.0D || horizontal >= 1.0D || vertical < 0.0D || vertical >= 1.0D) {
            return null;
        }

        final double absoluteX = localBlockX + horizontal;
        final double absoluteY = blockHeight - 1 - localBlockY + vertical;
        if (absoluteX <= SCREEN_BORDER
            || absoluteY <= SCREEN_BORDER
            || absoluteX >= blockWidth - SCREEN_BORDER
            || absoluteY >= blockHeight - SCREEN_BORDER) {
            return null;
        }

        final double innerWidth = blockWidth - SCREEN_BORDER * 2D;
        final double innerHeight = blockHeight - SCREEN_BORDER * 2D;
        final double scale = Math.min(innerWidth / (width * CELL_WIDTH), innerHeight / (height * LINE_HEIGHT));
        final double usedWidth = width * CELL_WIDTH * scale;
        final double usedHeight = height * LINE_HEIGHT * scale;
        final double usedLeft = SCREEN_BORDER + (innerWidth - usedWidth) * 0.5D;
        final double usedTop = SCREEN_BORDER + (innerHeight - usedHeight) * 0.5D;
        if (absoluteX < usedLeft
            || absoluteY < usedTop
            || absoluteX >= usedLeft + usedWidth
            || absoluteY >= usedTop + usedHeight) {
            return null;
        }

        final double screenX = (absoluteX - usedLeft) / usedWidth;
        final double screenY = (absoluteY - usedTop) / usedHeight;
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
