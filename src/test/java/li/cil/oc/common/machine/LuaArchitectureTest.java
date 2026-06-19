package li.cil.oc.common.machine;

import li.cil.oc.api.machine.ExecutionResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LuaArchitectureTest {
    @Test
    void executesConfiguredLuaChunkOnce() {
        LuaArchitecture architecture = new LuaArchitecture("counter = (counter or 0) + 1");

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(1, architecture.globalInteger("counter"));
    }

    @Test
    void reportsLuaRuntimeErrorsAsExecutionErrors() {
        LuaArchitecture architecture = new LuaArchitecture("error('boot failed')");

        assertTrue(architecture.initialize());
        ExecutionResult result = architecture.runThreaded(false);

        ExecutionResult.Error error = assertInstanceOf(ExecutionResult.Error.class, result);
        assertTrue(error.message.contains("boot failed"));
    }
}
