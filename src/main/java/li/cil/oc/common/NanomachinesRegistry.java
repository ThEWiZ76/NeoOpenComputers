package li.cil.oc.common;

import li.cil.oc.api.detail.NanomachinesAPI;
import li.cil.oc.api.nanomachines.BehaviorProvider;
import li.cil.oc.api.nanomachines.Controller;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public final class NanomachinesRegistry implements NanomachinesAPI {
    private final List<BehaviorProvider> providers = new ArrayList<>();

    @Override
    public void addProvider(final BehaviorProvider provider) {
        providers.add(provider);
    }

    @Override
    public Iterable<BehaviorProvider> getProviders() {
        return List.copyOf(providers);
    }

    @Override
    public boolean hasController(final Player player) {
        return false;
    }

    @Override
    public Controller getController(final Player player) {
        return null;
    }

    @Override
    public Controller installController(final Player player) {
        return null;
    }

    @Override
    public void uninstallController(final Player player) {
    }
}
