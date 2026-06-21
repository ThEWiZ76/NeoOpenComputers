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

    @Test
    void exposesTerminalKeyApplyEntryPoint() throws NoSuchMethodException {
        final Method apply = TerminalNetworking.class.getDeclaredMethod(
            "applyTerminalKey",
            net.minecraft.world.inventory.AbstractContainerMenu.class,
            TerminalKeyPayload.class,
            net.minecraft.world.entity.player.Player.class);

        assertEquals(void.class, apply.getReturnType());
    }
}
