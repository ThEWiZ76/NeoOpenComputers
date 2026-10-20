package li.cil.oc.common.inventory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class BlockInventoryAccess {
    private BlockInventoryAccess() { }

    public static boolean stillValid(final BlockEntity host, final Player player) {
        final var level = host.getLevel();
        final var pos = host.getBlockPos();
        return player != null && level != null && player.level() == level && !host.isRemoved()
            && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64D
            && level.hasChunkAt(pos) && level.getBlockEntity(pos) == host;
    }
}
