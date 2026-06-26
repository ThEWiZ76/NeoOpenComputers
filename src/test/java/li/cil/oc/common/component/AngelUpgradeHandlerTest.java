package li.cil.oc.common.component;

import li.cil.oc.api.event.RobotPlaceInAirEvent;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Network;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AngelUpgradeHandlerTest {
    @Test
    void keepsRobotPlaceInAirDeniedWithoutReachableAngelUpgrade() {
        final TestNode machineNode = new TestNode(null, false, List.of());
        final RobotPlaceInAirEvent event = new RobotPlaceInAirEvent(agent(machineNode));

        AngelUpgradeHandler.onPlaceInAir(event);

        assertFalse(event.isAllowed());
    }

    @Test
    void allowsRobotPlaceInAirWithReachableAngelUpgradeLikeUpstream() {
        final TestNode angelNode = new TestNode(new AngelUpgradeEnvironment(), true, List.of());
        final TestNode machineNode = new TestNode(null, false, List.of(angelNode));
        final RobotPlaceInAirEvent event = new RobotPlaceInAirEvent(agent(machineNode));

        AngelUpgradeHandler.onPlaceInAir(event);

        assertTrue(event.isAllowed());
    }

    @Test
    void ignoresAngelUpgradeNodeThatCannotReachMachineLikeUpstream() {
        final TestNode angelNode = new TestNode(new AngelUpgradeEnvironment(), false, List.of());
        final TestNode machineNode = new TestNode(null, false, List.of(angelNode));
        final RobotPlaceInAirEvent event = new RobotPlaceInAirEvent(agent(machineNode));

        AngelUpgradeHandler.onPlaceInAir(event);

        assertFalse(event.isAllowed());
    }

    private static Agent agent(final Node node) {
        final Machine machine = (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "node" -> node;
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

    private record TestNode(Environment host, boolean canBeReachedFromMachine, Iterable<Node> reachableNodes) implements Node {
        @Override public Visibility reachability() { return Visibility.Network; }
        @Override public String address() { return "test"; }
        @Override public Network network() { return null; }
        @Override public boolean isNeighborOf(final Node other) { return false; }
        @Override public boolean canBeReachedFrom(final Node other) { return canBeReachedFromMachine; }
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
