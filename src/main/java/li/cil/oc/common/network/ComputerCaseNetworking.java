package li.cil.oc.common.network;

import li.cil.oc.api.machine.Machine;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.menu.ComputerCaseMenu;
import net.minecraft.network.chat.Component;
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
        final ComputerCaseBlockEntity computer = menu.computerInventory() instanceof ComputerCaseBlockEntity blockEntity ? blockEntity : null;
        final boolean wasRunning = computer != null && (computer.machine().isRunning() || computer.machine().isPaused());
        final boolean accepted = applyComputerCaseControl(containerMenu, payload);
        if (computer != null) {
            final Component message = startFailureMessage(computer.machine(), wasRunning, payload.action());
            if (message != null) {
                player.sendSystemMessage(message);
            }
        }
        return accepted;
    }

    static Component startFailureMessage(final Machine machine, final boolean wasRunning, final int action) {
        if (action != RackControlPayload.START || wasRunning) {
            return null;
        }
        return startErrorMessage(machine);
    }

    static Component startErrorMessage(final Machine machine) {
        if (machine == null || machine.lastError() == null || machine.lastError().isEmpty()) {
            return null;
        }
        return Component.literal("Last error: " + firstErrorLine(machine.lastError()));
    }

    static String firstErrorLine(final String lastError) {
        int end = lastError.length();
        final int carriageReturn = lastError.indexOf('\r');
        final int lineFeed = lastError.indexOf('\n');
        if (carriageReturn >= 0) {
            end = Math.min(end, carriageReturn);
        }
        if (lineFeed >= 0) {
            end = Math.min(end, lineFeed);
        }
        return lastError.substring(0, end);
    }

    private static void handleComputerCaseControl(final ComputerCaseControlPayload payload, final IPayloadContext context) {
        applyComputerCaseControl(context.player(), context.player().containerMenu, payload);
    }

    private ComputerCaseNetworking() {
    }
}
