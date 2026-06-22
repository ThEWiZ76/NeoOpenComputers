package li.cil.oc.common.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class NanomachinesNetworkingShapeTest {
    @Test
    void exposesRegisterPayloadHandlerEntryPoint() throws NoSuchMethodException {
        final Method register = NanomachinesNetworking.class.getMethod("register", RegisterPayloadHandlersEvent.class);

        assertEquals(void.class, register.getReturnType());
    }

    @Test
    void exposesPowerApplyEntryPoint() throws NoSuchMethodException {
        final Method apply = NanomachinesNetworking.class.getDeclaredMethod("applyPower", NanomachinePowerPayload.class);

        assertEquals(void.class, apply.getReturnType());
    }
}
