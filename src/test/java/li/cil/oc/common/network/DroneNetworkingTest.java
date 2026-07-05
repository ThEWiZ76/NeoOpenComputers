package li.cil.oc.common.network;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

final class DroneNetworkingTest {
    @Test
    void droneControlPayloadAndNetworkingAreRegisteredShape() {
        final Class<?> payload = assertDoesNotThrow(() -> Class.forName("li.cil.oc.common.network.DroneControlPayload"));
        final Class<?> networking = assertDoesNotThrow(() -> Class.forName("li.cil.oc.common.network.DroneNetworking"));

        assertDoesNotThrow(() -> payload.getDeclaredField("TYPE"));
        assertDoesNotThrow(() -> payload.getDeclaredField("STREAM_CODEC"));
        assertDoesNotThrow(() -> networking.getDeclaredMethod("register", net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent.class));
        assertDoesNotThrow(() -> networking.getDeclaredMethod("applyDroneControl", AbstractContainerMenu.class, payload));
        assertDoesNotThrow(() -> networking.getDeclaredMethod("applyDroneControl", Player.class, AbstractContainerMenu.class, payload));
    }
}
