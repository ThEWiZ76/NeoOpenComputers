package li.cil.oc.common;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.item.AnalyzerItem;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class AnalyzerItemTest {
    @Test
    void describesEnvironmentComponentAddressAndEnergy() {
        NetworkRegistry registry = new NetworkRegistry();
        TestEnvironment host = new TestEnvironment();
        ComponentConnector node = registry.newNode(host, Visibility.Network)
            .withComponent("geolyzer", Visibility.Network)
            .withConnector(10D)
            .create();
        host.node = node;
        registry.joinNewNetwork(node);
        node.changeBuffer(4D);

        List<Component> lines = AnalyzerItem.describe(host);

        assertTrue(contains(lines, "Component: geolyzer"));
        assertTrue(contains(lines, "Address: node-1"));
        assertTrue(contains(lines, "Stored energy: 4.00/10.00"));
    }

    @Test
    void describesDeviceInfoMetadata() {
        NetworkRegistry registry = new NetworkRegistry();
        TestDeviceEnvironment host = new TestDeviceEnvironment();
        ComponentConnector node = registry.newNode(host, Visibility.Network)
            .withComponent("geolyzer", Visibility.Network)
            .withConnector(10D)
            .create();
        host.node = node;
        registry.joinNewNetwork(node);

        List<Component> lines = AnalyzerItem.describe(host);

        assertTrue(contains(lines, "Device class: generic"));
        assertTrue(contains(lines, "Description: Geolyzer"));
        assertTrue(contains(lines, "Vendor: MightyPirates"));
        assertTrue(contains(lines, "Product: Terrain Analyzer MkII"));
    }

    @Test
    void ignoresNullAnalyzableNodesLikeUpstream() {
        List<Component> lines = AnalyzerItem.describe(new NullNodeAnalyzable());

        assertTrue(lines.isEmpty());
    }

    @Test
    void describesMachineLastErrorAsFirstLineForPlayerFeedback() {
        NetworkRegistry registry = new NetworkRegistry();
        Node[] holder = new Node[1];
        Machine machine = machine(holder, "boot:60 no bootable medium found\r\nstack traceback");
        Node node = registry.newNode(machine, Visibility.Network)
            .withConnector(10D)
            .create();
        holder[0] = node;
        registry.joinNewNetwork(node);

        List<Component> lines = AnalyzerItem.describe(machine);

        assertTrue(contains(lines, "Last error: boot:60 no bootable medium found"));
    }

    private static boolean contains(final List<Component> lines, final String expected) {
        return lines.stream().anyMatch(line -> line.getString().equals(expected));
    }

    private static Machine machine(final Node[] holder, final String lastError) {
        return (Machine) Proxy.newProxyInstance(AnalyzerItemTest.class.getClassLoader(), new Class<?>[]{Machine.class}, (proxy, method, args) -> switch (method.getName()) {
            case "node" -> holder[0];
            case "lastError" -> lastError;
            case "componentCount", "maxComponents" -> 0;
            case "components", "methods" -> Map.of();
            case "users" -> new String[0];
            case "isRunning", "isPaused", "start", "pause", "stop", "crash", "canInteract", "signal", "canUpdate", "removeUser" -> false;
            case "getCostPerTick", "upTime", "cpuTime" -> 0D;
            case "worldTime" -> 0L;
            case "tmpAddress", "host", "architecture", "popSignal" -> null;
            case "beep", "consumeCallBudget", "addUser", "onHostChanged", "load", "save", "onConnect", "onDisconnect", "onMessage", "setCostPerTick", "update" -> null;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static Object defaultValue(final Class<?> type) {
        if (type == Void.TYPE) {
            return null;
        }
        if (type == Boolean.TYPE) {
            return false;
        }
        if (type == Integer.TYPE) {
            return 0;
        }
        if (type == Long.TYPE) {
            return 0L;
        }
        if (type == Double.TYPE) {
            return 0D;
        }
        return null;
    }

    private static class TestEnvironment implements Environment {
        protected Node node;

        @Override
        public Node node() {
            return node;
        }

        @Override
        public void onConnect(final Node node) {
        }

        @Override
        public void onDisconnect(final Node node) {
        }

        @Override
        public void onMessage(final Message message) {
        }
    }

    private static final class TestDeviceEnvironment extends TestEnvironment implements DeviceInfo {
        @Override
        public Map<String, String> getDeviceInfo() {
            return Map.of(
                DeviceAttribute.Class, DeviceClass.Generic,
                DeviceAttribute.Description, "Geolyzer",
                DeviceAttribute.Vendor, "MightyPirates",
                DeviceAttribute.Product, "Terrain Analyzer MkII");
        }
    }

    private static final class NullNodeAnalyzable implements Analyzable {
        @Override
        public Node[] onAnalyze(final Player player, final Direction side, final float hitX, final float hitY, final float hitZ) {
            return new Node[]{null};
        }
    }
}
