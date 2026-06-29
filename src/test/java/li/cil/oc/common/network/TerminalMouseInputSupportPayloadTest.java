package li.cil.oc.common.network;

import io.netty.buffer.Unpooled;
import li.cil.oc.NeoOpenComputers;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TerminalMouseInputSupportPayloadTest {
    @Test
    void exposesStablePayloadType() {
        assertEquals(
            ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "terminal_mouse_input_support"),
            TerminalMouseInputSupportPayload.TYPE.id());
    }

    @Test
    void roundTripsThroughStreamCodec() {
        final TerminalMouseInputSupportPayload payload = new TerminalMouseInputSupportPayload(7, false);
        final RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
            Unpooled.buffer(),
            RegistryAccess.EMPTY,
            ConnectionType.NEOFORGE);

        TerminalMouseInputSupportPayload.STREAM_CODEC.encode(buffer, payload);
        final TerminalMouseInputSupportPayload decoded = TerminalMouseInputSupportPayload.STREAM_CODEC.decode(buffer);

        assertEquals(7, decoded.containerId());
        assertEquals(false, decoded.supportsMouseInput());
    }
}
