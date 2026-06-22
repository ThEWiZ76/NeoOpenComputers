package li.cil.oc.common;

import li.cil.oc.api.detail.NanomachinesAPI;
import li.cil.oc.api.nanomachines.BehaviorProvider;
import li.cil.oc.api.nanomachines.Controller;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class NanomachinesRegistry implements NanomachinesAPI {
    private static final String TAG_HAS_NANOMACHINES = "oc:hasNanomachines";
    static final String TAG_CONTROLLER = "oc:nanomachines";

    private final Set<BehaviorProvider> providers = new LinkedHashSet<>();
    private final Map<Player, SimpleNanomachineController> controllers = new WeakHashMap<>();

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
        return player != null && player.getPersistentData().getBoolean(TAG_HAS_NANOMACHINES);
    }

    @Override
    public Controller getController(final Player player) {
        if (!hasController(player)) {
            return null;
        }
        return controllers.computeIfAbsent(player, ignored -> {
            final SimpleNanomachineController controller = new SimpleNanomachineController(player, this);
            final CompoundTag persistentData = player.getPersistentData();
            if (persistentData.contains(TAG_CONTROLLER)) {
                controller.load(persistentData.getCompound(TAG_CONTROLLER));
            }
            return controller;
        });
    }

    @Override
    public Controller installController(final Player player) {
        if (player == null) {
            return null;
        }
        player.getPersistentData().putBoolean(TAG_HAS_NANOMACHINES, true);
        return getController(player);
    }

    @Override
    public void uninstallController(final Player player) {
        if (player == null) {
            return;
        }
        final SimpleNanomachineController controller = controllers.remove(player);
        if (controller != null) {
            controller.dispose();
        }
        player.getPersistentData().remove(TAG_HAS_NANOMACHINES);
        player.getPersistentData().remove(TAG_CONTROLLER);
    }
}
