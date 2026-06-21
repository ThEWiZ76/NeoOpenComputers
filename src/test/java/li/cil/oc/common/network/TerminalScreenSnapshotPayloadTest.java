package li.cil.oc.common.network;

import io.netty.buffer.Unpooled;
import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.component.TerminalScreenSnapshot;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TerminalScreenSnapshotPayloadTest {
    @Test
    void exposesStablePayloadType() {
        assertEquals(
            ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "terminal_screen_snapshot"),
            TerminalScreenSnapshotPayload.TYPE.id());
    }

    @Test
    void roundTripsThroughStreamCodec() {
        final TerminalScreenSnapshot snapshot = new TerminalScreenSnapshot(8, 2, new String[]{"neo", "open"});
        final TerminalScreenSnapshotPayload payload = new TerminalScreenSnapshotPayload(42, snapshot);
        final RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
            Unpooled.buffer(),
            RegistryAccess.EMPTY,
            ConnectionType.NEOFORGE);

        TerminalScreenSnapshotPayload.STREAM_CODEC.encode(buffer, payload);
        final TerminalScreenSnapshotPayload decoded = TerminalScreenSnapshotPayload.STREAM_CODEC.decode(buffer);

        assertEquals(42, decoded.containerId());
        assertEquals(8, decoded.snapshot().width());
        assertEquals(2, decoded.snapshot().height());
        assertEquals("neo", decoded.snapshot().line(0));
        assertEquals("open", decoded.snapshot().line(1));
    }
}
