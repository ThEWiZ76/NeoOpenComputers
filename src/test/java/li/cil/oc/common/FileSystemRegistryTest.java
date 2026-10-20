package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.event.FileSystemAccessEvent;
import li.cil.oc.api.network.EnvironmentHost;
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
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
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
import java.util.ArrayList;
import java.util.List;
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
    void readOnlyWrapperRejectsPreviouslySavedWritableHandles() throws IOException {
        final var registry = new FileSystemRegistry();
        final var original = registry.fromMemory(4096);
        final int output = original.open("probe.bin", Mode.Write);
        original.getHandle(output).write(new byte[]{97, 0, (byte) 128, (byte) 255});
        original.getHandle(output).seek(1);
        final var saved = new net.minecraft.nbt.CompoundTag();
        original.save(saved); original.close();
        final var writable = registry.fromMemory(4096);
        final var locked = registry.asReadOnly(writable);
        locked.load(saved);
        final var restored = locked.getHandle(output);
        assertEquals(1, restored.position());
        assertThrows(IOException.class, () -> restored.write(new byte[]{42}));
        final int input = locked.open("probe.bin", Mode.Read);
        final byte[] bytes = new byte[4];
        assertEquals(4, locked.getHandle(input).read(bytes));
        assertArrayEquals(new byte[]{97, 0, (byte) 128, (byte) 255}, bytes);
        assertNull(locked.getHandle(Integer.MAX_VALUE));
        locked.close();
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
        FileSystemRegistry registry = new FileSystemRegistry(() -> tempDir);
        FileSystem fileSystem = registry.fromSaveDirectory("drive", 4096, false);

        assertNotNull(fileSystem);
        assertFalse(fileSystem.isReadOnly());
        assertEquals(4096, fileSystem.spaceTotal());
        assertTrue(fileSystem.makeDirectory("tmp"));
        int outputHandle = fileSystem.open("tmp/data.txt", Mode.Write);
        fileSystem.getHandle(outputHandle).write("hello".getBytes(StandardCharsets.UTF_8));
        fileSystem.getHandle(outputHandle).close();

        assertEquals("hello", Files.readString(tempDir.resolve("opencomputers/drive/tmp/data.txt")));
        FileSystem reloaded = registry.fromSaveDirectory("drive", 4096, false);
        int inputHandle = reloaded.open("tmp/data.txt", Mode.Read);
        byte[] buffer = new byte[5];
        assertEquals(5, reloaded.getHandle(inputHandle).read(buffer));
        reloaded.getHandle(inputHandle).close();
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), buffer);
    }

    @Test
    void saveDirectoryFileSystemRejectsPathEscape() {
        FileSystem fileSystem = new FileSystemRegistry(() -> tempDir).fromSaveDirectory("drive", 128, false);

        assertThrows(IllegalArgumentException.class, () -> fileSystem.exists("../outside.txt"));
        assertThrows(IllegalArgumentException.class, () -> fileSystem.open("tmp/../outside.txt", Mode.Write));
    }

    @Test
    void memoryAndBufferedDirectoryRenameCannotOrphanItsOwnTree() throws IOException {
        for (final boolean buffered : List.of(false, true)) {
            final var registry = new FileSystemRegistry(() -> tempDir);
            final var fs = buffered ? registry.fromSaveDirectory("rename-tree", 8192, true) : registry.fromMemory(8192);
            try {
                assertTrue(fs.makeDirectory("parent"));
                assertTrue(fs.makeDirectory("parent/child"));
                final int output = fs.open("parent/child/binary", Mode.Write);
                fs.getHandle(output).write(new byte[]{1, 2, 3});
                fs.getHandle(output).close();
                assertFalse(fs.rename("parent", "parent/child/loop"), "Directory moved into its own descendant");
                assertTrue(fs.exists("parent/child/binary"), "Rejected rename orphaned the tree");
                assertTrue(fs.rename("parent", "parent"));
                assertTrue(fs.exists("parent/child/binary"));
            } finally { fs.close(); }
        }
    }

    @Test
    void memoryAndBufferedFilesRejectASecondWriterWithoutDeletingOrTruncatingTheFirst() throws IOException {
        for (final boolean buffered : List.of(false, true)) {
            final var registry = new FileSystemRegistry(() -> tempDir);
            final var fs = buffered ? registry.fromSaveDirectory("one-writer", 4096, true) : registry.fromMemory(4096);
            try {
                final int output = fs.open("binary", Mode.Write);
                fs.getHandle(output).write(new byte[]{1, 2, 3});
                assertThrows(FileNotFoundException.class, () -> fs.open("binary", Mode.Write));
                assertThrows(FileNotFoundException.class, () -> fs.open("binary", Mode.Append));
                assertEquals(3, fs.size("binary"));
                assertFalse(fs.delete("binary"), "A live writer lost its directory entry");
                fs.getHandle(output).close();
                final int append = fs.open("binary", Mode.Append);
                assertEquals(3, fs.getHandle(append).position());
                fs.getHandle(append).close();
                assertTrue(fs.delete("binary"));
            } finally { fs.close(); }
        }
    }

    @Test
    void saveDirectoriesChargeEntryCostsAndEnforceCapacityInBothModes() throws IOException {
        for (final boolean buffered : List.of(false, true)) {
            final long cost = ModSettings.fileCost();
            final var fs = new FileSystemRegistry(() -> tempDir).fromSaveDirectory(buffered ? "buffered-quota" : "direct-quota", cost * 2 + 3, buffered);
            try {
                assertEquals(cost, fs.spaceUsed(), "Filesystem root must consume its entry cost");
                final int output = fs.open("binary", Mode.Write);
                fs.getHandle(output).write(new byte[]{1, 2, 3});
                assertEquals(cost * 2 + 3, fs.spaceUsed());
                assertThrows(IOException.class, () -> fs.getHandle(output).write(new byte[]{4}));
                assertThrows(FileNotFoundException.class, () -> fs.open("second", Mode.Write));
                assertFalse(fs.makeDirectory("folder"));
                fs.getHandle(output).close();
                assertTrue(fs.delete("binary"));
                assertEquals(cost, fs.spaceUsed());
                assertTrue(fs.makeDirectory("folder"));
                assertEquals(cost * 2, fs.spaceUsed());
            } finally { fs.close(); }
        }
    }

    @Test
    void bufferedRenameKeepsTheSourceWhenAnExternalDestinationBlocksSaving() throws IOException {
        final var directory = tempDir.resolve("opencomputers/conflict");
        Files.createDirectories(directory);
        Files.write(directory.resolve("old"), new byte[]{1, 2, 3});
        final var fs = new FileSystemRegistry(() -> tempDir).fromSaveDirectory("conflict", 4096, true);
        assertTrue(fs.rename("old", "new"));
        Files.createDirectories(directory.resolve("new"));
        Files.write(directory.resolve("new/external"), new byte[]{8});
        fs.save(new CompoundTag());
        assertThrows(java.util.concurrent.CompletionException.class, fs::close);
        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(directory.resolve("old")), "Failed rename save deleted its only durable source");
        assertArrayEquals(new byte[]{8}, Files.readAllBytes(directory.resolve("new/external")));
    }

    @Test
    void bufferedQueuedSavesAreOrderedBeforeAnotherInstanceLoadsThem() throws IOException {
        final var registry = new FileSystemRegistry(() -> tempDir);
        final var fs = registry.fromSaveDirectory("ordered", 4096, true);
        final int output = fs.open("binary", Mode.Write);
        fs.getHandle(output).write(new byte[]{1});
        fs.save(new CompoundTag());
        fs.getHandle(output).seek(0);
        fs.getHandle(output).write(new byte[]{2});
        final var latest = new CompoundTag();
        fs.save(latest);
        final var restored = registry.fromSaveDirectory("ordered", 4096, true);
        try {
            restored.load(latest);
            assertEquals(1, restored.getHandle(output).position());
            final int input = restored.open("binary", Mode.Read);
            final byte[] data = new byte[1];
            assertEquals(1, restored.getHandle(input).read(data));
            assertArrayEquals(new byte[]{2}, data);
        } finally { restored.close(); fs.close(); }
    }

    @Test
    void bufferedSaveDirectoryCommitsOnlySavedBytesAndKeepsContentsOutOfHostNbt() throws Exception {
        final var registry = new FileSystemRegistry(() -> tempDir);
        final var path = tempDir.resolve("opencomputers/buffered/binary");
        final var fs = registry.fromSaveDirectory("buffered", 4096, true);
        final int output = fs.open("binary", Mode.Write);
        fs.getHandle(output).write(new byte[]{0, 1, (byte) 255});
        assertFalse(Files.exists(path), "buffered=true wrote file contents before a save");
        final int input = fs.open("binary", Mode.Read);
        fs.getHandle(input).seek(1);
        fs.getHandle(output).seek(1);
        final var saved = new CompoundTag();
        final var started = new java.util.concurrent.CountDownLatch(1);
        final var release = new java.util.concurrent.CountDownLatch(1);
        final var blocker = BufferedFileSystem.IO.submit(() -> {
            started.countDown();
            if (!release.await(10, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Snapshot fixture was not released");
            return null;
        });
        try {
            assertTrue(started.await(5, java.util.concurrent.TimeUnit.SECONDS));
            fs.save(saved);
            assertFalse(saved.contains("children"), "Disk-backed files were duplicated into host NBT");
            fs.getHandle(output).write(new byte[]{9});
            assertFalse(Files.exists(path), "Save bypassed the serialized IO queue");
        } finally { release.countDown(); }
        blocker.get(5, java.util.concurrent.TimeUnit.SECONDS);
        fs.close();
        assertArrayEquals(new byte[]{0, 1, (byte) 255}, Files.readAllBytes(path), "Post-save mutations leaked into the queued disk snapshot");

        final var restored = registry.fromSaveDirectory("buffered", 4096, true);
        try {
            restored.load(saved);
            assertEquals(1, restored.getHandle(output).position());
            assertEquals(1, restored.getHandle(input).position());
            final byte[] data = new byte[2];
            assertEquals(2, restored.getHandle(input).read(data));
            assertArrayEquals(new byte[]{1, (byte) 255}, data);
            restored.getHandle(output).write(new byte[]{7});
        } finally { restored.close(); }
        assertArrayEquals(new byte[]{0, 1, (byte) 255}, Files.readAllBytes(path), "Closing a filesystem committed unsaved mutations");
    }

    @Test
    void bufferedSaveDirectoryDefersRenameAndDeletionAndPreservesUnrelatedDiskFiles() throws IOException {
        final var directory = tempDir.resolve("opencomputers/buffered");
        Files.createDirectories(directory.resolve("folder"));
        Files.write(directory.resolve("folder/old"), new byte[]{1, 2});
        Files.write(directory.resolve("remove"), new byte[]{3});
        final var fs = new FileSystemRegistry(() -> tempDir).fromSaveDirectory("buffered", 8192, true);
        assertTrue(fs.exists("folder/old"));
        assertTrue(fs.rename("folder/old", "folder/new"));
        assertTrue(fs.delete("remove"));
        assertTrue(fs.setLastModified("folder/new", 123456000));
        assertTrue(Files.exists(directory.resolve("folder/old")), "Buffered rename changed the host filesystem before save");
        assertTrue(Files.exists(directory.resolve("remove")), "Buffered deletion changed the host filesystem before save");
        Files.write(directory.resolve("external"), new byte[]{8});
        fs.save(new CompoundTag());
        fs.close();
        assertFalse(Files.exists(directory.resolve("folder/old")));
        assertFalse(Files.exists(directory.resolve("remove")));
        assertArrayEquals(new byte[]{1, 2}, Files.readAllBytes(directory.resolve("folder/new")));
        assertEquals(123456000, Files.getLastModifiedTime(directory.resolve("folder/new")).toMillis());
        assertArrayEquals(new byte[]{8}, Files.readAllBytes(directory.resolve("external")));
    }

    @Test
    void saveDirectoryRestoresOpenBinaryHandlesWithoutTruncatingFiles() throws IOException {
        final var registry = new FileSystemRegistry(() -> tempDir);
        final var fs = registry.fromSaveDirectory("handles", 4096, false);
        final int output = fs.open("binary", Mode.Write);
        fs.getHandle(output).write(new byte[]{0, 1, 2, (byte) 255});
        fs.getHandle(output).seek(2);
        final int input = fs.open("binary", Mode.Read);
        fs.getHandle(input).seek(1);
        final var tag = new CompoundTag();
        fs.save(tag);
        assertEquals(2, fs.getHandle(output).position());
        assertEquals(1, fs.getHandle(input).position());
        fs.close();

        final var restored = registry.fromSaveDirectory("handles", 4096, false);
        try {
            restored.load(tag);
            assertNotNull(restored.getHandle(input), "Saved input handle is absent after reload");
            assertNotNull(restored.getHandle(output), "Saved output handle is absent after reload");
            final byte[] buffer = new byte[3];
            assertEquals(3, restored.getHandle(input).read(buffer));
            assertArrayEquals(new byte[]{1, 2, (byte) 255}, buffer);
            restored.getHandle(output).write(new byte[]{9});
            assertArrayEquals(new byte[]{0, 1, 9, (byte) 255}, Files.readAllBytes(tempDir.resolve("opencomputers/handles/binary")));
            final int next = restored.open("binary", Mode.Read);
            assertTrue(next != input && next != output, "Restored handles were overwritten by a new handle");
        } finally { restored.close(); }
    }

    @Test
    void saveDirectorySkipsInvalidSavedHandlesWithoutCreatingFiles() throws IOException {
        final var registry = new FileSystemRegistry(() -> tempDir);
        final var fs = registry.fromSaveDirectory("handles", 4096, false);
        final int output = fs.open("binary", Mode.Write);
        fs.getHandle(output).write(new byte[]{1, 2, 3});
        fs.getHandle(output).close();
        final int input = fs.open("binary", Mode.Read);
        fs.getHandle(input).seek(1);
        final var tag = new CompoundTag();
        fs.save(tag);
        fs.close();
        final var escape = new CompoundTag();
        escape.putInt("handle", 123);
        escape.putString("path", "../outside");
        escape.putLong("position", 0);
        tag.getList("input", Tag.TAG_COMPOUND).add(escape);
        final var missing = new CompoundTag();
        missing.putInt("handle", 124);
        missing.putString("path", "missing");
        missing.putLong("position", 0);
        final var outputs = new ListTag();
        outputs.add(missing);
        tag.put("output", outputs);

        final var restored = registry.fromSaveDirectory("handles", 4096, false);
        try {
            restored.load(tag);
            assertEquals(1, restored.getHandle(input).position());
            assertNull(restored.getHandle(123));
            assertNull(restored.getHandle(124));
            assertFalse(restored.exists("missing"));
        } finally { restored.close(); }
    }

    @Test
    void saveDirectoryTracksTheCurrentWorldWithoutSharingDriveNames() throws IOException {
        final var world = new java.util.concurrent.atomic.AtomicReference<>(tempDir.resolve("first"));
        final var registry = new FileSystemRegistry(world::get);
        final var first = registry.fromSaveDirectory("addon/drive", 4096, false);
        final int id = first.open("marker", Mode.Write);
        first.getHandle(id).write(new byte[]{1, 2, 3});
        first.close();
        world.set(tempDir.resolve("second"));
        final var second = registry.fromSaveDirectory("addon/drive", 4096, false);
        assertFalse(second.exists("marker"));
        second.close();
        world.set(tempDir.resolve("first"));
        final var reloaded = registry.fromSaveDirectory("addon/drive", 4096, false);
        assertTrue(reloaded.exists("marker"));
        reloaded.close();
        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(tempDir.resolve("first/opencomputers/addon/drive/marker")));
    }

    @Test
    void saveDirectoryRequiresAWorldAndRejectsEscapingNames() throws IOException {
        final var registry = new FileSystemRegistry(() -> null);
        assertNull(registry.fromSaveDirectory("addon/drive", 4096, false));
        for (final var root : List.of("../outside", "addon/../outside", "C:outside", "\\outside", tempDir.toString())) {
            assertThrows(IllegalArgumentException.class, () -> registry.fromSaveDirectory(root, 4096, false));
        }
        final var occupied = tempDir.resolve("opencomputers/occupied");
        Files.createDirectories(occupied.getParent());
        Files.writeString(occupied, "preserve");
        assertNull(new FileSystemRegistry(() -> tempDir).fromSaveDirectory("occupied", 4096, false));
        assertEquals("preserve", Files.readString(occupied));
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
        assertEquals("MightyPirates GmbH & Co. KG", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
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
    void managedFileSystemEnvironmentPostsThrottledAccessEventsLikeUpstream() throws Exception {
        withCachedConfig(ModSettings.DISK_ACTIVITY_SOUND_DELAY, 500, () -> {
            OpenComputersApi.initialize();
            FileSystem fileSystem = API.fileSystem.fromMemory(4096);
            fileSystem.makeDirectory("tmp");
            String sound = "neoopencomputers:test/fs-access-" + System.nanoTime();
            ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(fileSystem, "tmp", new RecordingHost(), sound, 1);
            Component component = (Component) environment.node();
            List<FileSystemAccessEvent.Server> events = new ArrayList<>();
            NeoForge.EVENT_BUS.start();
            NeoForge.EVENT_BUS.addListener(FileSystemAccessEvent.Server.class, event -> {
                if (sound.equals(event.getSound())) {
                    events.add(event);
                }
            });

            component.invoke("exists", null, "tmp");
            component.invoke("size", null, "tmp");

            assertEquals(1, events.size());
            FileSystemAccessEvent.Server event = events.getFirst();
            assertEquals(environment.node(), event.getNode());
            assertEquals(1.5D, event.getX());
            assertEquals(2.5D, event.getY());
            assertEquals(3.5D, event.getZ());
            return null;
        });
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
    void managedFileSystemWithoutLabelReturnsNoLabelResultLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        FileSystem fileSystem = API.fileSystem.fromMemory(256);
        ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(fileSystem, (Label) null, null, null, 1);
        Component component = (Component) environment.node();

        assertNull(component.invoke("getLabel", null));
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
    void managedFileSystemEnvironmentClampsSeekOffsetToIntegerLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        FileSystem fileSystem = API.fileSystem.fromMemory(-1);
        ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(fileSystem, "tmp", null, null, 1);
        Component component = (Component) environment.node();

        Object handle = component.invoke("open", null, "data.txt", "w")[0];

        assertArrayEquals(new Object[]{(long) Integer.MAX_VALUE}, component.invoke("seek", null, handle, "set", Long.MAX_VALUE));
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
    void managedFileSystemEnvironmentValidatesIoArgumentsBeforeHandleOwnershipLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        FileSystem fileSystem = API.fileSystem.fromMemory(4096);
        ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(fileSystem, "tmp", null, null, 1);
        Component component = (Component) environment.node();
        charge(environment, 1D);
        RecordingContext owner = new RecordingContext("owner");
        RecordingContext stranger = new RecordingContext("stranger");

        Object handle = component.invoke("open", owner, "data.txt", "w")[0];

        IllegalArgumentException readError = assertThrows(IllegalArgumentException.class, () -> component.invoke("read", stranger, handle, "bad"));
        assertEquals("bad argument #2 (integer expected)", readError.getMessage());
        IllegalArgumentException writeError = assertThrows(IllegalArgumentException.class, () -> component.invoke("write", stranger, handle, new Object()));
        assertEquals("bad argument #2 (byte array expected)", writeError.getMessage());
        IllegalArgumentException seekError = assertThrows(IllegalArgumentException.class, () -> component.invoke("seek", stranger, handle, "set", "bad"));
        assertEquals("bad argument #3 (integer expected)", seekError.getMessage());
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

    private static final class RecordingHost implements EnvironmentHost {
        @Override
        public Level world() {
            return null;
        }

        @Override
        public double xPosition() {
            return 1.5D;
        }

        @Override
        public double yPosition() {
            return 2.5D;
        }

        @Override
        public double zPosition() {
            return 3.5D;
        }

        @Override
        public void markChanged() {
        }
    }
}
