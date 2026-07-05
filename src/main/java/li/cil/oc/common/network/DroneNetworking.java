package li.cil.oc.common.network;

import li.cil.oc.common.entity.DroneEntity;
import li.cil.oc.common.menu.DroneMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class DroneNetworking {
    public static final String NETWORK_VERSION = "1";

    public static void register(final RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION)
            .playToServer(
                DroneControlPayload.TYPE,
                DroneControlPayload.STREAM_CODEC,
                DroneNetworking::handleDroneControl);
    }

    static boolean applyDroneControl(final AbstractContainerMenu containerMenu, final DroneControlPayload payload) {
        if (!(containerMenu instanceof DroneMenu menu) || menu.containerId != payload.containerId()) {
            return false;
        }
        if (!(menu.droneInventory() instanceof DroneEntity drone)) {
            return false;
        }
        final boolean running = drone.machine().isRunning() || drone.machine().isPaused();
        if (payload.action() == RackControlPayload.START) {
            return running || drone.toggleMachine();
        }
        if (payload.action() == RackControlPayload.STOP) {
            return !running || drone.toggleMachine();
        }
        return payload.action() == RackControlPayload.TOGGLE && drone.toggleMachine();
    }

    static boolean applyDroneControl(final Player player, final AbstractContainerMenu containerMenu, final DroneControlPayload payload) {
        if (!(containerMenu instanceof DroneMenu menu) || menu.containerId != payload.containerId()) {
            return false;
        }
        if (player == null || !menu.stillValid(player)) {
            return false;
        }
        return applyDroneControl(containerMenu, payload);
    }

    private static void handleDroneControl(final DroneControlPayload payload, final IPayloadContext context) {
        applyDroneControl(context.player(), context.player().containerMenu, payload);
    }

    private DroneNetworking() {
    }
}
