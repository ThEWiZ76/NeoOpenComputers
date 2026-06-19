package li.cil.oc.api.machine;

import li.cil.oc.api.network.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ContextTest {
    @Test
    void exposesComputerControlAndSignalOperations() {
        Node node = TestNodes.node("computer");
        var context = new TestContext(node);

        context.consumeCallBudget(0.5);

        assertSame(node, context.node());
        assertTrue(context.canInteract("player"));
        assertTrue(context.isRunning());
        assertFalse(context.isPaused());
        assertTrue(context.start());
        assertTrue(context.pause(0.25));
        assertTrue(context.stop());
        assertTrue(context.signal("component_added", "gpu"));
        assertTrue(context.consumedBudget);
    }

    private static final class TestContext implements Context {
        private final Node node;
        private boolean consumedBudget;

        private TestContext(final Node node) {
            this.node = node;
        }

        @Override
        public Node node() {
            return node;
        }

        @Override
        public boolean canInteract(final String player) {
            return true;
        }

        @Override
        public boolean isRunning() {
            return true;
        }

        @Override
        public boolean isPaused() {
            return false;
        }

        @Override
        public boolean start() {
            return true;
        }

        @Override
        public boolean pause(final double seconds) {
            return seconds > 0;
        }

        @Override
        public boolean stop() {
            return true;
        }

        @Override
        public void consumeCallBudget(final double callCost) {
            consumedBudget = callCost > 0;
        }

        @Override
        public boolean signal(final String name, final Object... args) {
            return "component_added".equals(name) && args.length == 1;
        }
    }
}
