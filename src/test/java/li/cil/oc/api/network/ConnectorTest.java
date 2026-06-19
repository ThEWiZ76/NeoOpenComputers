package li.cil.oc.api.network;

import li.cil.oc.api.machine.TestNodes;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConnectorTest {
    @Test
    void connectorExtendsNodeAndExposesEnergyBuffers() {
        var connector = new TestConnector();

        connector.setLocalBufferSize(64);

        assertSame(connector, (Node) connector);
        assertEquals(10, connector.localBuffer());
        assertEquals(64, connector.localBufferSize());
        assertEquals(20, connector.globalBuffer());
        assertEquals(128, connector.globalBufferSize());
        assertEquals(1.5, connector.changeBuffer(1.5));
        assertTrue(connector.tryChangeBuffer(-2));
    }

    private static final class TestConnector implements Connector {
        private double localBufferSize;

        @Override
        public double localBuffer() {
            return 10;
        }

        @Override
        public double localBufferSize() {
            return localBufferSize;
        }

        @Override
        public double globalBuffer() {
            return 20;
        }

        @Override
        public double globalBufferSize() {
            return 128;
        }

        @Override
        public double changeBuffer(final double delta) {
            return delta;
        }

        @Override
        public boolean tryChangeBuffer(final double delta) {
            return true;
        }

        @Override
        public void setLocalBufferSize(final double size) {
            localBufferSize = size;
        }

        @Override
        public Environment host() {
            return TestNodes.node("connector").host();
        }

        @Override
        public Visibility reachability() {
            return Visibility.Network;
        }

        @Override
        public String address() {
            return "connector";
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
            return java.util.List.of();
        }

        @Override
        public Iterable<Node> reachableNodes() {
            return java.util.List.of();
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
