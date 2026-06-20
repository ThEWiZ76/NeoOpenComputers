package li.cil.oc.common.block;

import li.cil.oc.api.Network;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

final class BlockNetworkConnector {
    static void joinIfServer(final Level level, final BlockPos pos) {
        if (level != null && !level.isClientSide) {
            Network.joinOrCreateNetwork(level, pos);
        }
    }

    private BlockNetworkConnector() {
    }
}
