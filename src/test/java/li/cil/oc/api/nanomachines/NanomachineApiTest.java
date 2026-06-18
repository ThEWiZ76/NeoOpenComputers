package li.cil.oc.api.nanomachines;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NanomachineApiTest {
    @Test
    void disableReasonsKeepUpstreamOrder() {
        assertArrayEquals(
                new DisableReason[]{DisableReason.Default, DisableReason.InputChanged, DisableReason.OutOfEnergy},
                DisableReason.values());
    }

    @Test
    void behaviorExposesLifecycleMethods() {
        var behavior = new TestBehavior();

        behavior.onEnable();
        behavior.update();
        behavior.onDisable(DisableReason.InputChanged);

        assertEquals("test", behavior.getNameHint());
        assertTrue(behavior.enabled);
        assertTrue(behavior.updated);
        assertSame(DisableReason.InputChanged, behavior.disabledReason);
    }

    @Test
    void controllerExposesInputsBehaviorsAndEnergyBuffer() {
        var behavior = new TestBehavior();
        Controller controller = new TestController(behavior);

        assertSame(controller, controller.reconfigure());
        assertEquals(4, controller.getTotalInputCount());
        assertEquals(2, controller.getSafeActiveInputs());
        assertEquals(3, controller.getMaxActiveInputs());
        assertTrue(controller.setInput(1, true));
        assertTrue(controller.getInput(1));
        assertEquals(List.of(behavior), controller.getActiveBehaviors());
        assertEquals(1, controller.getInputCount(behavior));
        assertEquals(10.0, controller.getLocalBuffer());
        assertEquals(20.0, controller.getLocalBufferSize());
        assertEquals(0.5, controller.changeBuffer(0.5));
    }

    private static final class TestBehavior implements Behavior {
        private boolean enabled;
        private boolean updated;
        private DisableReason disabledReason;

        @Override
        public String getNameHint() {
            return "test";
        }

        @Override
        public void onEnable() {
            enabled = true;
        }

        @Override
        public void onDisable(final DisableReason reason) {
            disabledReason = reason;
        }

        @Override
        public void update() {
            updated = true;
        }
    }

    private static final class TestController implements Controller {
        private final Behavior behavior;
        private boolean input;

        private TestController(final Behavior behavior) {
            this.behavior = behavior;
        }

        @Override
        public Controller reconfigure() {
            return this;
        }

        @Override
        public int getTotalInputCount() {
            return 4;
        }

        @Override
        public int getSafeActiveInputs() {
            return 2;
        }

        @Override
        public int getMaxActiveInputs() {
            return 3;
        }

        @Override
        public boolean getInput(final int index) {
            return input;
        }

        @Override
        public boolean setInput(final int index, final boolean value) {
            input = value;
            return true;
        }

        @Override
        public Iterable<Behavior> getActiveBehaviors() {
            return List.of(behavior);
        }

        @Override
        public int getInputCount(final Behavior behavior) {
            return behavior == this.behavior ? 1 : 0;
        }

        @Override
        public double getLocalBuffer() {
            return 10;
        }

        @Override
        public double getLocalBufferSize() {
            return 20;
        }

        @Override
        public double changeBuffer(final double delta) {
            return delta;
        }
    }
}
