package li.cil.oc.api.prefab;

import li.cil.oc.api.driver.DriverBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class DriverSidedTileEntity implements DriverBlock {
    public abstract Class<?> getTileEntityClass();

    @Override
    public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
        final Class<?> blockEntityClass = getTileEntityClass();
        if (blockEntityClass == null || world == null || pos == null) {
            return false;
        }

        final BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity != null && blockEntityClass.isAssignableFrom(blockEntity.getClass());
    }
}
