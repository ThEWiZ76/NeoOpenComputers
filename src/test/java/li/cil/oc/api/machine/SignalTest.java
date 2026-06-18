package li.cil.oc.api.machine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class SignalTest {
    @Test
    void exposesSignalNameAndArguments() {
        Object[] args = {"component_added", 42};
        Signal signal = new TestSignal("computer_signal", args);

        assertEquals("computer_signal", signal.name());
        assertArrayEquals(args, signal.args());
    }

    private record TestSignal(String name, Object[] args) implements Signal {
    }
}
