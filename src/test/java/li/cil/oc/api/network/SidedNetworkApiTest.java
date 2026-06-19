package li.cil.oc.api.network;

import li.cil.oc.api.machine.TestNodes;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SidedNetworkApiTest {
    @Test
    void sidedComponentUsesModernDirection() {
        SidedComponent component = side -> side == Direction.NORTH;

        assertTrue(component.canConnectNode(Direction.NORTH));
        assertFalse(component.canConnectNode(Direction.SOUTH));
    }

    @Test
    void sidedEnvironmentReturnsPerSideNodes() {
        Node node = TestNodes.node("sided");
        SidedEnvironment environment = new TestSidedEnvironment(node);

        assertSame(node, environment.sidedNode(Direction.UP));
        assertNull(environment.sidedNode(Direction.DOWN));
        assertTrue(environment.canConnect(Direction.UP));
        assertFalse(environment.canConnect(Direction.DOWN));
    }

    private static final class TestSidedEnvironment implements SidedEnvironment {
        private final Node node;

        private TestSidedEnvironment(final Node node) {
            this.node = node;
        }

        @Override
        public Node sidedNode(final Direction side) {
            return side == Direction.UP ? node : null;
        }

        @Override
        public boolean canConnect(final Direction side) {
            return sidedNode(side) != null;
        }
    }
}
