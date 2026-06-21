package li.cil.oc.common.block;

import com.mojang.serialization.MapCodec;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.blockentity.RelayBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

@SuppressWarnings("deprecation")
public class RelayBlock extends Block implements EntityBlock {
    public static final MapCodec<RelayBlock> CODEC = simpleCodec(RelayBlock::new);

    public RelayBlock(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new RelayBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.RELAY.get()) {
            return null;
        }
        return (tickerLevel, blockPos, blockState, blockEntity) ->
            RelayBlockEntity.serverTick(tickerLevel, blockPos, blockState, (RelayBlockEntity) blockEntity);
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
        if (level.getBlockEntity(pos) instanceof RelayBlockEntity relay) {
            player.openMenu(relay);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState, final boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof RelayBlockEntity relay) {
            if (!level.isClientSide) {
                for (int slot = 0; slot < relay.getContainerSize(); slot++) {
                    final ItemStack stack = relay.getItem(slot);
                    if (!stack.isEmpty()) {
                        popResource(level, pos, stack.copy());
                    }
                }
            }
            relay.removeNodes();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
