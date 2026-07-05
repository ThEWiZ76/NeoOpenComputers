package li.cil.oc.common.network;

import li.cil.oc.common.blockentity.RobotBlockEntity;
import li.cil.oc.common.menu.RobotMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class RobotNetworking {
    public static final String NETWORK_VERSION = "1";

    public static void register(final RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION)
            .playToServer(
                RobotControlPayload.TYPE,
                RobotControlPayload.STREAM_CODEC,
                RobotNetworking::handleRobotControl);
    }

    static boolean applyRobotControl(final AbstractContainerMenu containerMenu, final RobotControlPayload payload) {
        if (!(containerMenu instanceof RobotMenu menu) || menu.containerId != payload.containerId()) {
            return false;
        }
        if (!(menu.robotInventory() instanceof RobotBlockEntity robot)) {
            return false;
        }
        final boolean running = robot.machine().isRunning() || robot.machine().isPaused();
        if (payload.action() == RackControlPayload.START) {
            return running || robot.toggleMachine();
        }
        if (payload.action() == RackControlPayload.STOP) {
            return !running || robot.toggleMachine();
        }
        return payload.action() == RackControlPayload.TOGGLE && robot.toggleMachine();
    }

    static boolean applyRobotControl(final Player player, final AbstractContainerMenu containerMenu, final RobotControlPayload payload) {
        if (!(containerMenu instanceof RobotMenu menu) || menu.containerId != payload.containerId()) {
            return false;
        }
        if (player == null || !menu.stillValid(player)) {
            return false;
        }
        final RobotBlockEntity robot = menu.robotInventory() instanceof RobotBlockEntity blockEntity ? blockEntity : null;
        final boolean wasRunning = robot != null && (robot.machine().isRunning() || robot.machine().isPaused());
        final boolean accepted = applyRobotControl(containerMenu, payload);
        if (robot != null) {
            final Component message = ComputerCaseNetworking.startFailureMessage(robot.machine(), wasRunning, payload.action());
            if (message != null) {
                player.displayClientMessage(message, false);
            }
        }
        return accepted;
    }

    private static void handleRobotControl(final RobotControlPayload payload, final IPayloadContext context) {
        applyRobotControl(context.player(), context.player().containerMenu, payload);
    }

    private RobotNetworking() {
    }
}
