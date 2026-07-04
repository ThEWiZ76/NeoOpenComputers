package li.cil.oc.common.network;

import li.cil.oc.common.blockentity.MicrocontrollerBlockEntity;
import li.cil.oc.common.menu.MicrocontrollerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class MicrocontrollerNetworking {
    public static final String NETWORK_VERSION = "1";

    public static void register(final RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION)
            .playToServer(
                MicrocontrollerControlPayload.TYPE,
                MicrocontrollerControlPayload.STREAM_CODEC,
                MicrocontrollerNetworking::handleMicrocontrollerControl);
    }

    static boolean applyMicrocontrollerControl(final AbstractContainerMenu containerMenu, final MicrocontrollerControlPayload payload) {
        if (!(containerMenu instanceof MicrocontrollerMenu menu) || menu.containerId != payload.containerId()) {
            return false;
        }
        if (!(menu.microcontrollerInventory() instanceof MicrocontrollerBlockEntity microcontroller)) {
            return false;
        }
        final boolean running = microcontroller.machine().isRunning() || microcontroller.machine().isPaused();
        if (payload.action() == RackControlPayload.START) {
            return running || microcontroller.toggleMachine();
        }
        if (payload.action() == RackControlPayload.STOP) {
            return !running || microcontroller.toggleMachine();
        }
        return payload.action() == RackControlPayload.TOGGLE && microcontroller.toggleMachine();
    }

    static boolean applyMicrocontrollerControl(final Player player, final AbstractContainerMenu containerMenu, final MicrocontrollerControlPayload payload) {
        if (!(containerMenu instanceof MicrocontrollerMenu menu) || menu.containerId != payload.containerId()) {
            return false;
        }
        if (player == null || !menu.stillValid(player)) {
            return false;
        }
        return applyMicrocontrollerControl(containerMenu, payload);
    }

    private static void handleMicrocontrollerControl(final MicrocontrollerControlPayload payload, final IPayloadContext context) {
        applyMicrocontrollerControl(context.player(), context.player().containerMenu, payload);
    }

    private MicrocontrollerNetworking() {
    }
}
