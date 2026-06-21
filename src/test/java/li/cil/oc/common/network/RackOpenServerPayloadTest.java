package li.cil.oc.common.network;

import io.netty.buffer.Unpooled;
import li.cil.oc.NeoOpenComputers;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RackOpenServerPayloadTest {
    @Test
    void exposesStablePayloadType() {
        assertEquals(
            ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "rack_open_server"),
            RackOpenServerPayload.TYPE.id());
    }

    @Test
    void roundTripsThroughStreamCodec() {
        final RackOpenServerPayload payload = new RackOpenServerPayload(7, 2);
        final RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
            Unpooled.buffer(),
            RegistryAccess.EMPTY,
            ConnectionType.NEOFORGE);

        RackOpenServerPayload.STREAM_CODEC.encode(buffer, payload);
        final RackOpenServerPayload decoded = RackOpenServerPayload.STREAM_CODEC.decode(buffer);

        assertEquals(7, decoded.containerId());
        assertEquals(2, decoded.slot());
    }
}
