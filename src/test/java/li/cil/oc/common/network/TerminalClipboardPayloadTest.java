package li.cil.oc.common.network;

import io.netty.buffer.Unpooled;
import li.cil.oc.NeoOpenComputers;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TerminalClipboardPayloadTest {
    @Test
    void exposesStablePayloadType() {
        assertEquals(
            ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "terminal_clipboard"),
            TerminalClipboardPayload.TYPE.id());
    }

    @Test
    void roundTripsThroughStreamCodec() {
        final TerminalClipboardPayload payload = new TerminalClipboardPayload(4, "alpha\nbeta");
        final RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
            Unpooled.buffer(),
            RegistryAccess.EMPTY,
            ConnectionType.NEOFORGE);

        TerminalClipboardPayload.STREAM_CODEC.encode(buffer, payload);
        final TerminalClipboardPayload decoded = TerminalClipboardPayload.STREAM_CODEC.decode(buffer);

        assertEquals(4, decoded.containerId());
        assertEquals("alpha\nbeta", decoded.value());
    }
}
