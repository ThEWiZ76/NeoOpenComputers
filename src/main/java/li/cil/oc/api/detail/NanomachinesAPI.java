package li.cil.oc.api.detail;

import li.cil.oc.api.nanomachines.BehaviorProvider;
import li.cil.oc.api.nanomachines.Controller;
import net.minecraft.world.entity.player.Player;

public interface NanomachinesAPI {
    void addProvider(BehaviorProvider provider);

    Iterable<BehaviorProvider> getProviders();

    boolean hasController(Player player);

    Controller getController(Player player);

    Controller installController(Player player);

    void uninstallController(Player player);
}
