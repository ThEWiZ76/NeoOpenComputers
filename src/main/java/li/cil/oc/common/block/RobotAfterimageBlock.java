package li.cil.oc.common.block;

import li.cil.oc.common.ModSettings;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Temporary interaction surface at the previous position of a moving robot. */
public final class RobotAfterimageBlock extends Block {
    public RobotAfterimageBlock(final Properties properties) {
        super(properties);
    }

    public static RobotBlockEntity findRobot(final BlockGetter level, final BlockPos pos) {
        for (final Direction side : Direction.values()) {
            final BlockPos target = pos.relative(side);
            if (level instanceof Level world && !world.hasChunkAt(target)) continue;
            if (level.getBlockEntity(target) instanceof RobotBlockEntity robot && robot.movedFrom(pos)) return robot;
        }
        return null;
    }

    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        final RobotBlockEntity robot = findRobot(level, pos);
        if (robot == null) return Shapes.empty();
        final BlockPos target = robot.getBlockPos();
        return robot.getBlockState().getShape(level, target, context)
            .move(target.getX() - pos.getX(), target.getY() - pos.getY(), target.getZ() - pos.getZ());
    }

    @Override
    public ItemStack getCloneItemStack(final LevelReader level, final BlockPos pos, final BlockState state) {
        final RobotBlockEntity robot = findRobot(level, pos);
        return robot == null ? ItemStack.EMPTY : robot.getBlockState().getBlock().getCloneItemStack(level, robot.getBlockPos(), robot.getBlockState());
    }

    @Override
    protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean moving) {
        if (!level.isClientSide) level.scheduleTick(pos, this, Math.max(1, (int) (ModSettings.robotMoveDelay() * 20D)) - 1);
    }

    @Override
    protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hit) {
        final RobotBlockEntity robot = findRobot(level, pos);
        if (robot != null) {
            return robot.getBlockState().useWithoutItem(level, player, hit.withPosition(robot.getBlockPos()));
        }
        if (!level.isClientSide) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        return InteractionResult.PASS;
    }

    @Override
    public boolean onDestroyedByPlayer(final BlockState state, final Level level, final BlockPos pos, final Player player, final boolean harvest, final FluidState fluid) {
        final RobotBlockEntity robot = findRobot(level, pos);
        if (robot != null && player instanceof ServerPlayer serverPlayer) {
            // Use the target's normal break path, including permissions, events and drops.
            if (!serverPlayer.gameMode.destroyBlock(robot.getBlockPos()) || level.getBlockEntity(robot.getBlockPos()) == robot) return false;
        }
        return level.removeBlock(pos, false);
    }
}
