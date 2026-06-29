package li.cil.oc.common.driver;

import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.blockentity.ScreenBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public final class ScreenBlockDriver implements DriverBlock {
    @Override
    public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
        return world != null && pos != null && world.getBlockEntity(pos) instanceof ScreenBlockEntity screen && screen.canConnect(side);
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        if (worksWith(world, pos, side) && world.getBlockEntity(pos) instanceof ScreenBlockEntity screen) {
            return screen;
        }
        return null;
    }
}
