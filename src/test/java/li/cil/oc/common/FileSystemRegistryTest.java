package li.cil.oc.common;

import li.cil.oc.api.API;
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

        CompoundTag nbt = new CompoundTag();
        environment.save(nbt);
        assertTrue(nbt.contains("node"));
    }
}
