package li.cil.oc.api.network;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

final class PacketTest {
    @Test
    void packetExposesNetworkPayloadAndSavesToCompoundTag() {
        var tag = new CompoundTag();
        Packet hop = new TestPacket("source", null, 123, new Object[]{"payload"}, 7);
        Packet packet = new TestPacket("source", "target", 123, new Object[]{"payload"}, 8, hop);

        packet.save(tag);

        assertEquals("source", packet.source());
        assertEquals("target", packet.destination());
        assertEquals(123, packet.port());
        assertArrayEquals(new Object[]{"payload"}, packet.data());
        assertEquals(7, packet.size());
        assertEquals(8, packet.ttl());
        assertSame(hop, packet.hop());
        assertEquals("source", tag.getString("source"));
    }

    private record TestPacket(
            String source,
            String destination,
            int port,
            Object[] data,
            int ttl,
            Packet hop
    ) implements Packet {
        private TestPacket(final String source, final String destination, final int port, final Object[] data, final int ttl) {
            this(source, destination, port, data, ttl, null);
        }

        @Override
        public int size() {
            return data.length + 6;
        }

        @Override
        public void save(final CompoundTag nbt) {
            nbt.putString("source", source);
        }
    }
}
