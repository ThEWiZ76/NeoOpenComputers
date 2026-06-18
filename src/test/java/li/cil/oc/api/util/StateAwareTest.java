package li.cil.oc.api.util;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class StateAwareTest {
    @Test
    void exposesWorkStatesInUpstreamOrder() {
        assertArrayEquals(
                new StateAware.State[]{StateAware.State.None, StateAware.State.CanWork, StateAware.State.IsWorking},
                StateAware.State.values());
    }

    @Test
    void exposesCurrentWorkState() {
        StateAware stateAware = () -> EnumSet.of(StateAware.State.CanWork);

        assertEquals(EnumSet.of(StateAware.State.CanWork), stateAware.getCurrentState());
    }
}
