package li.cil.oc.common.block;

import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ComputerCaseBlock extends Block implements EntityBlock {
    public ComputerCaseBlock(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new ComputerCaseBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.COMPUTER_CASE.get()) {
            return null;
        }
        return (tickerLevel, pos, blockState, blockEntity) ->
            ComputerCaseBlockEntity.serverTick(tickerLevel, pos, blockState, (ComputerCaseBlockEntity) blockEntity);
    }
}
