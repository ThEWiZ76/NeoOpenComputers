package li.cil.oc.common.network;

import io.netty.buffer.Unpooled;
import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.component.TerminalScreenDelta;
import li.cil.oc.common.component.TerminalScreenSnapshot;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TerminalScreenDeltaPayloadTest {
    @Test
    void exposesStablePayloadType() {
        assertEquals(
            ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "terminal_screen_delta"),
            TerminalScreenDeltaPayload.TYPE.id());
    }

    @Test
    void roundTripsThroughStreamCodec() {
        final TerminalScreenDelta delta = TerminalScreenDelta.between(
            new TerminalScreenSnapshot(4, 2, new String[]{"old ", "same"}),
            new TerminalScreenSnapshot(4, 2, new String[]{"new ", "same"}));
        final TerminalScreenDeltaPayload payload = new TerminalScreenDeltaPayload(7, delta);
        final RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
            Unpooled.buffer(),
            RegistryAccess.EMPTY,
            ConnectionType.NEOFORGE);

        TerminalScreenDeltaPayload.STREAM_CODEC.encode(buffer, payload);
        final TerminalScreenDeltaPayload decoded = TerminalScreenDeltaPayload.STREAM_CODEC.decode(buffer);

        assertEquals(7, decoded.containerId());
        assertEquals(1, decoded.delta().rowCount());
        assertEquals(0, decoded.delta().rowIndex(0));
    }
}
