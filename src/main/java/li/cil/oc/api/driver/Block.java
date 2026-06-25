package li.cil.oc.api.driver;

import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * @deprecated Use {@link SidedBlock} instead, ignoring the side argument if the side does not matter.
 */
@Deprecated
public interface Block extends DriverBlock {
    boolean worksWith(Level world, BlockPos pos);

    ManagedEnvironment createEnvironment(Level world, BlockPos pos);

    @Override
    default boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
        return worksWith(world, pos);
    }

    @Override
    default ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        return createEnvironment(world, pos);
    }
}
