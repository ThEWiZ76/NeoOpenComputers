package li.cil.oc.common.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TerminalNetworkingShapeTest {
    @Test
    void exposesRegisterPayloadHandlerEntryPoint() throws NoSuchMethodException {
        final Method register = TerminalNetworking.class.getMethod("register", RegisterPayloadHandlersEvent.class);

        assertEquals(void.class, register.getReturnType());
    }
}
