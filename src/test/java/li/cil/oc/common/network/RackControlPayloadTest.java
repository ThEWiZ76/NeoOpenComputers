package li.cil.oc.common.network;

import io.netty.buffer.Unpooled;
import li.cil.oc.NeoOpenComputers;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RackControlPayloadTest {
    @Test
    void exposesStablePayloadType() {
        assertEquals(
            ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "rack_control"),
            RackControlPayload.TYPE.id());
    }

    @Test
    void roundTripsThroughStreamCodec() {
        final RackControlPayload payload = new RackControlPayload(7, 2, RackControlPayload.MAP, -1, 3);
        final RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
            Unpooled.buffer(),
            RegistryAccess.EMPTY,
            ConnectionType.NEOFORGE);

        RackControlPayload.STREAM_CODEC.encode(buffer, payload);
        final RackControlPayload decoded = RackControlPayload.STREAM_CODEC.decode(buffer);

        assertEquals(7, decoded.containerId());
        assertEquals(2, decoded.slot());
        assertEquals(RackControlPayload.MAP, decoded.action());
        assertEquals(-1, decoded.connectableIndex());
        assertEquals(3, decoded.side());
    }
}
