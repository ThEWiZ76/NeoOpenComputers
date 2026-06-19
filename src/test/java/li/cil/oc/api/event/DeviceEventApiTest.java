package li.cil.oc.api.event;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DeviceEventApiTest {
    @Test
    void filesystemAccessEventsUseModernLevelCompoundTagAndAreCancelable() throws NoSuchMethodException {
        Constructor<FileSystemAccessEvent.Server> constructor = FileSystemAccessEvent.Server.class.getConstructor(String.class, Level.class, double.class, double.class, double.class, Node.class);
        FileSystemAccessEvent.Server server = new FileSystemAccessEvent.Server("disk", null, 1, 2, 3, null);
        CompoundTag data = new CompoundTag();
        FileSystemAccessEvent.Client client = new FileSystemAccessEvent.Client("disk", null, 4, 5, 6, data);

        assertEquals(Level.class, constructor.getParameterTypes()[1]);
        assertInstanceOf(Event.class, server);
        assertCancelable(server);
        assertEquals("disk", server.getSound());
        assertNull(server.getWorld());
        assertEquals(1, server.getX());
        assertEquals(2, server.getY());
        assertEquals(3, server.getZ());
        assertNull(server.getTileEntity());
        assertNull(server.getNode());
        assertSame(data, client.getData());
    }

    @Test
    void networkActivityEventsUseModernLevelCompoundTag() throws NoSuchMethodException {
        Constructor<NetworkActivityEvent.Server> constructor = NetworkActivityEvent.Server.class.getConstructor(Level.class, double.class, double.class, double.class, Node.class);
        NetworkActivityEvent.Server server = new NetworkActivityEvent.Server(null, 1, 2, 3, null);
        CompoundTag data = new CompoundTag();
        NetworkActivityEvent.Client client = new NetworkActivityEvent.Client(null, 4, 5, 6, data);

        assertEquals(Level.class, constructor.getParameterTypes()[0]);
        assertInstanceOf(Event.class, server);
        assertNull(server.getWorld());
        assertEquals(1, server.getX());
        assertEquals(2, server.getY());
        assertEquals(3, server.getZ());
        assertNull(server.getTileEntity());
        assertNull(server.getNode());
        assertSame(data, client.getData());
    }

    @Test
    void geolyzerEventsExposeBoundsDataAndAreCancelable() {
        GeolyzerEvent.Scan scan = new GeolyzerEvent.Scan(null, Map.of("noise", false), -1, -2, -3, 1, 2, 3);
        GeolyzerEvent.Analyze analyze = new GeolyzerEvent.Analyze(null, Map.of(), BlockPos.ZERO);

        assertSame(Boolean.FALSE, scan.options.get("noise"));
        assertEquals(-1, scan.minX);
        assertEquals(-2, scan.minY);
        assertEquals(-3, scan.minZ);
        assertEquals(1, scan.maxX);
        assertEquals(2, scan.maxY);
        assertEquals(3, scan.maxZ);
        assertEquals(64, scan.data.length);
        assertSame(BlockPos.ZERO, analyze.pos);
        analyze.data.put("name", "stone");
        assertEquals("stone", analyze.data.get("name"));
        assertCancelable(scan);
        assertCancelable(analyze);
    }

    @Test
    void signChangePreEventIsCancelableAndUsesModernSignBlockEntity() throws NoSuchMethodException {
        Constructor<SignChangeEvent.Pre> constructor = SignChangeEvent.Pre.class.getConstructor(SignBlockEntity.class, String[].class);
        String[] lines = new String[]{"a", "b", "c", "d"};
        SignChangeEvent.Pre pre = new SignChangeEvent.Pre(null, lines);
        SignChangeEvent.Post post = new SignChangeEvent.Post(null, lines);

        assertEquals(SignBlockEntity.class, constructor.getParameterTypes()[0]);
        assertNull(pre.sign);
        assertSame(lines, pre.lines);
        assertSame(lines, post.lines);
        assertCancelable(pre);
    }

    @Test
    void blockEntityConstructorsAreAvailableForHostedDevices() throws NoSuchMethodException {
        FileSystemAccessEvent.Server.class.getConstructor(String.class, BlockEntity.class, Node.class);
        FileSystemAccessEvent.Client.class.getConstructor(String.class, BlockEntity.class, CompoundTag.class);
        NetworkActivityEvent.Server.class.getConstructor(BlockEntity.class, Node.class);
        NetworkActivityEvent.Client.class.getConstructor(BlockEntity.class, CompoundTag.class);
    }

    private static void assertCancelable(final Event event) {
        ICancellableEvent cancellable = assertInstanceOf(ICancellableEvent.class, event);
        cancellable.setCanceled(true);
        assertTrue(cancellable.isCanceled());
    }
}
