package li.cil.oc.api.driver;

import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public interface DriverBlock {
    boolean worksWith(Level world, BlockPos pos, Direction side);

    ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side);
}
