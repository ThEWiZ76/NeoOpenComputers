package li.cil.oc.common.block;

import com.mojang.serialization.MapCodec;
import li.cil.oc.common.blockentity.RaidBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.phys.BlockHitResult;

@SuppressWarnings("deprecation")
public class RaidBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<RaidBlock> CODEC = simpleCodec(RaidBlock::new);

    public RaidBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new RaidBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(final BlockState state, final Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(final BlockState state, final Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }

    @Override
    protected boolean hasAnalogOutputSignal(final BlockState state) { return true; }

    @Override
    protected int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos) {
        return level.getBlockEntity(pos) instanceof RaidBlockEntity raid && raid.isComplete() ? 15 : 0;
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state, final LivingEntity placer, final ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RaidBlockEntity raid) {
            raid.loadFromStack(stack, level.registryAccess());
        }
    }

    @Override
    public ItemStack getCloneItemStack(final LevelReader level, final BlockPos pos, final BlockState state) {
        final ItemStack stack = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof RaidBlockEntity raid) {
            raid.saveToStack(stack, level.registryAccess());
        }
        return stack;
    }

    @Override
    protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        BlockNetworkConnector.joinIfServer(level, pos);
    }

    @Override
    protected void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block block, final BlockPos fromPos, final boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        BlockNetworkConnector.joinIfServer(level, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(
        final BlockState state,
        final Level level,
        final BlockPos pos,
        final Player player,
        final BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof RaidBlockEntity raid) {
            player.openMenu(raid);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState, final boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof RaidBlockEntity raid) {
            if (!level.isClientSide) {
                final ItemStack stack = new ItemStack(this);
                raid.saveToStack(stack, level.registryAccess());
                popResource(level, pos, stack);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
