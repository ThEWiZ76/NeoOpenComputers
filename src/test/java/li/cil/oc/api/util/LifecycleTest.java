package li.cil.oc.api.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class LifecycleTest {
    @Test
    void lifecycleStatesKeepUpstreamOrderAndCanBeObserved() {
        TestLifecycle lifecycle = new TestLifecycle();

        lifecycle.onLifecycleStateChange(Lifecycle.LifecycleState.Initializing);
        lifecycle.onLifecycleStateChange(Lifecycle.LifecycleState.Disposed);

        assertArrayEquals(new Lifecycle.LifecycleState[]{
                Lifecycle.LifecycleState.Constructing,
                Lifecycle.LifecycleState.Initializing,
                Lifecycle.LifecycleState.Initialized,
                Lifecycle.LifecycleState.Disposing,
                Lifecycle.LifecycleState.Disposed
        }, Lifecycle.LifecycleState.values());
        assertEquals(List.of(Lifecycle.LifecycleState.Initializing, Lifecycle.LifecycleState.Disposed), lifecycle.states);
    }

    private static final class TestLifecycle implements Lifecycle {
        private final List<Lifecycle.LifecycleState> states = new ArrayList<>();

        @Override
        public void onLifecycleStateChange(final LifecycleState state) {
            states.add(state);
        }
    }
}
