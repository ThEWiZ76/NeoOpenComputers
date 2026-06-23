package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
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
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
    @TempDir
    private Path tempDir;

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
        FileSystem fileSystem = new FileSystemRegistry().fromMemory(4096);
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
    void memoryFileSystemChargesFileCostForEntriesAndBytes() throws IOException {
        FileSystem fileSystem = new FileSystemRegistry().fromMemory(4096);

        assertEquals(ModSettings.fileCost(), fileSystem.spaceUsed());
        assertTrue(fileSystem.makeDirectory("tmp"));
        assertEquals(ModSettings.fileCost() * 2L, fileSystem.spaceUsed());

        int outputHandle = fileSystem.open("tmp/data.txt", Mode.Write);
        assertEquals(ModSettings.fileCost() * 3L, fileSystem.spaceUsed());
        fileSystem.getHandle(outputHandle).write("hello".getBytes(StandardCharsets.UTF_8));
        fileSystem.getHandle(outputHandle).close();

        assertEquals(ModSettings.fileCost() * 3L + 5L, fileSystem.spaceUsed());
        assertEquals(5, fileSystem.size("tmp/data.txt"));
    }

    @Test
    void memoryFileSystemRequiresFileCostBudgetForNewEntries() throws IOException {
        FileSystem fileSystem = new FileSystemRegistry().fromMemory(ModSettings.fileCost() * 2L);

        assertTrue(fileSystem.makeDirectory("tmp"));

        IOException error = assertThrows(IOException.class, () -> fileSystem.open("data.txt", Mode.Write));
        assertEquals("not enough space", error.getMessage());
    }

    @Test
    void memoryFileSystemAllowsOverwritingExistingBytesAtCapacity() throws IOException {
        FileSystem fileSystem = new FileSystemRegistry().fromMemory(ModSettings.fileCost() * 2L + 4L);

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
    void memoryFileSystemRestoresOutputHandlePosition() throws IOException {
        FileSystem fileSystem = new FileSystemRegistry().fromMemory(ModSettings.fileCost() * 2L + 4L);
        int outputHandle = fileSystem.open("data.txt", Mode.Write);
        Handle output = fileSystem.getHandle(outputHandle);
        output.write("abcd".getBytes(StandardCharsets.UTF_8));
        output.seek(1);
        CompoundTag nbt = new CompoundTag();
        fileSystem.save(nbt);

        FileSystem loaded = new FileSystemRegistry().fromMemory(ModSettings.fileCost() * 2L + 4L);
        loaded.load(nbt);
        loaded.getHandle(outputHandle).write("Z".getBytes(StandardCharsets.UTF_8));
        loaded.getHandle(outputHandle).close();

        int inputHandle = loaded.open("data.txt", Mode.Read);
        byte[] buffer = new byte[4];
        assertEquals(4, loaded.getHandle(inputHandle).read(buffer));
        assertArrayEquals("aZcd".getBytes(StandardCharsets.UTF_8), buffer);
    }

    @Test
    void memoryFileSystemClosePreservesStoredFiles() throws IOException {
        FileSystem fileSystem = new FileSystemRegistry().fromMemory(4096);
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
        FileSystem fileSystem = registry.fromMemory(4096);
        fileSystem.makeDirectory("tmp");

        FileSystem readOnly = registry.asReadOnly(fileSystem);

        assertTrue(readOnly.isReadOnly());
        assertEquals(4096, readOnly.spaceTotal());
        assertEquals(ModSettings.fileCost() * 2L, readOnly.spaceUsed());
        assertFalse(readOnly.makeDirectory("other"));
        assertThrows(FileNotFoundException.class, () -> readOnly.open("tmp/data.txt", Mode.Write));
    }

    @Test
    void deferredFileSystemSourcesReturnNull() {
        FileSystemRegistry registry = new FileSystemRegistry();

        assertNull(registry.asManagedEnvironment(null, "label", null, null, 1));
    }

    @Test
    void classpathFileSystemReadsBundledResources() throws IOException {
        FileSystem fileSystem = new FileSystemRegistry().fromClass(getClass(), "neoopencomputers", "classpathfs");

        assertNotNull(fileSystem);
        assertTrue(fileSystem.isReadOnly());
        assertTrue(fileSystem.exists("data.txt"));
        assertTrue(fileSystem.isDirectory("sub"));
        assertArrayEquals(new String[]{"data.txt", "sub/"}, fileSystem.list(""));
        assertThrows(FileNotFoundException.class, () -> fileSystem.open("data.txt", Mode.Write));

        int inputHandle = fileSystem.open("data.txt", Mode.Read);
        byte[] buffer = new byte[64];
        int read = fileSystem.getHandle(inputHandle).read(buffer);
        fileSystem.getHandle(inputHandle).close();

        assertEquals("hello resource\n", new String(buffer, 0, read, StandardCharsets.UTF_8));
    }

    @Test
    void classpathFileSystemRejectsPathEscape() {
        FileSystem fileSystem = new FileSystemRegistry().fromClass(getClass(), "neoopencomputers", "classpathfs");

        assertNotNull(fileSystem);
        assertThrows(IllegalArgumentException.class, () -> fileSystem.exists("../outside.txt"));
    }

    @Test
    void saveDirectoryFileSystemPersistsFilesOnDisk() throws IOException {
        FileSystemRegistry registry = new FileSystemRegistry();
        FileSystem fileSystem = registry.fromSaveDirectory(tempDir.toString(), 128, true);

        assertNotNull(fileSystem);
        assertFalse(fileSystem.isReadOnly());
        assertEquals(128, fileSystem.spaceTotal());
        assertTrue(fileSystem.makeDirectory("tmp"));
        int outputHandle = fileSystem.open("tmp/data.txt", Mode.Write);
        fileSystem.getHandle(outputHandle).write("hello".getBytes(StandardCharsets.UTF_8));
        fileSystem.getHandle(outputHandle).close();

        assertEquals("hello", Files.readString(tempDir.resolve("tmp").resolve("data.txt")));
        FileSystem reloaded = registry.fromSaveDirectory(tempDir.toString(), 128, true);
        int inputHandle = reloaded.open("tmp/data.txt", Mode.Read);
        byte[] buffer = new byte[5];
        assertEquals(5, reloaded.getHandle(inputHandle).read(buffer));
        reloaded.getHandle(inputHandle).close();
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), buffer);
    }

    @Test
    void saveDirectoryFileSystemRejectsPathEscape() {
        FileSystem fileSystem = new FileSystemRegistry().fromSaveDirectory(tempDir.toString(), 128, true);

        assertThrows(IllegalArgumentException.class, () -> fileSystem.exists("../outside.txt"));
        assertThrows(IllegalArgumentException.class, () -> fileSystem.open("tmp/../outside.txt", Mode.Write));
    }

    @Test
    void createsManagedFileSystemEnvironment() {
        OpenComputersApi.initialize();
        FileSystem fileSystem = API.fileSystem.fromMemory(4096);

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
        assertEquals("80/80/40", metadata.get(DeviceInfo.DeviceAttribute.Clock));
    }

    @Test
    void managedFileSystemEnvironmentExposesBasicCallbacks() throws Exception {
        OpenComputersApi.initialize();
        FileSystem fileSystem = API.fileSystem.fromMemory(4096);
        assertTrue(fileSystem.makeDirectory("tmp"));
        ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(fileSystem, "tmp", null, null, 1);
        Component component = (Component) environment.node();

        assertArrayEquals(new Object[]{false}, component.invoke("isReadOnly", null));
        assertArrayEquals(new Object[]{4096L}, component.invoke("spaceTotal", null));
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
    void managedFileSystemEnvironmentRejectsEscapingPathsLikeUpstream() {
        OpenComputersApi.initialize();
        FileSystem fileSystem = API.fileSystem.fromMemory(256);
        ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(fileSystem, "tmp", null, null, 1);
        Component component = (Component) environment.node();

        FileNotFoundException error = assertThrows(FileNotFoundException.class, () -> component.invoke("exists", null, "../secret"));
        assertEquals("../secret", error.getMessage());
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
        FileSystem fileSystem = API.fileSystem.fromMemory(4096);
        assertTrue(fileSystem.makeDirectory("tmp"));
        ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(fileSystem, "tmp", null, null, 1);
        Component component = (Component) environment.node();
        charge(environment, 1D);

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
    void managedFileSystemEnvironmentAcceptsHandleTables() throws Exception {
        OpenComputersApi.initialize();
        FileSystem fileSystem = API.fileSystem.fromMemory(4096);
        ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(fileSystem, "tmp", null, null, 1);
        Component component = (Component) environment.node();
        charge(environment, 1D);

        Object handle = component.invoke("open", null, "data.txt", "w")[0];
        Map<String, Object> handleTable = Map.of("handle", Integer.parseInt(handle.toString()));

        assertArrayEquals(new Object[]{true}, component.invoke("write", null, handleTable, "hello"));
        component.invoke("close", null, handleTable);

        Object readHandle = component.invoke("open", null, "data.txt", "r")[0];
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), (byte[]) component.invoke("read", null, readHandle, 5)[0]);
    }

    @Test
    void managedFileSystemEnvironmentConsumesCallBudgetForIo() throws Exception {
        OpenComputersApi.initialize();
        FileSystem fileSystem = API.fileSystem.fromMemory(4096);
        assertTrue(fileSystem.makeDirectory("tmp"));
        ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(fileSystem, "tmp", null, null, 2);
        Component component = (Component) environment.node();
        charge(environment, 1D);
        RecordingContext context = new RecordingContext();

        Object writeHandle = component.invoke("open", context, "tmp/data.txt", "w")[0];
        component.invoke("write", context, writeHandle, "hello");
        component.invoke("close", context, writeHandle);
        Object readHandle = component.invoke("open", context, "tmp/data.txt", "r")[0];
        component.invoke("read", context, readHandle, 2);
        component.invoke("seek", context, readHandle, "set", 0);

        assertEquals((1.0D / 3.0D) + (1.0D / 7.0D) + (1.0D / 7.0D), context.callBudget, 0.000_001D);
    }

    @Test
    void managedFileSystemEnvironmentConsumesEnergyForIo() throws Exception {
        OpenComputersApi.initialize();
        FileSystem fileSystem = API.fileSystem.fromMemory(4096);
        ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(fileSystem, "tmp", null, null, 1);
        Component component = (Component) environment.node();
        Connector connector = charge(environment, 1D);
        byte[] data = new byte[1024];
        java.util.Arrays.fill(data, (byte) 'x');

        Object writeHandle = component.invoke("open", null, "data.txt", "w")[0];
        assertArrayEquals(new Object[]{true}, component.invoke("write", null, writeHandle, data));
        component.invoke("close", null, writeHandle);
        Object readHandle = component.invoke("open", null, "data.txt", "r")[0];
        component.invoke("read", null, readHandle, data.length);

        assertEquals(0.65D, connector.localBuffer(), 0.000_001D);
    }

    @Test
    void managedFileSystemEnvironmentConsumesOwnConnectorEnergyLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        FileSystem fileSystem = API.fileSystem.fromMemory(4096);
        ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(fileSystem, "tmp", null, null, 1);
        Component component = (Component) environment.node();
        Connector fileSystemConnector = assertInstanceOf(Connector.class, environment.node());
        RecordingEnvironment contextEnvironment = new RecordingEnvironment();
        Connector contextConnector = (Connector) API.network.newNode(contextEnvironment, Visibility.Network).withConnector(1).create();
        contextEnvironment.node = contextConnector;
        fileSystemConnector.setLocalBufferSize(1);
        fileSystemConnector.changeBuffer(1);
        RecordingContext context = new RecordingContext(contextConnector);
        byte[] data = new byte[1024];
        java.util.Arrays.fill(data, (byte) 'x');

        Object writeHandle = component.invoke("open", context, "data.txt", "w")[0];
        assertArrayEquals(new Object[]{true}, component.invoke("write", context, writeHandle, data));
        component.invoke("close", context, writeHandle);
        Object readHandle = component.invoke("open", context, "data.txt", "r")[0];
        component.invoke("read", context, readHandle, data.length);

        assertEquals(0.65D, fileSystemConnector.localBuffer(), 0.000_001D);
        assertEquals(0D, contextConnector.localBuffer(), 0.000_001D);
    }

    @Test
    void managedFileSystemEnvironmentUsesConfiguredHddEnergyCosts() throws Exception {
        withCachedConfig(ModSettings.HDD_READ, 0.5D, () -> {
            withCachedConfig(ModSettings.HDD_WRITE, 0.75D, () -> {
                OpenComputersApi.initialize();
                FileSystem fileSystem = API.fileSystem.fromMemory(4096);
                ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(fileSystem, "tmp", null, null, 1);
                Component component = (Component) environment.node();
                Connector connector = charge(environment, 2D);
                byte[] data = new byte[1024];
                java.util.Arrays.fill(data, (byte) 'x');

                Object writeHandle = component.invoke("open", null, "data.txt", "w")[0];
                assertArrayEquals(new Object[]{true}, component.invoke("write", null, writeHandle, data));
                component.invoke("close", null, writeHandle);
                Object readHandle = component.invoke("open", null, "data.txt", "r")[0];
                component.invoke("read", null, readHandle, data.length);

                assertEquals(0.75D, connector.localBuffer(), 0.000_001D);
                return null;
            });
            return null;
        });
    }

    @Test
    void managedFileSystemEnvironmentRejectsHandlesOwnedByAnotherContext() throws Exception {
        OpenComputersApi.initialize();
        FileSystem fileSystem = API.fileSystem.fromMemory(4096);
        ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(fileSystem, "tmp", null, null, 1);
        Component component = (Component) environment.node();
        charge(environment, 1D);
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
        withCachedConfig(ModSettings.MAX_HANDLES, 2, () -> {
            assertOpenHandleLimit(2);
            return null;
        });
    }

    private static void assertOpenHandleLimit(final int limit) throws Exception {
        OpenComputersApi.initialize();
        FileSystem fileSystem = API.fileSystem.fromMemory(4096);
        ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(fileSystem, "tmp", null, null, 1);
        Component component = (Component) environment.node();
        RecordingContext context = new RecordingContext("owner");
        Object firstHandle = null;

        for (int index = 0; index < limit; index++) {
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
        ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(API.fileSystem.fromMemory(4096), "tmp", null, null, 1);
        Component component = (Component) environment.node();
        Object handle = component.invoke("open", context, "data.txt", "w")[0];
        int rawHandle = Integer.parseInt(handle.toString());
        CompoundTag nbt = new CompoundTag();
        environment.save(nbt);
        ListTag owners = nbt.getList("owners", Tag.TAG_COMPOUND);
        assertEquals(1, owners.size());
        assertEquals("owner", owners.getCompound(0).getString("address"));
        assertArrayEquals(new int[]{rawHandle}, owners.getCompound(0).getIntArray("handles"));

        ManagedEnvironment loaded = API.fileSystem.asManagedEnvironment(API.fileSystem.fromMemory(4096), "tmp", null, null, 1);
        loaded.load(nbt);
        Component loadedComponent = (Component) loaded.node();
        charge(loaded, 1D);

        assertArrayEquals(new Object[]{true}, loadedComponent.invoke("write", context, rawHandle, "after reload"));
        loadedComponent.invoke("close", context, rawHandle);
    }

    @Test
    void managedFileSystemEnvironmentSaveKeepsLiveHandlesOpen() throws Exception {
        OpenComputersApi.initialize();
        ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(API.fileSystem.fromMemory(4096), "tmp", null, null, 1);
        Component component = (Component) environment.node();
        charge(environment, 1D);
        Object handle = component.invoke("open", null, "data.txt", "w")[0];

        environment.save(new CompoundTag());

        assertArrayEquals(new Object[]{true}, component.invoke("write", null, handle, "still open"));
        component.invoke("close", null, handle);
    }

    @Test
    void managedFileSystemEnvironmentCapsReadBufferSize() throws Exception {
        withCachedConfig(ModSettings.MAX_READ_BUFFER, 32, () -> {
            assertReadBufferCap(32);
            return null;
        });
    }

    private static void assertReadBufferCap(final int maxReadBuffer) throws Exception {
        OpenComputersApi.initialize();
        FileSystem fileSystem = API.fileSystem.fromMemory(4096);
        ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(fileSystem, "tmp", null, null, 1);
        Component component = (Component) environment.node();
        charge(environment, 1D);
        byte[] data = new byte[maxReadBuffer + 2];
        java.util.Arrays.fill(data, (byte) 'x');

        Object writeHandle = component.invoke("open", null, "data.txt", "w")[0];
        assertArrayEquals(new Object[]{true}, component.invoke("write", null, writeHandle, data));
        component.invoke("close", null, writeHandle);
        Object readHandle = component.invoke("open", null, "data.txt", "r")[0];

        byte[] read = (byte[]) component.invoke("read", null, readHandle, 4096)[0];

        assertEquals(maxReadBuffer, read.length);
    }

    private static Connector charge(final ManagedEnvironment environment, final double energy) {
        Connector connector = assertInstanceOf(Connector.class, environment.node());
        connector.setLocalBufferSize(energy);
        connector.changeBuffer(energy);
        return connector;
    }

    private static <T, V> T withCachedConfig(final ModConfigSpec.ConfigValue<V> value, final V override, final ThrowingSupplier<T> action) throws Exception {
        final Field cachedValue = ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
        cachedValue.setAccessible(true);
        final Object previous = cachedValue.get(value);
        cachedValue.set(value, override);
        try {
            return action.get();
        } finally {
            cachedValue.set(value, previous);
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
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
            this((Node) null);
        }

        private RecordingContext(final String address) {
            this(address == null ? null : TestNodes.node(address));
        }

        private RecordingContext(final Node node) {
            this.node = node;
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

    private static final class RecordingEnvironment implements Environment {
        private Node node;

        @Override
        public Node node() {
            return node;
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
}
