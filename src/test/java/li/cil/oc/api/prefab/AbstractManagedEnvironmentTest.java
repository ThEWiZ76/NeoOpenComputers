package li.cil.oc.api.prefab;

import li.cil.oc.api.API;
import li.cil.oc.api.detail.Builder;
import li.cil.oc.api.detail.NetworkAPI;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.network.WirelessEndpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AbstractManagedEnvironmentTest {
    @AfterEach
    void resetApiReference() {
        API.network = null;
    }

    @Test
    void providesNoopManagedEnvironmentDefaults() {
        TestManagedEnvironment environment = new TestManagedEnvironment();
        TestNode node = new TestNode("address");

        environment.installNode(node);

        assertSame(node, environment.node());
        assertFalse(environment.canUpdate());
        environment.update();
        environment.onConnect(node);
        environment.onDisconnect(node);
        environment.onMessage(null);
    }

    @Test
    void loadDelegatesNodeStateFromNodeTag() {
        TestManagedEnvironment environment = new TestManagedEnvironment();
        TestNode node = new TestNode("address");
        CompoundTag tag = new CompoundTag();
        tag.put(AbstractManagedEnvironment.NODE_TAG, new CompoundTag());
        environment.installNode(node);

        environment.load(tag);

        assertTrue(node.loaded);
    }

    @Test
    void saveWritesNodeStateWhenNodeAlreadyHasAddress() {
        TestManagedEnvironment environment = new TestManagedEnvironment();
        TestNode node = new TestNode("address");
        CompoundTag tag = new CompoundTag();
        environment.installNode(node);

        environment.save(tag);

        assertTrue(node.saved);
        assertTrue(tag.contains(AbstractManagedEnvironment.NODE_TAG));
        assertFalse(node.removed);
    }

    @Test
    void saveTemporarilyJoinsNewNetworkForAddresslessNode() {
        TestNetworkAPI api = new TestNetworkAPI();
        API.network = api;
        TestManagedEnvironment environment = new TestManagedEnvironment();
        TestNode node = new TestNode(null);
        CompoundTag tag = new CompoundTag();
        environment.installNode(node);

        environment.save(tag);

        assertSame(node, api.joinedNode);
        assertTrue(node.saved);
        assertTrue(tag.contains(AbstractManagedEnvironment.NODE_TAG));
        assertTrue(node.removed);
    }

    private static final class TestManagedEnvironment extends AbstractManagedEnvironment {
        private void installNode(final Node node) {
            setNode(node);
        }
    }

    private static final class TestNode implements Node {
        private String address;
        private boolean loaded;
        private boolean saved;
        private boolean removed;

        private TestNode(final String address) {
            this.address = address;
        }

        @Override
        public Environment host() {
            return null;
        }

        @Override
        public Visibility reachability() {
            return Visibility.Network;
        }

        @Override
        public String address() {
            return address;
        }

        @Override
        public li.cil.oc.api.network.Network network() {
            return null;
        }

        @Override
        public boolean isNeighborOf(final Node other) {
            return false;
        }

        @Override
        public boolean canBeReachedFrom(final Node other) {
            return false;
        }

        @Override
        public Iterable<Node> neighbors() {
            return List.of();
        }

        @Override
        public Iterable<Node> reachableNodes() {
            return List.of();
        }

        @Override
        public void connect(final Node node) {
        }

        @Override
        public void disconnect(final Node node) {
        }

        @Override
        public void remove() {
            removed = true;
        }

        @Override
        public void sendToAddress(final String target, final String name, final Object... data) {
        }

        @Override
        public void sendToNeighbors(final String name, final Object... data) {
        }

        @Override
        public void sendToReachable(final String name, final Object... data) {
        }

        @Override
        public void sendToVisible(final String name, final Object... data) {
        }

        @Override
        public void load(final CompoundTag nbt) {
            loaded = true;
        }

        @Override
        public void save(final CompoundTag nbt) {
            saved = true;
            nbt.putString("address", address == null ? "" : address);
        }
    }

    private static final class TestNetworkAPI implements NetworkAPI {
        private Node joinedNode;

        @Override
        public void joinOrCreateNetwork(final BlockEntity blockEntity) {
        }

        @Override
        public void joinOrCreateNetwork(final BlockGetter world, final BlockPos pos) {
        }

        @Override
        public void joinNewNetwork(final Node node) {
            joinedNode = node;
            ((TestNode) node).address = "joined";
        }

        @Override
        public void joinWirelessNetwork(final WirelessEndpoint endpoint) {
        }

        @Override
        public void updateWirelessNetwork(final WirelessEndpoint endpoint) {
        }

        @Override
        public void leaveWirelessNetwork(final WirelessEndpoint endpoint) {
        }

        @Override
        public void leaveWirelessNetwork(final WirelessEndpoint endpoint, final ResourceKey<Level> dimension) {
        }

        @Override
        public void sendWirelessPacket(final WirelessEndpoint source, final double strength, final Packet packet) {
        }

        @Override
        public Builder.NodeBuilder newNode(final Environment host, final Visibility reachability) {
            return null;
        }

        @Override
        public Packet newPacket(final String source, final String destination, final int port, final Object[] data) {
            return null;
        }

        @Override
        public Packet newPacket(final CompoundTag nbt) {
            return null;
        }
    }
}
