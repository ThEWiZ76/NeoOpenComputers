package li.cil.oc.common.block;

import com.mojang.serialization.MapCodec;
import li.cil.oc.common.blockentity.KeyboardBlockEntity;
import li.cil.oc.common.blockentity.ScreenBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class KeyboardBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<KeyboardBlock> CODEC = simpleCodec(KeyboardBlock::new);
    public static final DirectionProperty ATTACH_FACE = DirectionProperty.create("attach_face");

    private static final VoxelShape FLOOR_SHAPE = Block.box(1, 0, 1, 15, 1, 15);
    private static final VoxelShape CEILING_SHAPE = Block.box(1, 15, 1, 15, 16, 15);
    private static final VoxelShape NORTH_WALL_SHAPE = Block.box(1, 4, 0, 15, 12, 1);
    private static final VoxelShape SOUTH_WALL_SHAPE = Block.box(1, 4, 15, 15, 12, 16);
    private static final VoxelShape WEST_WALL_SHAPE = Block.box(0, 4, 1, 1, 12, 15);
    private static final VoxelShape EAST_WALL_SHAPE = Block.box(15, 4, 1, 16, 12, 15);

    public KeyboardBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(ATTACH_FACE, Direction.UP)
            .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new KeyboardBlockEntity(pos, state);
    }

    @Override
    protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        BlockNetworkConnector.joinIfServer(level, pos);
    }

    @Override
    protected void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block block, final BlockPos fromPos, final boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide && !canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
            return;
        }
        BlockNetworkConnector.joinIfServer(level, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(
        final BlockState state,
        final Level level,
        final BlockPos pos,
        final Player player,
        final BlockHitResult hitResult) {
        final ScreenBlockEntity screen = findAdjacentScreen(state, level, pos);
        if (screen == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        return ScreenBlock.openPhysicalTerminal(screen, player);
    }

    @Override
    protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        final Direction attachFace = state.getValue(ATTACH_FACE);
        final BlockPos supportPos = pos.relative(attachFace.getOpposite());
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, attachFace);
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return switch (state.getValue(ATTACH_FACE)) {
            case DOWN -> CEILING_SHAPE;
            case NORTH -> SOUTH_WALL_SHAPE;
            case SOUTH -> NORTH_WALL_SHAPE;
            case WEST -> EAST_WALL_SHAPE;
            case EAST -> WEST_WALL_SHAPE;
            case UP -> FLOOR_SHAPE;
        };
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final BlockState state = defaultBlockState()
            .setValue(ATTACH_FACE, context.getClickedFace())
            .setValue(FACING, context.getHorizontalDirection().getOpposite());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    private static ScreenBlockEntity findAdjacentScreen(final BlockState state, final Level level, final BlockPos pos) {
        final Direction attachFace = state.getValue(ATTACH_FACE);
        final ScreenBlockEntity attached = screenAt(level, pos.relative(attachFace.getOpposite()));
        if (attached != null) {
            return attached;
        }

        final Direction forward = attachFace.getAxis().isVertical() ? state.getValue(FACING) : Direction.UP;
        final ScreenBlockEntity inFront = screenAt(level, pos.relative(forward));
        if (inFront != null) {
            return inFront;
        }

        if (!attachFace.getAxis().isVertical()) {
            return screenAt(level, pos.relative(forward.getOpposite()));
        }
        return null;
    }

    private static ScreenBlockEntity screenAt(final Level level, final BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ScreenBlockEntity screen ? screen : null;
    }

    @Override
    protected BlockState rotate(final BlockState state, final Rotation rotation) {
        return state
            .setValue(ATTACH_FACE, rotation.rotate(state.getValue(ATTACH_FACE)))
            .setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(final BlockState state, final Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ATTACH_FACE, FACING);
    }
}
