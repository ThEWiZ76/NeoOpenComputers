package li.cil.oc.common.component;

import li.cil.oc.api.event.RobotBreakBlockEvent;
import li.cil.oc.api.event.RobotExhaustionEvent;
import li.cil.oc.api.event.RobotMoveEvent;
import li.cil.oc.api.event.RobotPlaceBlockEvent;
import li.cil.oc.api.event.RobotUsedToolEvent;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.ModSettings;
import net.neoforged.neoforge.common.NeoForge;

public final class ExperienceUpgradeHandler {
    private ExperienceUpgradeHandler() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ExperienceUpgradeHandler::onRobotComputeDamageRate);
        NeoForge.EVENT_BUS.addListener(ExperienceUpgradeHandler::onRobotBreakBlockPre);
        NeoForge.EVENT_BUS.addListener(ExperienceUpgradeHandler::onRobotBreakBlockPost);
        NeoForge.EVENT_BUS.addListener(ExperienceUpgradeHandler::onRobotPlaceBlockPost);
        NeoForge.EVENT_BUS.addListener(ExperienceUpgradeHandler::onRobotMovePost);
        NeoForge.EVENT_BUS.addListener(ExperienceUpgradeHandler::onRobotExhaustion);
    }

    static void onRobotComputeDamageRate(final RobotUsedToolEvent.ComputeDamageRate event) {
        event.setDamageRate(event.getDamageRate() * Math.max(0D, 1D - totalLevel(event.agent) * ModSettings.toolEfficiencyPerLevel()));
    }

    static void onRobotBreakBlockPre(final RobotBreakBlockEvent.Pre event) {
        event.setBreakTime(event.getBreakTime() * Math.max(0D, 1D - totalLevel(event.agent) * ModSettings.harvestSpeedBoostPerLevel()));
    }

    static void onRobotBreakBlockPost(final RobotBreakBlockEvent.Post event) {
        addExperience(event.agent, event.experience * ModSettings.robotOreXpRate() + ModSettings.robotActionXp());
    }

    static void onRobotPlaceBlockPost(final RobotPlaceBlockEvent.Post event) {
        addExperience(event.agent, ModSettings.robotActionXp());
    }

    static void onRobotMovePost(final RobotMoveEvent.Post event) {
        addExperience(event.agent, ModSettings.robotExhaustionXpRate() * 0.01D);
    }

    static void onRobotExhaustion(final RobotExhaustionEvent event) {
        addExperience(event.agent, ModSettings.robotExhaustionXpRate() * event.exhaustion);
    }

    private static int totalLevel(final Agent agent) {
        final int[] result = {0};
        forEachUpgrade(agent, upgrade -> result[0] += upgrade.storedLevel());
        return result[0];
    }

    private static void addExperience(final Agent agent, final double amount) {
        forEachUpgrade(agent, upgrade -> upgrade.addExperience(amount));
    }

    private static void forEachUpgrade(final Agent agent, final UpgradeConsumer consumer) {
        if (agent == null) {
            return;
        }
        final Machine machine = agent.machine();
        if (machine == null || machine.node() == null) {
            return;
        }
        for (final Node node : machine.node().reachableNodes()) {
            if (node != null && node.host() instanceof ExperienceUpgradeEnvironment upgrade) {
                consumer.accept(upgrade);
            }
        }
    }

    @FunctionalInterface
    private interface UpgradeConsumer {
        void accept(ExperienceUpgradeEnvironment upgrade);
    }
}
