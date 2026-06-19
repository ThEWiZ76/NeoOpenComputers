package li.cil.oc.api.machine;

import li.cil.oc.api.network.ManagedEnvironment;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MachineContractTest {
    @Test
    void machineExtendsManagedEnvironmentAndContext() {
        assertTrue(ManagedEnvironment.class.isAssignableFrom(Machine.class));
        assertTrue(Context.class.isAssignableFrom(Machine.class));
    }

    @Test
    void exposesCoreMachineStateAndInvocationSignatures() throws NoSuchMethodException {
        assertEquals(MachineHost.class, Machine.class.getMethod("host").getReturnType());
        assertEquals(Architecture.class, Machine.class.getMethod("architecture").getReturnType());
        assertEquals(Map.class, Machine.class.getMethod("components").getReturnType());
        assertEquals(Signal.class, Machine.class.getMethod("popSignal").getReturnType());
        assertEquals(String[].class, Machine.class.getMethod("users").getReturnType());

        Method componentInvoke = Machine.class.getMethod("invoke", String.class, String.class, Object[].class);
        Method valueInvoke = Machine.class.getMethod("invoke", Value.class, String.class, Object[].class);

        assertArrayEquals(new Class<?>[]{String.class, String.class, Object[].class}, componentInvoke.getParameterTypes());
        assertEquals(Object[].class, componentInvoke.getReturnType());
        assertArrayEquals(new Class<?>[]{Value.class, String.class, Object[].class}, valueInvoke.getParameterTypes());
        assertEquals(Object[].class, valueInvoke.getReturnType());
    }
}
