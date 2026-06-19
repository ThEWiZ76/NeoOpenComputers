package li.cil.oc.api.machine;

import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Network;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

public final class TestNodes {
    private TestNodes() {
    }

    public static Node node(final String address) {
        return new TestNode(address);
    }

    private static final class TestNode implements Node {
        private final String address;

        private TestNode(final String address) {
            this.address = address;
        }

        @Override
        public Environment host() {
            return new Environment() {
                @Override
                public Node node() {
                    return TestNode.this;
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
            };
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
        public Network network() {
            return null;
        }

        @Override
        public boolean isNeighborOf(final Node other) {
            return false;
        }

        @Override
        public boolean canBeReachedFrom(final Node other) {
            return true;
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
        }

        @Override
        public void save(final CompoundTag nbt) {
        }
    }
}
