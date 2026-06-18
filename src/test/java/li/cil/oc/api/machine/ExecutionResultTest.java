package li.cil.oc.api.machine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

final class ExecutionResultTest {
    @Test
    void sleepStoresRequestedTickCount() {
        var result = new ExecutionResult.Sleep(20);

        assertInstanceOf(ExecutionResult.class, result);
        assertEquals(20, result.ticks);
    }

    @Test
    void shutdownStoresRebootFlag() {
        var result = new ExecutionResult.Shutdown(true);

        assertInstanceOf(ExecutionResult.class, result);
        assertEquals(true, result.reboot);
    }

    @Test
    void errorStoresMessage() {
        var result = new ExecutionResult.Error("computer crashed");

        assertInstanceOf(ExecutionResult.class, result);
        assertEquals("computer crashed", result.message);
    }

    @Test
    void synchronizedCallIsExecutionResult() {
        assertInstanceOf(ExecutionResult.class, new ExecutionResult.SynchronizedCall());
    }
}
