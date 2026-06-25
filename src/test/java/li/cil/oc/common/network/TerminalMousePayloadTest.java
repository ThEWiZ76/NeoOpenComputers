package li.cil.oc.common.network;

import io.netty.buffer.Unpooled;
import li.cil.oc.NeoOpenComputers;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TerminalMousePayloadTest {
    @Test
    void exposesStablePayloadType() {
        assertEquals(
            ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "terminal_mouse"),
            TerminalMousePayload.TYPE.id());
    }

    @Test
    void roundTripsThroughStreamCodec() {
        final TerminalMousePayload payload = new TerminalMousePayload(
            4,
            TerminalMousePayload.MOUSE_SCROLL,
            3.5D,
            8.0D,
            -1);
        final RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
            Unpooled.buffer(),
            RegistryAccess.EMPTY,
            ConnectionType.NEOFORGE);

        TerminalMousePayload.STREAM_CODEC.encode(buffer, payload);
        final TerminalMousePayload decoded = TerminalMousePayload.STREAM_CODEC.decode(buffer);

        assertEquals(4, decoded.containerId());
        assertEquals(TerminalMousePayload.MOUSE_SCROLL, decoded.kind());
        assertEquals(3.5D, decoded.x());
        assertEquals(8.0D, decoded.y());
        assertEquals(-1, decoded.buttonOrDelta());
    }

    @Test
    void streamCodecQuantizesCoordinatesToUpstreamFloatPrecision() {
        final double x = 1.0D / 3.0D;
        final double y = 2.0D / 3.0D;
        final TerminalMousePayload payload = new TerminalMousePayload(
            4,
            TerminalMousePayload.MOUSE_DOWN,
            x,
            y,
            1);
        final RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
            Unpooled.buffer(),
            RegistryAccess.EMPTY,
            ConnectionType.NEOFORGE);

        TerminalMousePayload.STREAM_CODEC.encode(buffer, payload);
        final TerminalMousePayload decoded = TerminalMousePayload.STREAM_CODEC.decode(buffer);

        assertEquals((double) (float) x, decoded.x());
        assertEquals((double) (float) y, decoded.y());
    }

    @Test
    void streamCodecQuantizesButtonOrScrollToUpstreamSignedByte() {
        final TerminalMousePayload payload = new TerminalMousePayload(
            4,
            TerminalMousePayload.MOUSE_SCROLL,
            1.0D,
            2.0D,
            130);
        final RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
            Unpooled.buffer(),
            RegistryAccess.EMPTY,
            ConnectionType.NEOFORGE);

        TerminalMousePayload.STREAM_CODEC.encode(buffer, payload);
        final TerminalMousePayload decoded = TerminalMousePayload.STREAM_CODEC.decode(buffer);

        assertEquals((byte) 130, decoded.buttonOrDelta());
    }
}
