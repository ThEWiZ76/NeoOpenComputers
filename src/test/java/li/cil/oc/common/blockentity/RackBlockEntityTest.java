package li.cil.oc.common.blockentity;

import li.cil.oc.api.Network;
import li.cil.oc.api.component.RackBusConnectable;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.util.StateAware;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RackBlockEntityTest {
    @Test
    void rackExposesFourRackMountableSlots() {
        assertEquals(4, RackBlockEntity.CONTAINER_SIZE);
        assertTrue(RackBlockEntity.acceptsDriverSlot(Slot.RackMountable));
        assertFalse(RackBlockEntity.acceptsDriverSlot(Slot.HDD));
        assertFalse(RackBlockEntity.acceptsDriverSlot(Slot.Upgrade));
    }

    @Test
    void rackIsContainerAndApiRack() {
        assertTrue(Container.class.isAssignableFrom(RackBlockEntity.class));
        assertTrue(Rack.class.isAssignableFrom(RackBlockEntity.class));
        assertTrue(MenuProvider.class.isAssignableFrom(RackBlockEntity.class));
    }

    @Test
    void rackExposesSidedBusNodesExceptFrontLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        final RackBlockEntity rack = allocateRack();

        assertFalse(rack.canConnect(Direction.NORTH));
        assertTrue(rack.canConnect(Direction.SOUTH));
        assertTrue(rack.canConnect(Direction.UP));
        assertNull(rack.sidedNode(Direction.NORTH));

        final Node southBus = rack.sidedNode(Direction.SOUTH);
        assertNotNull(southBus);
        assertSame(southBus, rack.sidedNode(Direction.SOUTH));
        assertTrue(southBus instanceof Connector);
        assertEquals(Visibility.Network, southBus.reachability());
        assertArrayEquals(new Node[]{southBus}, rack.onAnalyze(null, Direction.SOUTH, 0.5F, 0.5F, 0.5F));
    }

    @Test
    void rackConnectsPrimaryMountableNodeToSideBusLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        final RackBlockEntity rack = allocateRack();
        final TestRackMountable mountable = new TestRackMountable("server");
        final RackMountable[] mountables = new RackMountable[RackBlockEntity.CONTAINER_SIZE];
        mountables[0] = mountable;
        setField(rack, "mountables", mountables);

        final Method connect = RackBlockEntity.class.getMethod("connect", int.class, int.class, Direction.class);
        connect.invoke(rack, 0, -1, Direction.SOUTH);

        final Node bus = rack.sidedNode(Direction.SOUTH);
        assertTrue(mountable.node().isNeighborOf(bus));
        assertSame(bus.network(), mountable.node().network());

        connect.invoke(rack, 0, -1, null);

        assertFalse(mountable.node().isNeighborOf(bus));
    }

    @Test
    void rackRoutesSecondaryBusPacketsWithoutExposingComponentsLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        final RackBlockEntity rack = allocateRack();
        final TestRackBusConnectable connectable = new TestRackBusConnectable();
        final TestRackMountable mountable = new TestRackMountable("server", connectable);
        final RackMountable[] mountables = new RackMountable[RackBlockEntity.CONTAINER_SIZE];
        mountables[0] = mountable;
        setField(rack, "mountables", mountables);

        final Method connect = RackBlockEntity.class.getMethod("connect", int.class, int.class, Direction.class);
        connect.invoke(rack, 0, 0, Direction.SOUTH);

        final Node bus = rack.sidedNode(Direction.SOUTH);
        final TestEnvironment external = new TestEnvironment();
        final Node externalNode = Network.newNode(external, Visibility.Network).create();
        Network.joinNewNetwork(bus);
        externalNode.connect(bus);

        final TestPacket inbound = new TestPacket("remote", null, 123, new Object[]{"payload"});
        externalNode.sendToReachable("network.message", inbound);

        assertSame(inbound, connectable.packet);
        assertFalse(connectable.node().canBeReachedFrom(bus));

        final TestPacket outbound = new TestPacket(connectable.node().address(), null, 124, new Object[]{"reply"});
        connectable.node().sendToReachable("network.message", outbound);

        assertSame(outbound, external.packet);
    }

    @Test
    void onAnalyzeFrontFaceUsesClickedSlotLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        final RackBlockEntity rack = allocateRack();
        final TestRackMountable topMountable = new TestRackMountable("top_server");
        final TestRackMountable bottomMountable = new TestRackMountable("bottom_server");
        final RackMountable[] mountables = new RackMountable[RackBlockEntity.CONTAINER_SIZE];
        mountables[0] = topMountable;
        mountables[3] = bottomMountable;
        setField(rack, "mountables", mountables);

        assertArrayEquals(new Node[]{bottomMountable.node()}, rack.onAnalyze(null, Direction.NORTH, 0.5F, 0.1F, 0.5F));
    }

    private static RackBlockEntity allocateRack() throws Exception {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (RackBlockEntity) ((Unsafe) unsafeField.get(null)).allocateInstance(TestRackBlockEntity.class);
    }

    private static void setField(final RackBlockEntity rack, final String name, final Object value) throws Exception {
        final Field field = RackBlockEntity.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(rack, value);
    }

    private static final class TestRackMountable implements RackMountable, Analyzable {
        private final Node node;
        private final RackBusConnectable[] connectables;

        private TestRackMountable(final String componentName, final RackBusConnectable... connectables) {
            this.connectables = connectables;
            node = Network.newNode(this, Visibility.Network)
                .withComponent(componentName, Visibility.Network)
                .create();
        }

        @Override public CompoundTag getData() { return new CompoundTag(); }
        @Override public int getConnectableCount() { return connectables.length; }
        @Override public RackBusConnectable getConnectableAt(final int index) { return index >= 0 && index < connectables.length ? connectables[index] : null; }
        @Override public boolean onActivate(final Player player, final InteractionHand hand, final ItemStack heldItem, final float hitX, final float hitY) { return false; }
        @Override public EnumSet<StateAware.State> getCurrentState() { return EnumSet.noneOf(StateAware.State.class); }
        @Override public boolean canUpdate() { return false; }
        @Override public void update() {}
        @Override public Node node() { return node; }
        @Override public void onConnect(final Node node) {}
        @Override public void onDisconnect(final Node node) {}
        @Override public void onMessage(final Message message) {}
        @Override public void load(final CompoundTag nbt) {}
        @Override public void save(final CompoundTag nbt) {}
        @Override public Node[] onAnalyze(final Player player, final Direction side, final float hitX, final float hitY, final float hitZ) {
            return new Node[]{node};
        }
    }

    private static final class TestRackBusConnectable implements RackBusConnectable {
        private final Node node = Network.newNode(this, Visibility.Network).create();
        private Packet packet;

        @Override public void receivePacket(final Packet packet) { this.packet = packet; }
        @Override public Node node() { return node; }
        @Override public void onConnect(final Node node) {}
        @Override public void onDisconnect(final Node node) {}
        @Override public void onMessage(final Message message) {}
    }

    private static final class TestEnvironment implements Environment {
        private Packet packet;

        @Override public Node node() { return null; }
        @Override public void onConnect(final Node node) {}
        @Override public void onDisconnect(final Node node) {}
        @Override public void onMessage(final Message message) {
            if ("network.message".equals(message.name()) && message.data().length == 1 && message.data()[0] instanceof Packet packet) {
                this.packet = packet;
            }
        }
    }

    private record TestPacket(String source, String destination, int port, Object[] data) implements Packet {
        @Override public int size() { return 0; }
        @Override public int ttl() { return 1; }
        @Override public Packet hop() { return this; }
        @Override public void save(final CompoundTag nbt) {}
    }

    private static final class TestRackBlockEntity extends RackBlockEntity {
        private TestRackBlockEntity() {
            super(null, (BlockState) null);
        }

        @Override
        public Direction facing() {
            return Direction.NORTH;
        }
    }
}
