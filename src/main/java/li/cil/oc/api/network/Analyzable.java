package li.cil.oc.api.network;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

public interface Analyzable {
    Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ);
}
