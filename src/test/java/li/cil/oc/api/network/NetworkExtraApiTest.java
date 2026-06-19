package li.cil.oc.api.network;

import li.cil.oc.api.machine.TestNodes;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

final class NetworkExtraApiTest {
    @Test
    void analyzableUsesModernPlayerAndDirection() throws NoSuchMethodException {
        Method onAnalyze = Analyzable.class.getMethod("onAnalyze", Player.class, Direction.class, float.class, float.class, float.class);
        Analyzable analyzable = (player, side, hitX, hitY, hitZ) -> new Node[]{TestNodes.node(side.getName())};

        Node[] nodes = analyzable.onAnalyze(null, Direction.NORTH, 0, 0, 0);

        assertArrayEquals(new Class<?>[]{Player.class, Direction.class, float.class, float.class, float.class}, onAnalyze.getParameterTypes());
        assertEquals("north", nodes[0].address());
    }

    @Test
    void wirelessEndpointUsesModernLevel() throws NoSuchMethodException {
        Method world = WirelessEndpoint.class.getMethod("world");
        TestWirelessEndpoint endpoint = new TestWirelessEndpoint();
        Packet packet = null;

        endpoint.receivePacket(packet, endpoint);

        assertEquals(Level.class, world.getReturnType());
        assertEquals(1, endpoint.x());
        assertEquals(2, endpoint.y());
        assertEquals(3, endpoint.z());
        assertNull(endpoint.world());
        assertSame(packet, endpoint.packet);
        assertSame(endpoint, endpoint.sender);
    }

    private static final class TestWirelessEndpoint implements WirelessEndpoint {
        private Packet packet;
        private WirelessEndpoint sender;

        @Override
        public int x() {
            return 1;
        }

        @Override
        public int y() {
            return 2;
        }

        @Override
        public int z() {
            return 3;
        }

        @Override
        public Level world() {
            return null;
        }

        @Override
        public void receivePacket(final Packet packet, final WirelessEndpoint sender) {
            this.packet = packet;
            this.sender = sender;
        }
    }
}
