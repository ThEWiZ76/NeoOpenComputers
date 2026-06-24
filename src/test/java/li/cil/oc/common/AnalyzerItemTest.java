package li.cil.oc.common;

import li.cil.oc.api.driver.DeviceInfo;
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

    private static boolean contains(final List<Component> lines, final String expected) {
        return lines.stream().anyMatch(line -> line.getString().equals(expected));
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
