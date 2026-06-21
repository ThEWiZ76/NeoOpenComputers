package li.cil.oc.common.network;

import io.netty.buffer.Unpooled;
import li.cil.oc.NeoOpenComputers;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TerminalKeyPayloadTest {
    @Test
    void exposesStablePayloadType() {
        assertEquals(
            ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "terminal_key"),
            TerminalKeyPayload.TYPE.id());
    }

    @Test
    void roundTripsThroughStreamCodec() {
        final TerminalKeyPayload payload = new TerminalKeyPayload(8, true, 'x', 45);
        final RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
            Unpooled.buffer(),
            RegistryAccess.EMPTY,
            ConnectionType.NEOFORGE);

        TerminalKeyPayload.STREAM_CODEC.encode(buffer, payload);
        final TerminalKeyPayload decoded = TerminalKeyPayload.STREAM_CODEC.decode(buffer);

        assertEquals(8, decoded.containerId());
        assertEquals(true, decoded.pressed());
        assertEquals((int) 'x', decoded.character());
        assertEquals(45, decoded.keyCode());
    }
}
