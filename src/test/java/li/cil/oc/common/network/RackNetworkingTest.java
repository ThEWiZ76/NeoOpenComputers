package li.cil.oc.common.network;

import li.cil.oc.api.Network;
import li.cil.oc.api.component.RackBusConnectable;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.util.StateAware;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.blockentity.RackBlockEntity;
import li.cil.oc.common.component.ServerRackMountableEnvironment;
import li.cil.oc.common.menu.RackMenu;
import li.cil.oc.common.menu.ServerRackMenu;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RackNetworkingTest {
    @Test
    void applyRackControlMapsRackMountableBusLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        final TestRackBlockEntity rack = allocateRack();
        final TestRackMountable mountable = new TestRackMountable();
        final RackMountable[] mountables = new RackMountable[RackBlockEntity.CONTAINER_SIZE];
        mountables[0] = mountable;
        setField(rack, RackBlockEntity.class, "mountables", mountables);
        final RackMenu menu = allocateMenu(17, rack);

        final RackControlPayload payload = new RackControlPayload(17, 0, RackControlPayload.MAP, -1, Direction.SOUTH.ordinal());

        assertTrue(RackNetworking.applyRackControl(menu, payload));
        assertTrue(mountable.node().isNeighborOf(rack.sidedNode(Direction.SOUTH)));
    }

    @Test
    void applyRackControlRejectsMappingPayloadForWrongContainer() throws Exception {
        OpenComputersApi.initialize();
        final TestRackBlockEntity rack = allocateRack();
        final RackMenu menu = allocateMenu(17, rack);

        final RackControlPayload payload = new RackControlPayload(18, 0, RackControlPayload.MAP, -1, Direction.SOUTH.ordinal());

        assertFalse(RackNetworking.applyRackControl(menu, payload));
    }

    @Test
    void applyRackControlTogglesRackRelayStateLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        final TestRackBlockEntity rack = allocateRack();
        final RackMenu menu = allocateMenu(17, rack);

        assertTrue(RackNetworking.applyRackControl(menu, RackControlPayload.relay(17, true)));
        assertTrue(rack.isRelayEnabled());
        assertTrue(RackNetworking.applyRackControl(menu, RackControlPayload.relay(17, false)));
        assertFalse(rack.isRelayEnabled());
    }

    @Test
    void applyRackControlRejectsStaleRackMenuLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        final TestRackBlockEntity rack = allocateRack();
        rack.valid = false;
        final RackMenu menu = allocateMenu(17, rack);

        assertFalse(RackNetworking.applyRackControl(null, menu, RackControlPayload.relay(17, true)));
        assertFalse(rack.isRelayEnabled());
    }

    @Test
    void applyServerRackControlRejectsStaleRackServerLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        final AtomicInteger stopCount = new AtomicInteger();
        final Rack rack = rackWithoutServer();
        final ServerRackMountableEnvironment server = allocateServer(rack, runningMachine(stopCount));
        final ServerRackMenu menu = allocateServerMenu(17, server, serverData(false, false));

        assertFalse(RackNetworking.applyServerRackControl(menu, new ServerRackControlPayload(17, RackControlPayload.STOP)));
        assertEquals(0, stopCount.get());
    }

    @Test
    void applyServerRackControlKeepsItemServerControlsAvailable() throws Exception {
        OpenComputersApi.initialize();
        final AtomicInteger stopCount = new AtomicInteger();
        final ServerRackMountableEnvironment server = allocateServer(null, runningMachine(stopCount));
        final ServerRackMenu menu = allocateServerMenu(17, server, serverData(true, false));

        assertTrue(RackNetworking.applyServerRackControl(menu, new ServerRackControlPayload(17, RackControlPayload.STOP)));
        assertEquals(1, stopCount.get());
    }

    private static TestRackBlockEntity allocateRack() throws Exception {
        return (TestRackBlockEntity) unsafe().allocateInstance(TestRackBlockEntity.class);
    }

    private static RackMenu allocateMenu(final int containerId, final Container rackInventory) throws Exception {
        final RackMenu menu = (RackMenu) unsafe().allocateInstance(RackMenu.class);
        setField(menu, AbstractContainerMenu.class, "containerId", containerId);
        setField(menu, RackMenu.class, "rackInventory", rackInventory);
        return menu;
    }

    private static ServerRackMenu allocateServerMenu(final int containerId, final Container serverInventory, final ContainerData serverData) throws Exception {
        final ServerRackMenu menu = (ServerRackMenu) unsafe().allocateInstance(ServerRackMenu.class);
        setField(menu, AbstractContainerMenu.class, "containerId", containerId);
        setField(menu, ServerRackMenu.class, "serverInventory", serverInventory);
        setField(menu, ServerRackMenu.class, "serverData", serverData);
        return menu;
    }

    private static ServerRackMountableEnvironment allocateServer(final Rack rack, final Machine machine) throws Exception {
        final ServerRackMountableEnvironment server = (ServerRackMountableEnvironment) unsafe().allocateInstance(ServerRackMountableEnvironment.class);
        setField(server, ServerRackMountableEnvironment.class, "rack", rack);
        setField(server, ServerRackMountableEnvironment.class, "slot", 0);
        setField(server, ServerRackMountableEnvironment.class, "tier", 0);
        setField(server, ServerRackMountableEnvironment.class, "machine", machine);
        return server;
    }

    private static Rack rackWithoutServer() {
        return (Rack) Proxy.newProxyInstance(RackNetworkingTest.class.getClassLoader(), new Class<?>[]{Rack.class}, (proxy, method, args) -> defaultValue(method.getReturnType()));
    }

    private static Machine runningMachine(final AtomicInteger stopCount) {
        return (Machine) Proxy.newProxyInstance(RackNetworkingTest.class.getClassLoader(), new Class<?>[]{Machine.class}, (proxy, method, args) -> switch (method.getName()) {
            case "isRunning" -> true;
            case "isPaused" -> false;
            case "stop" -> {
                stopCount.incrementAndGet();
                yield true;
            }
            case "start", "pause", "crash", "canInteract", "signal" -> false;
            case "componentCount", "maxComponents" -> 0;
            case "getCostPerTick", "upTime", "cpuTime", "worldTime" -> 0;
            case "components", "methods" -> java.util.Map.of();
            case "users" -> new String[0];
            case "tmpAddress", "lastError" -> null;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static ContainerData serverData(final boolean item, final boolean present) {
        return new ContainerData() {
            @Override
            public int get(final int index) {
                if (index == ServerRackMenu.SERVER_IS_ITEM_INDEX) {
                    return item ? 1 : 0;
                }
                if (index == ServerRackMenu.SERVER_PRESENT_INDEX) {
                    return present ? 1 : 0;
                }
                return 0;
            }

            @Override
            public void set(final int index, final int value) {
            }

            @Override
            public int getCount() {
                return ServerRackMenu.SERVER_DATA_COUNT;
            }
        };
    }

    private static Object defaultValue(final Class<?> type) {
        if (type == Void.TYPE) {
            return null;
        }
        if (type == Boolean.TYPE) {
            return false;
        }
        if (type == Byte.TYPE) {
            return (byte) 0;
        }
        if (type == Short.TYPE) {
            return (short) 0;
        }
        if (type == Integer.TYPE) {
            return 0;
        }
        if (type == Long.TYPE) {
            return 0L;
        }
        if (type == Float.TYPE) {
            return 0F;
        }
        if (type == Double.TYPE) {
            return 0D;
        }
        if (type == Character.TYPE) {
            return (char) 0;
        }
        return null;
    }

    private static Unsafe unsafe() throws Exception {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (Unsafe) unsafeField.get(null);
    }

    private static void setField(final Object target, final Class<?> owner, final String name, final Object value) throws Exception {
        final Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void setField(final Object target, final Class<?> owner, final String name, final int value) throws Exception {
        final Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.setInt(target, value);
    }

    private static final class TestRackBlockEntity extends RackBlockEntity {
        private boolean valid = true;

        private TestRackBlockEntity() {
            super(null, (BlockState) null);
        }

        @Override
        public Direction facing() {
            return Direction.NORTH;
        }

        @Override
        public boolean stillValid(final Player player) {
            return valid;
        }
    }

    private static final class TestRackMountable implements RackMountable {
        private final Node node = Network.newNode(this, Visibility.Network).create();

        @Override public CompoundTag getData() { return new CompoundTag(); }
        @Override public int getConnectableCount() { return 0; }
        @Override public RackBusConnectable getConnectableAt(final int index) { return null; }
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
    }
}
