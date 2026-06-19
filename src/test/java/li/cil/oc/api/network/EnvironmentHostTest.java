package li.cil.oc.api.network;

import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class EnvironmentHostTest {
    @Test
    void exposesModernLevelBackedHostPositionAndChangeMarker() {
        TestHost host = new TestHost(List.of(new TestEnvironment()));

        host.markChanged();

        assertNull(host.world());
        assertEquals(1.5, host.xPosition());
        assertEquals(2.5, host.yPosition());
        assertEquals(3.5, host.zPosition());
        assertEquals(1, host.getComponents().spliterator().getExactSizeIfKnown());
        assertEquals(1, host.changed);
    }

    private static final class TestHost implements ComponentHost {
        private final Iterable<Environment> environments;
        private int changed;

        private TestHost(final Iterable<Environment> environments) {
            this.environments = environments;
        }

        @Override
        public Level world() {
            return null;
        }

        @Override
        public double xPosition() {
            return 1.5;
        }

        @Override
        public double yPosition() {
            return 2.5;
        }

        @Override
        public double zPosition() {
            return 3.5;
        }

        @Override
        public void markChanged() {
            changed++;
        }

        @Override
        public Iterable<Environment> getComponents() {
            return environments;
        }
    }

    private static final class TestEnvironment implements Environment {
        @Override
        public Node node() {
            return null;
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
