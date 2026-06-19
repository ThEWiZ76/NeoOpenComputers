package li.cil.oc.api.component;

import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.util.StateAware;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

final class RackComponentApiTest {
    @Test
    void rackBusConnectableReceivesPackets() {
        TestRackBusConnectable connectable = new TestRackBusConnectable();
        Packet packet = null;

        connectable.receivePacket(packet);

        assertSame(packet, connectable.packet);
    }

    @Test
    void rackMountableUsesModernActivationAndCompoundTagSignatures() throws NoSuchMethodException {
        Method getData = RackMountable.class.getMethod("getData");
        Method onActivate = RackMountable.class.getMethod("onActivate", Player.class, InteractionHand.class, ItemStack.class, float.class, float.class);
        TestRackMountable mountable = new TestRackMountable();

        assertEquals(CompoundTag.class, getData.getReturnType());
        assertArrayEquals(new Class<?>[]{Player.class, InteractionHand.class, ItemStack.class, float.class, float.class}, onActivate.getParameterTypes());
        assertInstanceOf(StateAware.class, mountable);
        assertEquals(EnumSet.of(StateAware.State.CanWork), mountable.getCurrentState());
        assertEquals("rack", mountable.getData().getString("kind"));
        assertEquals(1, mountable.getConnectableCount());
        assertNull(mountable.getConnectableAt(0));
        assertEquals(true, mountable.onActivate(null, InteractionHand.MAIN_HAND, null, 0.5F, 0.25F));
    }

    private static final class TestRackBusConnectable implements RackBusConnectable {
        private Packet packet;

        @Override
        public void receivePacket(final Packet packet) {
            this.packet = packet;
        }

        @Override
        public Node node() {
            return null;
        }

        @Override
        public void onConnect(final Node node) {
        }

        @Override
        public void onDisconnect(final Node node) {
        }

        @Override
        public void onMessage(final Message message) {
        }
    }

    private static final class TestRackMountable implements RackMountable {
        @Override
        public CompoundTag getData() {
            CompoundTag tag = new CompoundTag();
            tag.putString("kind", "rack");
            return tag;
        }

        @Override
        public int getConnectableCount() {
            return 1;
        }

        @Override
        public RackBusConnectable getConnectableAt(final int index) {
            return null;
        }

        @Override
        public boolean onActivate(final Player player, final InteractionHand hand, final ItemStack heldItem, final float hitX, final float hitY) {
            return hand == InteractionHand.MAIN_HAND;
        }

        @Override
        public EnumSet<State> getCurrentState() {
            return EnumSet.of(State.CanWork);
        }

        @Override
        public boolean canUpdate() {
            return false;
        }

        @Override
        public void update() {
        }

        @Override
        public Node node() {
            return null;
        }

        @Override
        public void onConnect(final Node node) {
        }

        @Override
        public void onDisconnect(final Node node) {
        }

        @Override
        public void onMessage(final Message message) {
        }

        @Override
        public void load(final CompoundTag nbt) {
        }

        @Override
        public void save(final CompoundTag nbt) {
        }
    }
}
