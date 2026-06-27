package li.cil.oc.common.network;

import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.menu.ComputerCaseMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ComputerCaseNetworking {
    public static final String NETWORK_VERSION = "1";

    public static void register(final RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION)
            .playToServer(
                ComputerCaseControlPayload.TYPE,
                ComputerCaseControlPayload.STREAM_CODEC,
                ComputerCaseNetworking::handleComputerCaseControl);
    }

    static boolean applyComputerCaseControl(final AbstractContainerMenu containerMenu, final ComputerCaseControlPayload payload) {
        if (!(containerMenu instanceof ComputerCaseMenu menu) || menu.containerId != payload.containerId()) {
            return false;
        }
        if (!(menu.computerInventory() instanceof ComputerCaseBlockEntity computer)) {
            return false;
        }
        final boolean running = computer.machine().isRunning() || computer.machine().isPaused();
        if (payload.action() == RackControlPayload.START) {
            return running || computer.toggleMachine();
        }
        if (payload.action() == RackControlPayload.STOP) {
            return !running || computer.toggleMachine();
        }
        return payload.action() == RackControlPayload.TOGGLE && computer.toggleMachine();
    }

    static boolean applyComputerCaseControl(final Player player, final AbstractContainerMenu containerMenu, final ComputerCaseControlPayload payload) {
        if (!(containerMenu instanceof ComputerCaseMenu menu) || menu.containerId != payload.containerId()) {
            return false;
        }
        if (player == null || !menu.stillValid(player)) {
            return false;
        }
        return applyComputerCaseControl(containerMenu, payload);
    }

    private static void handleComputerCaseControl(final ComputerCaseControlPayload payload, final IPayloadContext context) {
        applyComputerCaseControl(context.player(), context.player().containerMenu, payload);
    }

    private ComputerCaseNetworking() {
    }
}
