package li.cil.oc.common.network;

import li.cil.oc.api.Network;
import li.cil.oc.api.component.RackBusConnectable;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.util.StateAware;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.blockentity.RackBlockEntity;
import li.cil.oc.common.menu.RackMenu;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.EnumSet;

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

    private static TestRackBlockEntity allocateRack() throws Exception {
        return (TestRackBlockEntity) unsafe().allocateInstance(TestRackBlockEntity.class);
    }

    private static RackMenu allocateMenu(final int containerId, final Container rackInventory) throws Exception {
        final RackMenu menu = (RackMenu) unsafe().allocateInstance(RackMenu.class);
        setField(menu, AbstractContainerMenu.class, "containerId", containerId);
        setField(menu, RackMenu.class, "rackInventory", rackInventory);
        return menu;
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
        private TestRackBlockEntity() {
            super(null, (BlockState) null);
        }

        @Override
        public Direction facing() {
            return Direction.NORTH;
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
