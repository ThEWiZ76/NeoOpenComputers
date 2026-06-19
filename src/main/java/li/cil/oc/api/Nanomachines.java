package li.cil.oc.api;

import li.cil.oc.api.nanomachines.BehaviorProvider;
import li.cil.oc.api.nanomachines.Controller;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;

public final class Nanomachines {
    public static void addProvider(final BehaviorProvider provider) {
        if (API.nanomachines != null) {
            API.nanomachines.addProvider(provider);
        }
    }

    public static Iterable<BehaviorProvider> getProviders() {
        if (API.nanomachines != null) {
            return API.nanomachines.getProviders();
        }
        return Collections.emptyList();
    }

    public static boolean hasController(final Player player) {
        if (API.nanomachines != null) {
            return API.nanomachines.hasController(player);
        }
        return false;
    }

    public static Controller getController(final Player player) {
        if (API.nanomachines != null) {
            return API.nanomachines.getController(player);
        }
        return null;
    }

    public static Controller installController(final Player player) {
        if (API.nanomachines != null) {
            return API.nanomachines.installController(player);
        }
        return null;
    }

    public static void uninstallController(final Player player) {
        if (API.nanomachines != null) {
            API.nanomachines.uninstallController(player);
        }
    }

    private Nanomachines() {
    }
}
