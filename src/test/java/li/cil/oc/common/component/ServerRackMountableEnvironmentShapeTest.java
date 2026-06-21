package li.cil.oc.common.component;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ServerRackMountableEnvironmentShapeTest {
    @Test
    void exposesRackControlPowerEntryPoint() throws NoSuchMethodException {
        final Method method = ServerRackMountableEnvironment.class.getMethod("controlPower", int.class);

        assertEquals(boolean.class, method.getReturnType());
    }
}
