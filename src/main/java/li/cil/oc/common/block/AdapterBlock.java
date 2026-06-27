package li.cil.oc.common.block;

import com.mojang.serialization.MapCodec;
import li.cil.oc.common.WrenchTools;
import li.cil.oc.common.blockentity.AdapterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

@SuppressWarnings("deprecation")
public class AdapterBlock extends Block implements EntityBlock {
    public static final MapCodec<AdapterBlock> CODEC = simpleCodec(AdapterBlock::new);

    public AdapterBlock(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new AdapterBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        if (level.isClientSide || type != li.cil.oc.common.ModBlockEntities.ADAPTER.get()) {
            return null;
        }
        return (tickerLevel, pos, blockState, blockEntity) ->
            AdapterBlockEntity.serverTick(tickerLevel, pos, blockState, (AdapterBlockEntity) blockEntity);
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
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AdapterBlockEntity adapter) {
            adapter.refreshNeighbor(fromPos);
        }
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
        if (!WrenchTools.isWrench(stack) || !(level.getBlockEntity(pos) instanceof AdapterBlockEntity adapter)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            final Direction side = player != null && player.isShiftKeyDown()
                ? hitResult.getDirection().getOpposite()
                : hitResult.getDirection();
            adapter.setSideOpen(side, !adapter.isSideOpen(side));
            WrenchTools.wrenchUsed(player, pos);
            if (player != null) {
                player.swing(hand);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
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
        if (level.getBlockEntity(pos) instanceof AdapterBlockEntity adapter) {
            player.openMenu(adapter);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState, final boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AdapterBlockEntity adapter) {
            adapter.removeNode();
        }
        Containers.dropContentsOnDestroy(state, newState, level, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
