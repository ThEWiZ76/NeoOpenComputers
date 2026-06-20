package li.cil.oc.common.blockentity;

import li.cil.oc.common.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DiskDriveBlockEntity extends BlockEntity {
    public DiskDriveBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.DISK_DRIVE.get(), pos, blockState);
    }
}
