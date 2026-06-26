package li.cil.oc.common.component;

import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.item.Slot;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ServerRackMountableEnvironmentShapeTest {
    @Test
    void exposesRackControlPowerEntryPoint() throws NoSuchMethodException {
        final Method method = ServerRackMountableEnvironment.class.getMethod("controlPower", int.class);

        assertEquals(boolean.class, method.getReturnType());
    }

    @Test
    void exposesTierAwareSlotMetadata() {
        assertEquals(17, ServerRackMountableEnvironment.maxSlotCount());
        assertEquals(9, ServerRackMountableEnvironment.slotCountForTier(0));
        assertEquals(13, ServerRackMountableEnvironment.slotCountForTier(1));
        assertEquals(17, ServerRackMountableEnvironment.slotCountForTier(2));

        assertEquals(Slot.CPU, ServerRackMountableEnvironment.slotTypeName(1, 2));
        assertEquals(Slot.ComponentBus, ServerRackMountableEnvironment.slotTypeName(1, 3));
        assertEquals(Slot.Memory, ServerRackMountableEnvironment.slotTypeName(1, 5));
        assertEquals(Slot.HDD, ServerRackMountableEnvironment.slotTypeName(1, 8));
        assertEquals(ServerRackMountableEnvironment.SLOT_TYPE_EEPROM, ServerRackMountableEnvironment.slotTypeName(1, 12));
        assertEquals(Slot.None, ServerRackMountableEnvironment.slotTypeName(1, 13));
    }

    @Test
    void terminalServerExposesUpstreamDeviceInfoMetadata() {
        TerminalServerRackMountableEnvironment terminalServer = new TerminalServerRackMountableEnvironment();

        Map<String, String> metadata = terminalServer.getDeviceInfo();

        assertEquals(DeviceInfo.DeviceClass.Generic, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Terminal server", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MightyPirates GmbH & Co. KG", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
        assertEquals("RemoteViewing EX", metadata.get(DeviceInfo.DeviceAttribute.Product));
    }

    @Test
    void diskDriveMountableExposesUpstreamDeviceInfoMetadata() throws Exception {
        DiskDriveMountableEnvironment diskDrive = allocateDiskDriveMountable();

        Map<String, String> metadata = diskDrive.getDeviceInfo();

        assertEquals(DeviceInfo.DeviceClass.Disk, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Floppy disk drive", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MightyPirates GmbH & Co. KG", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
        assertEquals("RackDrive 100 Rev. 2", metadata.get(DeviceInfo.DeviceAttribute.Product));
    }

    @Test
    void diskDriveMountableReportsNoStateLikeUpstream() throws Exception {
        DiskDriveMountableEnvironment diskDrive = allocateDiskDriveMountable();

        assertTrue(diskDrive.getCurrentState().isEmpty());
    }

    @Test
    void diskDriveMountableDataMatchesUpstreamPayloadShape() throws Exception {
        DiskDriveMountableEnvironment diskDrive = allocateDiskDriveMountable();

        CompoundTag data = diskDrive.getData();

        assertEquals(0L, data.getLong("lastAccess"));
        assertTrue(data.contains("disk"));
        assertFalse(data.contains("kind"));
    }

    @Test
    void diskDriveMountableRecordsFilesystemAccessLikeUpstream() throws Exception {
        DiskDriveMountableEnvironment diskDrive = allocateDiskDriveMountable();
        TestManagedEnvironment filesystem = new TestManagedEnvironment();
        setField(diskDrive, DiskDriveMountableEnvironment.class, "diskEnvironment", filesystem);

        assertTrue(diskDrive.recordFileSystemAccess(filesystem.node(), 1234L));
        assertEquals(1234L, diskDrive.getData().getLong("lastAccess"));
    }

    @Test
    void terminalServerAnalyzeReturnsVirtualScreenAndKeyboardNodesLikeUpstream() {
        final TerminalServerRackMountableEnvironment terminalServer = new TerminalServerRackMountableEnvironment();
        final Analyzable analyzable = assertInstanceOf(Analyzable.class, terminalServer);

        final String[] componentNames = Arrays.stream(analyzable.onAnalyze(null, Direction.NORTH, 0.5F, 0.875F, 0.5F))
            .map(Component.class::cast)
            .map(Component::name)
            .toArray(String[]::new);

        assertArrayEquals(new String[]{"screen", "keyboard"}, componentNames);
    }

    @Test
    void serverExposesUpstreamDeviceInfoMetadata() throws Exception {
        ServerRackMountableEnvironment server = allocateServer(1);

        Map<String, String> metadata = server.getDeviceInfo();

        assertEquals(DeviceInfo.DeviceClass.System, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Server", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MightyPirates GmbH & Co. KG", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
        assertEquals("Blader", metadata.get(DeviceInfo.DeviceAttribute.Product));
        assertEquals("13", metadata.get(DeviceInfo.DeviceAttribute.Capacity));
    }

    @Test
    void serverDataMatchesUpstreamPayloadShape() throws Exception {
        ServerRackMountableEnvironment server = allocateServer(1);

        CompoundTag data = server.getData();

        assertFalse(data.getBoolean("isRunning"));
        assertFalse(data.getBoolean("hasErrored"));
        assertEquals(0L, data.getLong("lastFileSystemAccess"));
        assertEquals(0L, data.getLong("lastNetworkActivity"));
        assertFalse(data.contains("kind"));
        assertFalse(data.contains("tier"));
    }

    @Test
    void serverRecordsFilesystemAccessLikeUpstream() throws Exception {
        ServerRackMountableEnvironment server = allocateServer(1);
        Node filesystem = new TestNode("filesystem-address");
        setField(server, ServerRackMountableEnvironment.class, "componentSlots", new HashMap<>(Map.of(filesystem.address(), 2)));

        assertTrue(server.recordFileSystemAccess(filesystem, 2468L));
        assertEquals(2468L, server.getData().getLong("lastFileSystemAccess"));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ServerRackMountableEnvironment allocateServer(final int tier) throws Exception {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        final Unsafe unsafe = (Unsafe) unsafeField.get(null);
        final ServerRackMountableEnvironment server = (ServerRackMountableEnvironment) unsafe.allocateInstance(ServerRackMountableEnvironment.class);
        final NonNullList items = NonNullList.create();
        for (int index = 0; index < ServerRackMountableEnvironment.slotCountForTier(tier); index++) {
            items.add(new Object());
        }
        final Field tierField = ServerRackMountableEnvironment.class.getDeclaredField("tier");
        final Field itemsField = ServerRackMountableEnvironment.class.getDeclaredField("items");
        final Field componentSlotsField = ServerRackMountableEnvironment.class.getDeclaredField("componentSlots");
        unsafe.putInt(server, unsafe.objectFieldOffset(tierField), tier);
        unsafe.putObject(server, unsafe.objectFieldOffset(itemsField), items);
        unsafe.putObject(server, unsafe.objectFieldOffset(componentSlotsField), new HashMap<String, Integer>());
        return server;
    }

    private static DiskDriveMountableEnvironment allocateDiskDriveMountable() throws Exception {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (DiskDriveMountableEnvironment) ((Unsafe) unsafeField.get(null)).allocateInstance(DiskDriveMountableEnvironment.class);
    }

    private static void setField(final Object target, final Class<?> owner, final String name, final Object value) throws Exception {
        final Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
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

    private record TestNode(String address) implements Node {
        @Override public li.cil.oc.api.network.Environment host() { return null; }
        @Override public Visibility reachability() { return Visibility.Network; }
        @Override public li.cil.oc.api.network.Network network() { return null; }
        @Override public boolean isNeighborOf(final Node other) { return false; }
        @Override public boolean canBeReachedFrom(final Node other) { return false; }
        @Override public Iterable<Node> neighbors() { return java.util.List.of(); }
        @Override public Iterable<Node> reachableNodes() { return java.util.List.of(); }
        @Override public void connect(final Node node) {}
        @Override public void disconnect(final Node node) {}
        @Override public void remove() {}
        @Override public void sendToAddress(final String target, final String name, final Object... data) {}
        @Override public void sendToNeighbors(final String name, final Object... data) {}
        @Override public void sendToReachable(final String name, final Object... data) {}
        @Override public void sendToVisible(final String name, final Object... data) {}
        @Override public void load(final CompoundTag nbt) {}
        @Override public void save(final CompoundTag nbt) {}
    }
}
