package li.cil.oc.common;

import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.item.AnalyzerItem;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    private static boolean contains(final List<Component> lines, final String expected) {
        return lines.stream().anyMatch(line -> line.getString().equals(expected));
    }

    private static final class TestEnvironment implements Environment {
        private Node node;

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
}
