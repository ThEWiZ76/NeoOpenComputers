package li.cil.oc.common.block;

import com.mojang.serialization.MapCodec;
import li.cil.oc.common.blockentity.DiskDriveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

@SuppressWarnings("deprecation")
public class DiskDriveBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<DiskDriveBlock> CODEC = simpleCodec(DiskDriveBlock::new);
    public static final BooleanProperty HAS_MEDIA = BooleanProperty.create("has_media");

    public DiskDriveBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(HAS_MEDIA, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new DiskDriveBlockEntity(pos, state);
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
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState, final boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof DiskDriveBlockEntity diskDrive) {
            diskDrive.save(new CompoundTag());
        }
        Containers.dropContentsOnDestroy(state, newState, level, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
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
        if (level.getBlockEntity(pos) instanceof DiskDriveBlockEntity diskDrive) {
            if (player.isShiftKeyDown()) {
                return removeDisk(diskDrive, player);
            }
            player.openMenu(diskDrive);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected ItemInteractionResult useItemOn(
        final ItemStack stack,
        final BlockState state,
        final Level level,
        final BlockPos pos,
        final Player player,
        final InteractionHand hand,
        final BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof DiskDriveBlockEntity diskDrive &&
            diskDrive.isEmpty() &&
            diskDrive.canPlaceItem(DiskDriveBlockEntity.SLOT_FLOPPY, stack)) {
            if (level.isClientSide) {
                return ItemInteractionResult.SUCCESS;
            }
            diskDrive.setItem(DiskDriveBlockEntity.SLOT_FLOPPY, stack.split(1));
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static InteractionResult removeDisk(final DiskDriveBlockEntity diskDrive, final Player player) {
        final ItemStack removed = diskDrive.removeItemNoUpdate(DiskDriveBlockEntity.SLOT_FLOPPY);
        if (removed.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!player.addItem(removed)) {
            player.drop(removed, false);
        }
        return InteractionResult.CONSUME;
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
        builder.add(FACING, HAS_MEDIA);
    }
}
