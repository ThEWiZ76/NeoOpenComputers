package li.cil.oc.common.blockentity;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScreenBlockEntityTest {
    @Test
    void exposesScreenPowerCallbacks() throws NoSuchMethodException {
        assertCallback("isOn");
        assertCallback("turnOn");
        assertCallback("turnOff");
        assertCallback("getAspectRatio");
    }

    private static void assertCallback(final String methodName) throws NoSuchMethodException {
        Method method = ScreenBlockEntity.class.getMethod(methodName, Context.class, Arguments.class);
        assertTrue(method.isAnnotationPresent(Callback.class));
    }
}
