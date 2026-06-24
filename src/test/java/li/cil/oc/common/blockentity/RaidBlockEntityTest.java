package li.cil.oc.common.blockentity;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RaidBlockEntityTest {
    @Test
    void raidIsNetworkedMenuContainerHost() {
        assertTrue(ManagedEnvironment.class.isAssignableFrom(RaidBlockEntity.class));
        assertTrue(EnvironmentHost.class.isAssignableFrom(RaidBlockEntity.class));
        assertTrue(Container.class.isAssignableFrom(RaidBlockEntity.class));
        assertTrue(Analyzable.class.isAssignableFrom(RaidBlockEntity.class));
        assertFalse(DeviceInfo.class.isAssignableFrom(RaidBlockEntity.class));
        assertTrue(MenuProvider.class.isAssignableFrom(RaidBlockEntity.class));
    }

    @Test
    void raidAcceptsOnlyHardDiskSlots() {
        assertTrue(RaidBlockEntity.acceptsDriverSlot(Slot.HDD));
        assertFalse(RaidBlockEntity.acceptsDriverSlot(Slot.Floppy));
        assertFalse(RaidBlockEntity.acceptsDriverSlot(Slot.Card));
    }

    @Test
    void raidFilesystemBecomesNetworkVisibleLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        final RaidBlockEntity raid = allocateRaid();
        final Node raidNode = Network.newNode(raid, Visibility.None).create();
        final TestManagedEnvironment filesystem = new TestManagedEnvironment();
        setField(raid, "node", raidNode);
        setField(raid, "filesystem", filesystem);

        raid.onConnect(raidNode);

        final var component = assertInstanceOf(li.cil.oc.api.network.Component.class, filesystem.node());
        assertEquals(Visibility.Network, component.visibility());
    }

    @Test
    void onAnalyzeReturnsNullNodeArrayWhenRaidFilesystemMissingLikeUpstream() throws Exception {
        final RaidBlockEntity raid = allocateRaid();

        assertArrayEquals(new Node[]{null}, raid.onAnalyze(null, null, 0F, 0F, 0F));
    }

    private static RaidBlockEntity allocateRaid() throws Exception {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (RaidBlockEntity) ((Unsafe) unsafeField.get(null)).allocateInstance(RaidBlockEntity.class);
    }

    private static void setField(final RaidBlockEntity raid, final String name, final Object value) throws Exception {
        final Field field = RaidBlockEntity.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(raid, value);
    }

    private static final class TestManagedEnvironment implements ManagedEnvironment {
        private final Node node = Network.newNode(this, Visibility.Network)
            .withComponent("filesystem", Visibility.Neighbors)
            .create();

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
