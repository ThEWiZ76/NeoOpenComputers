package li.cil.oc.common.menu;

import li.cil.oc.api.Network;
import li.cil.oc.api.component.RackBusConnectable;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.util.StateAware;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.blockentity.RackBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RackMenuShapeTest {
    @Test
    void rackMenuHasClientAndServerConstructors() throws NoSuchMethodException {
        final Constructor<RackMenu> clientConstructor = RackMenu.class.getConstructor(int.class, Inventory.class);
        final Constructor<RackMenu> serverConstructor = RackMenu.class.getConstructor(int.class, Inventory.class, Container.class);
        final Constructor<RackMenu> dataConstructor = RackMenu.class.getConstructor(int.class, Inventory.class, Container.class, ContainerData.class);

        assertTrue(AbstractContainerMenu.class.isAssignableFrom(RackMenu.class));
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class}, clientConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, Container.class}, serverConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, Container.class, ContainerData.class}, dataConstructor.getParameterTypes());
    }

    @Test
    void rackMenuSlotCountsAreStable() {
        assertEquals(4, RackMenu.RACK_SLOT_COUNT);
        assertEquals(36, RackMenu.PLAYER_SLOT_COUNT);
        assertEquals(40, RackMenu.TOTAL_SLOT_COUNT);
        assertEquals(4, RackMenu.RACK_STATE_COUNT);
        assertEquals(42, RackMenu.RACK_DATA_COUNT);
        assertEquals(4, RackMenu.RACK_MISSING_REQUIREMENTS_COUNT);
        assertEquals(16, RackMenu.RACK_NODE_MAPPING_COUNT);
        assertEquals(16, RackMenu.RACK_NODE_PRESENCE_COUNT);
        assertEquals(-1, RackMenu.NO_SIDE);
        assertEquals(0, RackMenu.STATE_EMPTY);
        assertEquals(1, RackMenu.STATE_READY);
        assertEquals(2, RackMenu.STATE_RUNNING);
        assertEquals(3, RackMenu.STATE_INCOMPLETE);
        assertEquals(1, RackMenu.MISSING_CPU);
        assertEquals(2, RackMenu.MISSING_MEMORY);
        assertEquals(4, RackMenu.MISSING_EEPROM);
    }

    @Test
    void rackMenuExposesServerRackInventoryTarget() throws NoSuchMethodException {
        assertEquals(Container.class, RackMenu.class.getMethod("rackInventory").getReturnType());
    }

    @Test
    void rackMenuExposesRackStateLookup() throws NoSuchMethodException {
        assertEquals(int.class, RackMenu.class.getMethod("rackState", int.class).getReturnType());
        assertEquals(int.class, RackMenu.class.getMethod("rackStateFor", Container.class, int.class).getReturnType());
        assertEquals(int.class, RackMenu.class.getMethod("rackMissingRequirements", int.class).getReturnType());
        assertEquals(int.class, RackMenu.class.getMethod("rackMissingRequirementsFor", Container.class, int.class).getReturnType());
        assertEquals(int.class, RackMenu.class.getMethod("rackNodeMapping", int.class, int.class).getReturnType());
        assertEquals(int.class, RackMenu.class.getMethod("rackNodeMappingFor", Container.class, int.class, int.class).getReturnType());
        assertEquals(boolean.class, RackMenu.class.getMethod("rackNodePresent", int.class, int.class).getReturnType());
        assertEquals(boolean.class, RackMenu.class.getMethod("rackNodePresentFor", Container.class, int.class, int.class).getReturnType());
        assertEquals(Direction.class, RackMenu.class.getMethod("rackFacing").getReturnType());
        assertEquals(int.class, RackMenu.class.getMethod("rackFacingFor", Container.class).getReturnType());
        assertEquals(boolean.class, RackMenu.class.getMethod("rackRelayEnabled").getReturnType());
        assertEquals(boolean.class, RackMenu.class.getMethod("rackRelayEnabledFor", Container.class).getReturnType());
    }

    @Test
    void rackMenuExposesNodeMappingAndPresenceLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        final TestRackBlockEntity rack = allocateRack();
        final TestRackBusConnectable connectable = new TestRackBusConnectable();
        final RackMountable[] mountables = new RackMountable[RackBlockEntity.CONTAINER_SIZE];
        mountables[0] = new TestRackMountable(connectable);
        setField(rack, "mountables", mountables);

        rack.connect(0, -1, Direction.SOUTH);
        rack.connect(0, 0, Direction.UP);

        assertEquals(Direction.SOUTH.ordinal(), RackMenu.rackNodeMappingFor(rack, 0, 0));
        assertEquals(Direction.UP.ordinal(), RackMenu.rackNodeMappingFor(rack, 0, 1));
        assertEquals(RackMenu.NO_SIDE, RackMenu.rackNodeMappingFor(rack, 0, 2));
        assertTrue(RackMenu.rackNodePresentFor(rack, 0, 0));
        assertTrue(RackMenu.rackNodePresentFor(rack, 0, 1));
        assertFalse(RackMenu.rackNodePresentFor(rack, 0, 2));
        assertFalse(RackMenu.rackNodePresentFor(rack, 1, 0));
    }

    @Test
    void rackMenuExposesFacingForRotatedRackBusControls() throws Exception {
        final TestRackBlockEntity rack = allocateRack();
        rack.facing = Direction.SOUTH;

        assertEquals(Direction.SOUTH.ordinal() + 1, RackMenu.rackFacingFor(rack));

        final RackMenu menu = allocateMenuWithData(rackDataWithFacing(Direction.WEST));

        assertEquals(Direction.WEST, menu.rackFacing());
    }

    @Test
    void rackMenuExposesRelayStateLikeUpstream() throws Exception {
        final TestRackBlockEntity rack = allocateRack();
        rack.setRelayEnabled(true);

        assertTrue(RackMenu.rackRelayEnabledFor(rack));

        final RackMenu menu = allocateMenuWithData(rackDataWithRelayState(true));

        assertTrue(menu.rackRelayEnabled());
    }

    private static TestRackBlockEntity allocateRack() throws Exception {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (TestRackBlockEntity) ((Unsafe) unsafeField.get(null)).allocateInstance(TestRackBlockEntity.class);
    }

    private static void setField(final RackBlockEntity rack, final String name, final Object value) throws Exception {
        final Field field = RackBlockEntity.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(rack, value);
    }

    private static RackMenu allocateMenuWithData(final ContainerData rackData) throws Exception {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        final RackMenu menu = (RackMenu) ((Unsafe) unsafeField.get(null)).allocateInstance(RackMenu.class);
        final Field rackDataField = RackMenu.class.getDeclaredField("rackData");
        rackDataField.setAccessible(true);
        rackDataField.set(menu, rackData);
        return menu;
    }

    private static ContainerData rackDataWithFacing(final Direction facing) {
        return new ContainerData() {
            @Override
            public int get(final int index) {
                return index == RackMenu.RACK_FACING_OFFSET ? facing.ordinal() + 1 : 0;
            }

            @Override
            public void set(final int index, final int value) {
            }

            @Override
            public int getCount() {
                return RackMenu.RACK_DATA_COUNT;
            }
        };
    }

    private static ContainerData rackDataWithRelayState(final boolean enabled) {
        return new ContainerData() {
            @Override
            public int get(final int index) {
                return index == RackMenu.RACK_RELAY_OFFSET && enabled ? 1 : 0;
            }

            @Override
            public void set(final int index, final int value) {
            }

            @Override
            public int getCount() {
                return RackMenu.RACK_DATA_COUNT;
            }
        };
    }

    private static final class TestRackBlockEntity extends RackBlockEntity {
        private Direction facing = Direction.NORTH;

        private TestRackBlockEntity() {
            super(null, (BlockState) null);
        }

        @Override
        public Direction facing() {
            return facing;
        }
    }

    private static final class TestRackMountable implements RackMountable {
        private final Node node = Network.newNode(this, Visibility.Network).create();
        private final RackBusConnectable[] connectables;

        private TestRackMountable(final RackBusConnectable... connectables) {
            this.connectables = connectables;
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
    }

    private static final class TestRackBusConnectable implements RackBusConnectable {
        private final Node node = Network.newNode(this, Visibility.Network).create();

        @Override public void receivePacket(final li.cil.oc.api.network.Packet packet) {}
        @Override public Node node() { return node; }
        @Override public void onConnect(final Node node) {}
        @Override public void onDisconnect(final Node node) {}
        @Override public void onMessage(final Message message) {}
    }
}
