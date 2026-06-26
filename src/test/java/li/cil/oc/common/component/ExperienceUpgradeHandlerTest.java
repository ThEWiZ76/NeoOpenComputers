package li.cil.oc.common.component;

import li.cil.oc.api.event.RobotBreakBlockEvent;
import li.cil.oc.api.event.RobotExhaustionEvent;
import li.cil.oc.api.event.RobotMoveEvent;
import li.cil.oc.api.event.RobotPlaceBlockEvent;
import li.cil.oc.api.event.RobotUsedToolEvent;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Network;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExperienceUpgradeHandlerTest {
    @Test
    void robotBreakBlockPostAddsActionAndOreExperienceLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        final ExperienceUpgradeEnvironment upgrade = new ExperienceUpgradeEnvironment(null);

        ExperienceUpgradeHandler.onRobotBreakBlockPost(new RobotBreakBlockEvent.Post(agentWith(upgrade), 2D));

        assertEquals(2D * ModSettings.robotOreXpRate() + ModSettings.robotActionXp(), storedExperience(upgrade), 0.000_001D);
    }

    @Test
    void robotPlaceMoveAndExhaustionEventsAddExperienceLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        final ExperienceUpgradeEnvironment upgrade = new ExperienceUpgradeEnvironment(null);
        final Agent agent = agentWith(upgrade);

        ExperienceUpgradeHandler.onRobotPlaceBlockPost(new RobotPlaceBlockEvent.Post(agent, null, null, BlockPos.ZERO));
        ExperienceUpgradeHandler.onRobotMovePost(new RobotMoveEvent.Post(agent, Direction.NORTH));
        ExperienceUpgradeHandler.onRobotExhaustion(new RobotExhaustionEvent(agent, 0.25D));

        final double expected = ModSettings.robotActionXp()
            + ModSettings.robotExhaustionXpRate() * 0.01D
            + ModSettings.robotExhaustionXpRate() * 0.25D;
        assertEquals(expected, storedExperience(upgrade), 0.000_001D);
    }

    @Test
    void robotExperienceLevelReducesDamageAndBreakTimeLikeUpstream() {
        OpenComputersApi.initialize();
        final ExperienceUpgradeEnvironment upgrade = new ExperienceUpgradeEnvironment(null);
        upgrade.addExperience(ExperienceUpgradeEnvironment.xpForLevel(10));
        final Agent agent = agentWith(upgrade);

        final RobotUsedToolEvent.ComputeDamageRate damage = new RobotUsedToolEvent.ComputeDamageRate(agent, null, null, 1D);
        ExperienceUpgradeHandler.onRobotComputeDamageRate(damage);
        assertEquals(1D - 10D * ModSettings.toolEfficiencyPerLevel(), damage.getDamageRate(), 0.000_001D);

        final RobotBreakBlockEvent.Pre breakBlock = new RobotBreakBlockEvent.Pre(agent, null, BlockPos.ZERO, 10D);
        ExperienceUpgradeHandler.onRobotBreakBlockPre(breakBlock);
        assertEquals(10D * (1D - 10D * ModSettings.harvestSpeedBoostPerLevel()), breakBlock.getBreakTime(), 0.000_001D);
    }

    @Test
    void ignoresAgentsWithoutReachableExperienceUpgrade() {
        final RobotUsedToolEvent.ComputeDamageRate damage = new RobotUsedToolEvent.ComputeDamageRate(agentWith(), null, null, 1D);

        ExperienceUpgradeHandler.onRobotComputeDamageRate(damage);

        assertEquals(1D, damage.getDamageRate(), 0.000_001D);
    }

    private static Agent agentWith(final Environment... environments) {
        final List<Node> reachable = java.util.Arrays.stream(environments)
            .map(TestNode::new)
            .map(Node.class::cast)
            .toList();
        final Node machineNode = new TestNode(null, reachable);
        final Machine machine = (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "node" -> machineNode;
                case "toString" -> "TestMachine";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> defaultValue(method.getReturnType());
            });
        return (Agent) Proxy.newProxyInstance(
            Agent.class.getClassLoader(),
            new Class<?>[]{Agent.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "machine" -> machine;
                case "toString" -> "TestAgent";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> defaultValue(method.getReturnType());
            });
    }

    private static double storedExperience(final ExperienceUpgradeEnvironment upgrade) throws ReflectiveOperationException {
        final Field experience = ExperienceUpgradeEnvironment.class.getDeclaredField("experience");
        experience.setAccessible(true);
        return experience.getDouble(upgrade);
    }

    private static Object defaultValue(final Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == void.class) {
            return null;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }

    private record TestNode(Environment host, Iterable<Node> reachableNodes) implements Node {
        private TestNode(final Environment host) {
            this(host, List.of());
        }

        @Override public Visibility reachability() { return Visibility.Network; }
        @Override public String address() { return "test"; }
        @Override public Network network() { return null; }
        @Override public boolean isNeighborOf(final Node other) { return false; }
        @Override public boolean canBeReachedFrom(final Node other) { return false; }
        @Override public Iterable<Node> neighbors() { return List.of(); }
        @Override public void connect(final Node node) {}
        @Override public void disconnect(final Node node) {}
        @Override public void remove() {}
        @Override public void sendToAddress(final String target, final String name, final Object... data) {}
        @Override public void sendToNeighbors(final String name, final Object... data) {}
        @Override public void sendToReachable(final String name, final Object... data) {}
        @Override public void sendToVisible(final String name, final Object... data) {}
        @Override public void load(final CompoundTag nbt) {}
        @Override public void save(final CompoundTag nbt) {}
    }
}
