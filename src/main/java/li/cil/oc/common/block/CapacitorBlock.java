package li.cil.oc.common.block;

import com.mojang.serialization.MapCodec;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.blockentity.CapacitorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class CapacitorBlock extends Block implements EntityBlock {
    public static final MapCodec<CapacitorBlock> CODEC = simpleCodec(CapacitorBlock::new);
    public CapacitorBlock(BlockBehaviour.Properties properties) { super(properties); }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CapacitorBlockEntity(pos, state); }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide && type == ModBlockEntities.CAPACITOR.get()
            ? (world, pos, blockState, entity) -> ((CapacitorBlockEntity) entity).serverTick() : null;
    }

    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CapacitorBlockEntity capacitor ? capacitor.comparatorOutput() : 0;
    }

    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        update(level, pos);
    }

    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moved) {
        super.neighborChanged(state, level, pos, block, from, moved);
        update(level, pos);
    }

    private void update(Level level, BlockPos pos) {
        BlockNetworkConnector.joinIfServer(level, pos);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CapacitorBlockEntity capacitor) capacitor.refreshNearby();
    }
}
