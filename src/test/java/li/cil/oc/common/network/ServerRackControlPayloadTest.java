package li.cil.oc.common.network;

import io.netty.buffer.Unpooled;
import li.cil.oc.NeoOpenComputers;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ServerRackControlPayloadTest {
    @Test
    void exposesStablePayloadType() {
        assertEquals(
            ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "server_rack_control"),
            ServerRackControlPayload.TYPE.id());
    }

    @Test
    void roundTripsThroughStreamCodec() {
        final ServerRackControlPayload payload = new ServerRackControlPayload(7, RackControlPayload.TOGGLE);
        final RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
            Unpooled.buffer(),
            RegistryAccess.EMPTY,
            ConnectionType.NEOFORGE);

        ServerRackControlPayload.STREAM_CODEC.encode(buffer, payload);
        final ServerRackControlPayload decoded = ServerRackControlPayload.STREAM_CODEC.decode(buffer);

        assertEquals(7, decoded.containerId());
        assertEquals(RackControlPayload.TOGGLE, decoded.action());
    }
}
