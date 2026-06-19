package li.cil.oc.api.internal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface Wrench {
    boolean useWrenchOnBlock(Player player, Level world, BlockPos pos, boolean simulate);
}
