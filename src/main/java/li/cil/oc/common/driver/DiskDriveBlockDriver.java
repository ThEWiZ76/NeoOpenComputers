package li.cil.oc.common.driver;

import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.blockentity.DiskDriveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public final class DiskDriveBlockDriver implements DriverBlock {
    @Override
    public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
        return world != null && pos != null && world.getBlockEntity(pos) instanceof DiskDriveBlockEntity;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        if (world != null && pos != null && world.getBlockEntity(pos) instanceof DiskDriveBlockEntity diskDrive) {
            return diskDrive;
        }
        return null;
    }
}
