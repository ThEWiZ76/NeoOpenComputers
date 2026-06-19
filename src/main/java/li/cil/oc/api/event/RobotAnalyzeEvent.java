package li.cil.oc.api.event;

import li.cil.oc.api.internal.Agent;
import net.minecraft.world.entity.player.Player;

public class RobotAnalyzeEvent extends RobotEvent {
    public final Player player;

    public RobotAnalyzeEvent(final Agent agent, final Player player) {
        super(agent);
        this.player = player;
    }
}
