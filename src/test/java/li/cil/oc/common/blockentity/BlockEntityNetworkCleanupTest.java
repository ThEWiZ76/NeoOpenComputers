package li.cil.oc.common.blockentity;

import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Network;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class BlockEntityNetworkCleanupTest {
    @Test
    void keyboardRemovesNodeWhenUnloadedOrRemoved() throws Exception {
        KeyboardBlockEntity keyboard = allocate(KeyboardBlockEntity.class);
        CountingNode node = new CountingNode();
        setField(keyboard, "node", node);

        keyboard.onChunkUnloaded();
        keyboard.setRemoved();

        assertEquals(2, node.removals);
    }

    @Test
    void screenRemovesNodeWhenUnloadedOrRemoved() throws Exception {
        ScreenBlockEntity screen = allocate(ScreenBlockEntity.class);
        CountingNode node = new CountingNode();
        setField(screen, "node", node);

        screen.onChunkUnloaded();
        screen.setRemoved();

        assertEquals(2, node.removals);
    }

    @Test
    void diskDriveRemovesDriveAndDiskNodesWhenUnloadedOrRemoved() throws Exception {
        DiskDriveBlockEntity diskDrive = allocate(DiskDriveBlockEntity.class);
        CountingNode driveNode = new CountingNode();
        CountingNode diskNode = new CountingNode();
        setField(diskDrive, "node", driveNode);
        setField(diskDrive, "diskEnvironment", new TestManagedEnvironment(diskNode));

        diskDrive.onChunkUnloaded();
        diskDrive.setRemoved();

        assertEquals(2, driveNode.removals);
        assertEquals(2, diskNode.removals);
    }

    private static <T> T allocate(final Class<T> type) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(((Unsafe) field.get(null)).allocateInstance(type));
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class TestManagedEnvironment implements ManagedEnvironment {
        private final Node node;

        private TestManagedEnvironment(final Node node) {
            this.node = node;
        }

        @Override public Node node() { return node; }
        @Override public void onConnect(final Node node) {}
        @Override public void onDisconnect(final Node node) {}
        @Override public void onMessage(final Message message) {}
        @Override public boolean canUpdate() { return false; }
        @Override public void update() {}
        @Override public void load(final CompoundTag nbt) {}
        @Override public void save(final CompoundTag nbt) {}
    }

    private static final class CountingNode implements Node {
        private int removals;

        @Override public Environment host() { return null; }
        @Override public Visibility reachability() { return Visibility.Neighbors; }
        @Override public String address() { return "node"; }
        @Override public Network network() { return null; }
        @Override public boolean isNeighborOf(final Node other) { return false; }
        @Override public boolean canBeReachedFrom(final Node other) { return false; }
        @Override public Iterable<Node> neighbors() { return List.of(); }
        @Override public Iterable<Node> reachableNodes() { return List.of(); }
        @Override public void connect(final Node node) {}
        @Override public void disconnect(final Node node) {}
        @Override public void remove() { removals++; }
        @Override public void sendToAddress(final String target, final String name, final Object... data) {}
        @Override public void sendToNeighbors(final String name, final Object... data) {}
        @Override public void sendToReachable(final String name, final Object... data) {}
        @Override public void sendToVisible(final String name, final Object... data) {}
        @Override public void load(final CompoundTag nbt) {}
        @Override public void save(final CompoundTag nbt) {}
    }
}
