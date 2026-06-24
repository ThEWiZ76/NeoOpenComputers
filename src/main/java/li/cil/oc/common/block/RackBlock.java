package li.cil.oc.common.block;

import com.mojang.serialization.MapCodec;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.blockentity.RackBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("deprecation")
public class RackBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<RackBlock> CODEC = simpleCodec(RackBlock::new);

    public RackBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new RackBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state, final LivingEntity placer, final ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RackBlockEntity rack) {
            rack.loadFromStack(stack, level.registryAccess());
        }
    }

    @Override
    public ItemStack getCloneItemStack(final LevelReader level, final BlockPos pos, final BlockState state) {
        final ItemStack stack = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof RackBlockEntity rack) {
            rack.saveToStack(stack, level.registryAccess());
        }
        return stack;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.RACK.get()) {
            return null;
        }
        return (tickerLevel, pos, blockState, blockEntity) ->
            RackBlockEntity.serverTick(tickerLevel, pos, blockState, (RackBlockEntity) blockEntity);
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
        if (level.getBlockEntity(pos) instanceof RackBlockEntity rack) {
            final Vec3 clickLocation = hitResult.getLocation();
            final float hitX = (float) (clickLocation.x - pos.getX());
            final float hitY = (float) (clickLocation.y - pos.getY());
            final float hitZ = (float) (clickLocation.z - pos.getZ());
            final Integer slot = rack.slotAt(hitResult.getDirection(), hitX, hitY, hitZ);
            if (slot != null) {
                final RackMountable mountable = rack.getMountable(slot);
                if (mountable != null && mountable.onActivate(
                    player,
                    InteractionHand.MAIN_HAND,
                    player.getMainHandItem(),
                    localMountableX(hitResult.getDirection(), hitX),
                    localMountableY(hitY, slot))) {
                    return InteractionResult.CONSUME;
                }
            }
            player.openMenu(rack);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    private static float localMountableX(final Direction side, final float hitX) {
        final int globalX = (int) (hitX * 16.05F);
        final int localX = (side.getAxis() != Axis.Z ? 15 - globalX : globalX) - 1;
        return localX / 14F;
    }

    private static float localMountableY(final float hitY, final int slot) {
        final int globalY = (int) (hitY * 16.05F);
        return ((15 - globalY) - 2 - 3 * slot) / 3F;
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState, final boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof RackBlockEntity rack && !level.isClientSide) {
            for (final ItemStack stack : rack.stacksForDrop()) {
                popResource(level, pos, stack);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
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
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
