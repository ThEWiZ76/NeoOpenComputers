package li.cil.oc.common;

import li.cil.oc.api.detail.NanomachinesAPI;
import li.cil.oc.api.API;
import li.cil.oc.api.nanomachines.BehaviorProvider;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.common.network.NanomachinePowerPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

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

    public static void registerTickHandler() {
        NeoForge.EVENT_BUS.addListener(NanomachinesRegistry::onPlayerTick);
    }

    private static void onPlayerTick(final PlayerTickEvent.Post event) {
        if (API.nanomachines instanceof NanomachinesRegistry registry) {
            registry.update(event.getEntity());
        }
    }

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

    public Controller installController(final Player player, final CompoundTag itemData) {
        final Controller controller = installController(player);
        if (controller instanceof SimpleNanomachineController simpleController) {
            if (li.cil.oc.common.item.NanomachineItemData.hasConfiguration(itemData)) {
                simpleController.loadItemConfiguration(itemData);
            }
        } else if (controller != null) {
            controller.reconfigure();
        }
        return controller;
    }

    public Controller debugController(final Player player) {
        final Controller controller = installController(player);
        if (controller instanceof SimpleNanomachineController simpleController) {
            simpleController.debugConfiguration();
        }
        return controller;
    }

    public List<String> controllerConfigurationLines(final Player player) {
        final Controller controller = installController(player);
        if (controller instanceof SimpleNanomachineController simpleController) {
            return simpleController.configurationLines();
        }
        return List.of();
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
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new NanomachinePowerPayload(false, 0D, 0D, 0, 0));
        }
        player.getPersistentData().remove(TAG_HAS_NANOMACHINES);
        player.getPersistentData().remove(TAG_CONTROLLER);
    }

    public void update(final Player player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        final Controller controller = getController(player);
        if (controller instanceof SimpleNanomachineController simpleController) {
            final boolean powerChanged = simpleController.update();
            final boolean stateDirty = simpleController.consumeClientStateDirty();
            if (player instanceof ServerPlayer serverPlayer
                && shouldSendPowerUpdate(player.tickCount, powerChanged, stateDirty, ModSettings.mfuTickFrequency())) {
                PacketDistributor.sendToPlayer(serverPlayer, new NanomachinePowerPayload(
                    true,
                    simpleController.getLocalBuffer(),
                    simpleController.getLocalBufferSize(),
                    activeInputCount(simpleController),
                    simpleController.getTotalInputCount(),
                    simpleController.activeParticleEffects()));
            }
        }
    }

    static boolean shouldSendPowerUpdate(final int tickCount, final boolean powerChanged, final boolean stateDirty, final int tickFrequency) {
        if (powerChanged || stateDirty) {
            return true;
        }
        final int interval = Math.max(1, tickFrequency);
        return tickCount % interval == 0;
    }

    private static int activeInputCount(final Controller controller) {
        int activeInputs = 0;
        for (int i = 0; i < controller.getTotalInputCount(); i++) {
            if (controller.getInput(i)) {
                activeInputs++;
            }
        }
        return activeInputs;
    }
}
