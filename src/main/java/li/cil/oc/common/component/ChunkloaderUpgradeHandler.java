package li.cil.oc.common.component;

import li.cil.oc.api.event.RobotMoveEvent;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Node;
import net.neoforged.neoforge.common.NeoForge;

public final class ChunkloaderUpgradeHandler {
    private ChunkloaderUpgradeHandler() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ChunkloaderUpgradeHandler::onRobotMove);
    }

    static void onRobotMove(final RobotMoveEvent.Post event) {
        if (event.agent == null) {
            return;
        }
        final Machine machine = event.agent.machine();
        if (machine == null || machine.node() == null) {
            return;
        }
        for (final Node node : machine.node().reachableNodes()) {
            if (node != null && node.host() instanceof ChunkloaderUpgradeEnvironment loader) {
                loader.refreshForcedChunks();
            }
        }
    }
}
