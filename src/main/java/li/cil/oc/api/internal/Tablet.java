package li.cil.oc.api.internal;

import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.world.entity.player.Player;

public interface Tablet extends EnvironmentHost, MachineHost, Rotatable {
    Player player();
}
