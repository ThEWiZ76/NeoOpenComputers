package li.cil.oc.api.internal;

import li.cil.oc.api.Persistable;
import li.cil.oc.api.network.Environment;
import net.minecraft.world.entity.player.Player;

public interface Keyboard extends Environment, Persistable {
    void setUsableOverride(UsabilityChecker callback);

    interface UsabilityChecker {
        boolean isUsableByPlayer(Keyboard keyboard, Player player);
    }
}
