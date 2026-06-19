package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.api.fs.Handle;
import li.cil.oc.api.fs.Mode;
import net.minecraft.nbt.CompoundTag;
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
    void readOnlyWrapperRejectsWrites() throws IOException {
        FileSystemRegistry registry = new FileSystemRegistry();
        FileSystem fileSystem = registry.fromMemory(256);
        fileSystem.makeDirectory("tmp");

        FileSystem readOnly = registry.asReadOnly(fileSystem);

        assertTrue(readOnly.isReadOnly());
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
}
