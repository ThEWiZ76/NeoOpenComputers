package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.api.fs.Handle;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.fs.Mode;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.TestNodes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FileSystemRegistryTest {
    @AfterEach
    void resetApi() {
        API.fileSystem = null;
        API.network = null;
    }

    @Test
    void bootstrapInstallsFileSystemApi() {
        OpenComputersApi.initialize();

        assertInstanceOf(FileSystemRegistry.class, API.fileSystem);
    }

    @Test
    void memoryFileSystemReadsWrittenFiles() throws IOException {
        FileSystem fileSystem = new FileSystemRegistry().fromMemory(256);
        assertTrue(fileSystem.makeDirectory("tmp"));

        int outputHandle = fileSystem.open("tmp/data.txt", Mode.Write);
        fileSystem.getHandle(outputHandle).write("hello".getBytes(StandardCharsets.UTF_8));
        fileSystem.getHandle(outputHandle).close();

        int inputHandle = fileSystem.open("tmp/data.txt", Mode.Read);
        byte[] buffer = new byte[5];
        Handle input = fileSystem.getHandle(inputHandle);
        int read = input.read(buffer);

        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), buffer);
        assertTrue(read > 0);
    }

    @Test
    void memoryFileSystemAllowsOverwritingExistingBytesAtCapacity() throws IOException {
        FileSystem fileSystem = new FileSystemRegistry().fromMemory(4);

        int outputHandle = fileSystem.open("data.txt", Mode.Write);
        Handle output = fileSystem.getHandle(outputHandle);
        output.write("abcd".getBytes(StandardCharsets.UTF_8));
        output.seek(0);

        output.write("WXYZ".getBytes(StandardCharsets.UTF_8));
        output.close();

        int inputHandle = fileSystem.open("data.txt", Mode.Read);
        byte[] buffer = new byte[4];
        assertEquals(4, fileSystem.getHandle(inputHandle).read(buffer));
        assertArrayEquals("WXYZ".getBytes(StandardCharsets.UTF_8), buffer);
    }

    @Test
    void memoryFileSystemClosePreservesStoredFiles() throws IOException {
        FileSystem fileSystem = new FileSystemRegistry().fromMemory(256);
        assertTrue(fileSystem.makeDirectory("tmp"));
        int outputHandle = fileSystem.open("tmp/data.txt", Mode.Write);
        fileSystem.getHandle(outputHandle).write("hello".getBytes(StandardCharsets.UTF_8));

        fileSystem.close();

        assertTrue(fileSystem.exists("tmp/data.txt"));
        int inputHandle = fileSystem.open("tmp/data.txt", Mode.Read);
        byte[] buffer = new byte[5];
        assertEquals(5, fileSystem.getHandle(inputHandle).read(buffer));
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), buffer);
    }

    @Test
    void readOnlyWrapperRejectsWrites() throws IOException {
        FileSystemRegistry registry = new FileSystemRegistry();
        FileSystem fileSystem = registry.fromMemory(256);
        fileSystem.makeDirectory("tmp");

        FileSystem readOnly = registry.asReadOnly(fileSystem);

        assertTrue(readOnly.isReadOnly());
        assertEquals(256, readOnly.spaceTotal());
        assertEquals(0, readOnly.spaceUsed());
        assertFalse(readOnly.makeDirectory("other"));
        assertThrows(FileNotFoundException.class, () -> readOnly.open("tmp/data.txt", Mode.Write));
    }

    @Test
    void deferredFileSystemSourcesReturnNull() {
        FileSystemRegistry registry = new FileSystemRegistry();

        assertNull(registry.fromClass(getClass(), "neoopencomputers", "manual"));
        assertNull(registry.fromSaveDirectory("drive", 1024, true));
        assertNull(registry.asManagedEnvironment(null, "label", null, null, 1));
    }

    @Test
    void createsManagedFileSystemEnvironment() {
        OpenComputersApi.initialize();
        FileSystem fileSystem = API.fileSystem.fromMemory(256);

        ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(fileSystem, "tmp", null, null, 1);

        assertNotNull(environment);
        assertNotNull(environment.node());
        assertInstanceOf(Component.class, environment.node());
        Component component = (Component) environment.node();
        assertEquals("filesystem", component.name());
        assertEquals(Visibility.Neighbors, component.visibility());
        assertTrue(component.methods().contains("isReadOnly"));

        CompoundTag nbt = new CompoundTag();
        environment.save(nbt);
        assertTrue(nbt.contains("node"));
    }

    @Test
    void managedFileSystemEnvironmentExposesDeviceInfo() {
        OpenComputersApi.initialize();
        FileSystem fileSystem = API.fileSystem.fromMemory(256);

        ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(fileSystem, "tmp", null, null, 1);

        DeviceInfo info = assertInstanceOf(DeviceInfo.class, environment);
        Map<String, String> metadata = info.getDeviceInfo();
        assertEquals(DeviceInfo.DeviceClass.Volume, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Filesystem", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MPFS.21.6", metadata.get(DeviceInfo.DeviceAttribute.Product));
        assertEquals("262", metadata.get(DeviceInfo.DeviceAttribute.Capacity));
        assertEquals("256", metadata.get(DeviceInfo.DeviceAttribute.Size));
        assertEquals("20/20/20", metadata.get(DeviceInfo.DeviceAttribute.Clock));
    }

    @Test
    void managedFileSystemEnvironmentExposesBasicCallbacks() throws Exception {
        OpenComputersApi.initialize();
        FileSystem fileSystem = API.fileSystem.fromMemory(256);
        assertTrue(fileSystem.makeDirectory("tmp"));
        ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(fileSystem, "tmp", null, null, 1);
        Component component = (Component) environment.node();

        assertArrayEquals(new Object[]{false}, component.invoke("isReadOnly", null));
        assertArrayEquals(new Object[]{256L}, component.invoke("spaceTotal", null));
        assertArrayEquals(new Object[]{true}, component.invoke("exists", null, "tmp"));
        assertArrayEquals(new Object[]{0L}, component.invoke("size", null, "tmp"));
        assertArrayEquals(new Object[]{true}, component.invoke("isDirectory", null, "tmp"));
        assertArrayEquals(new Object[]{true}, component.invoke("makeDirectory", null, "tmp/nested/child"));
        assertArrayEquals(new String[]{"nested/"}, (String[]) component.invoke("list", null, "tmp")[0]);
        assertArrayEquals(new Object[]{true}, component.invoke("rename", null, "tmp/nested", "tmp/renamed"));
        assertArrayEquals(new Object[]{true}, component.invoke("remove", null, "tmp/renamed"));
        assertArrayEquals(new Object[]{false}, component.invoke("exists", null, "tmp/renamed"));
    }

    @Test
    void managedFileSystemEnvironmentReturnsNoResultsWhenListingInvalidPath() throws Exception {
        OpenComputersApi.initialize();
        FileSystem fileSystem = API.fileSystem.fromMemory(256);
        ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(fileSystem, "tmp", null, null, 1);
        Component component = (Component) environment.node();

        assertNull(component.invoke("list", null, "missing"));
    }

    @Test
    void managedFileSystemEnvironmentReportsInfiniteCapacityForUnlimitedFileSystems() throws Exception {
        OpenComputersApi.initialize();
        FileSystem fileSystem = API.fileSystem.fromMemory(-1);
        ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(fileSystem, "tmp", null, null, 1);
        Component component = (Component) environment.node();

        assertArrayEquals(new Object[]{Double.POSITIVE_INFINITY}, component.invoke("spaceTotal", null));
    }

    @Test
    void managedFileSystemEnvironmentExposesLabelCallbacks() throws Exception {
        OpenComputersApi.initialize();
        FileSystem fileSystem = API.fileSystem.fromMemory(256);
        MutableLabel label = new MutableLabel("tmp");
        ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(fileSystem, label, null, null, 1);
        Component component = (Component) environment.node();

        assertArrayEquals(new Object[]{"tmp"}, component.invoke("getLabel", null));
        assertArrayEquals(new Object[]{"data"}, component.invoke("setLabel", null, "data"));
        assertEquals("data", label.getLabel());
        assertArrayEquals(new Object[]{null}, component.invoke("setLabel", null, new Object[]{null}));
        assertNull(label.getLabel());
    }

    @Test
    void managedFileSystemEnvironmentReadsAndWritesFiles() throws Exception {
        OpenComputersApi.initialize();
        FileSystem fileSystem = API.fileSystem.fromMemory(256);
        assertTrue(fileSystem.makeDirectory("tmp"));
        ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(fileSystem, "tmp", null, null, 1);
        Component component = (Component) environment.node();

        Object writeHandle = component.invoke("open", null, "tmp/data.txt", "w")[0];
        assertArrayEquals(new Object[]{true}, component.invoke("write", null, writeHandle, "hello"));
        component.invoke("close", null, writeHandle);

        Object readHandle = component.invoke("open", null, "tmp/data.txt", "r")[0];
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), (byte[]) component.invoke("read", null, readHandle, 5)[0]);
        assertArrayEquals(new Object[]{0L}, component.invoke("seek", null, readHandle, "set", 0));
        assertArrayEquals("he".getBytes(StandardCharsets.UTF_8), (byte[]) component.invoke("read", null, readHandle, 2)[0]);
        component.invoke("close", null, readHandle);
    }

    @Test
    void managedFileSystemEnvironmentConsumesCallBudgetForIo() throws Exception {
        OpenComputersApi.initialize();
        FileSystem fileSystem = API.fileSystem.fromMemory(256);
        assertTrue(fileSystem.makeDirectory("tmp"));
        ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(fileSystem, "tmp", null, null, 2);
        Component component = (Component) environment.node();
        RecordingContext context = new RecordingContext();

        Object writeHandle = component.invoke("open", context, "tmp/data.txt", "w")[0];
        component.invoke("write", context, writeHandle, "hello");
        component.invoke("close", context, writeHandle);
        Object readHandle = component.invoke("open", context, "tmp/data.txt", "r")[0];
        component.invoke("read", context, readHandle, 2);
        component.invoke("seek", context, readHandle, "set", 0);

        assertEquals(1.0D, context.callBudget, 0.000_001D);
    }

    @Test
    void managedFileSystemEnvironmentRejectsHandlesOwnedByAnotherContext() throws Exception {
        OpenComputersApi.initialize();
        FileSystem fileSystem = API.fileSystem.fromMemory(256);
        ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(fileSystem, "tmp", null, null, 1);
        Component component = (Component) environment.node();
        RecordingContext owner = new RecordingContext("owner");
        RecordingContext stranger = new RecordingContext("stranger");

        Object handle = component.invoke("open", owner, "data.txt", "w")[0];
        int rawHandle = Integer.parseInt(handle.toString());

        IOException error = assertThrows(IOException.class, () -> component.invoke("close", stranger, rawHandle));
        assertEquals("bad file descriptor", error.getMessage());
        assertArrayEquals(new Object[]{true}, component.invoke("write", owner, handle, "ok"));
        component.invoke("close", owner, handle);
    }

    @Test
    void managedFileSystemEnvironmentLimitsOpenHandlesPerContext() throws Exception {
        OpenComputersApi.initialize();
        FileSystem fileSystem = API.fileSystem.fromMemory(256);
        ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(fileSystem, "tmp", null, null, 1);
        Component component = (Component) environment.node();
        RecordingContext context = new RecordingContext("owner");
        Object firstHandle = null;

        for (int index = 0; index < 16; index++) {
            Object handle = component.invoke("open", context, "data" + index + ".txt", "w")[0];
            if (index == 0) {
                firstHandle = handle;
            }
        }

        IOException error = assertThrows(IOException.class, () -> component.invoke("open", context, "overflow.txt", "w"));
        assertEquals("too many open handles", error.getMessage());
        component.invoke("close", context, firstHandle);
        assertNotNull(component.invoke("open", context, "overflow.txt", "w")[0]);
    }

    @Test
    void managedFileSystemEnvironmentPersistsHandleOwners() throws Exception {
        OpenComputersApi.initialize();
        RecordingContext context = new RecordingContext("owner");
        ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(API.fileSystem.fromMemory(256), "tmp", null, null, 1);
        Component component = (Component) environment.node();
        Object handle = component.invoke("open", context, "data.txt", "w")[0];
        int rawHandle = Integer.parseInt(handle.toString());
        CompoundTag nbt = new CompoundTag();
        environment.save(nbt);
        ListTag owners = nbt.getList("owners", Tag.TAG_COMPOUND);
        assertEquals(1, owners.size());
        assertEquals("owner", owners.getCompound(0).getString("address"));
        assertArrayEquals(new int[]{rawHandle}, owners.getCompound(0).getIntArray("handles"));

        ManagedEnvironment loaded = API.fileSystem.asManagedEnvironment(API.fileSystem.fromMemory(256), "tmp", null, null, 1);
        loaded.load(nbt);
        Component loadedComponent = (Component) loaded.node();

        assertArrayEquals(new Object[]{true}, loadedComponent.invoke("write", context, rawHandle, "after reload"));
        loadedComponent.invoke("close", context, rawHandle);
    }

    @Test
    void managedFileSystemEnvironmentSaveKeepsLiveHandlesOpen() throws Exception {
        OpenComputersApi.initialize();
        ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(API.fileSystem.fromMemory(256), "tmp", null, null, 1);
        Component component = (Component) environment.node();
        Object handle = component.invoke("open", null, "data.txt", "w")[0];

        environment.save(new CompoundTag());

        assertArrayEquals(new Object[]{true}, component.invoke("write", null, handle, "still open"));
        component.invoke("close", null, handle);
    }

    @Test
    void managedFileSystemEnvironmentCapsReadBufferSize() throws Exception {
        OpenComputersApi.initialize();
        FileSystem fileSystem = API.fileSystem.fromMemory(4096);
        ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(fileSystem, "tmp", null, null, 1);
        Component component = (Component) environment.node();
        byte[] data = new byte[2050];
        java.util.Arrays.fill(data, (byte) 'x');

        Object writeHandle = component.invoke("open", null, "data.txt", "w")[0];
        assertArrayEquals(new Object[]{true}, component.invoke("write", null, writeHandle, data));
        component.invoke("close", null, writeHandle);
        Object readHandle = component.invoke("open", null, "data.txt", "r")[0];

        byte[] read = (byte[]) component.invoke("read", null, readHandle, 4096)[0];

        assertEquals(2048, read.length);
    }

    private static final class MutableLabel implements Label {
        private String value;

        private MutableLabel(final String value) {
            this.value = value;
        }

        @Override
        public String getLabel() {
            return value;
        }

        @Override
        public void setLabel(final String value) {
            this.value = value;
        }

        @Override
        public void load(final CompoundTag nbt) {
            value = nbt.contains("label") ? nbt.getString("label") : null;
        }

        @Override
        public void save(final CompoundTag nbt) {
            if (value != null) {
                nbt.putString("label", value);
            }
        }
    }

    private static final class RecordingContext implements Context {
        private double callBudget;
        private final Node node;

        private RecordingContext() {
            this(null);
        }

        private RecordingContext(final String address) {
            this.node = address == null ? null : TestNodes.node(address);
        }

        @Override
        public Node node() {
            return node;
        }

        @Override
        public boolean canInteract(final String player) {
            return true;
        }

        @Override
        public boolean isRunning() {
            return true;
        }

        @Override
        public boolean isPaused() {
            return false;
        }

        @Override
        public boolean start() {
            return true;
        }

        @Override
        public boolean pause(final double seconds) {
            return true;
        }

        @Override
        public boolean stop() {
            return true;
        }

        @Override
        public void consumeCallBudget(final double callCost) {
            callBudget += callCost;
        }

        @Override
        public boolean signal(final String name, final Object... args) {
            return true;
        }
    }
}
