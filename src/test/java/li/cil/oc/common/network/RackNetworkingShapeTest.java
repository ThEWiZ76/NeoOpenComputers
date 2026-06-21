package li.cil.oc.common.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RackNetworkingShapeTest {
    @Test
    void exposesRegisterPayloadHandlerEntryPoint() throws NoSuchMethodException {
        final Method register = RackNetworking.class.getMethod("register", RegisterPayloadHandlersEvent.class);

        assertEquals(void.class, register.getReturnType());
    }

    @Test
    void exposesRackControlApplyEntryPoint() throws NoSuchMethodException {
        final Method apply = RackNetworking.class.getDeclaredMethod(
            "applyRackControl",
            net.minecraft.world.inventory.AbstractContainerMenu.class,
            RackControlPayload.class);

        assertEquals(boolean.class, apply.getReturnType());
    }

    @Test
    void exposesRackOpenServerApplyEntryPoint() throws NoSuchMethodException {
        final Method apply = RackNetworking.class.getDeclaredMethod(
            "applyRackOpenServer",
            net.minecraft.world.entity.player.Player.class,
            net.minecraft.world.inventory.AbstractContainerMenu.class,
            RackOpenServerPayload.class);

        assertEquals(boolean.class, apply.getReturnType());
    }
}
