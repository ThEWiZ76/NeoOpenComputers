package li.cil.oc.common.component;

import li.cil.oc.api.event.RobotPlaceInAirEvent;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Node;
import net.neoforged.neoforge.common.NeoForge;

public final class AngelUpgradeHandler {
    private AngelUpgradeHandler() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(AngelUpgradeHandler::onPlaceInAir);
    }

    static void onPlaceInAir(final RobotPlaceInAirEvent event) {
        if (event.agent == null) {
            return;
        }
        final Machine machine = event.agent.machine();
        if (machine == null || machine.node() == null) {
            return;
        }
        final Node machineNode = machine.node();
        for (final Node node : machineNode.reachableNodes()) {
            if (node != null && node.canBeReachedFrom(machineNode) && node.host() instanceof AngelUpgradeEnvironment) {
                event.setAllowed(true);
                return;
            }
        }
    }
}
