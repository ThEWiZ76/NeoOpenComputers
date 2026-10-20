package li.cil.oc.common;

import li.cil.oc.api.fs.Mode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class NativeVolumeStoreTest {
    private static final String ID = "00000000-0000-4000-8000-000000000001";
    private static final String NEW_ID = "00000000-0000-4000-8000-000000000002";
    private static final byte[] BYTES = {97, 0, (byte) 128, (byte) 255, 42};
    @TempDir Path world;

    @Test void importsNestedBinaryFilesWithoutChangingTheSource() throws Exception {
        final var source = legacy(); final var before = source.copy();
        final var mount = store().openManaged(source, 65536, false);
        try {
            assertEquals(ID, mount.nodeAddress()); assertEquals(ID, mount.storageRoot());
            assertArrayEquals(BYTES, Files.readAllBytes(volume().resolve("apps/probe.bin")));
            assertTrue(Files.isDirectory(volume().resolve("empty")));
            assertEquals(1000, Files.getLastModifiedTime(volume().resolve("apps/probe.bin")).toMillis());
            assertArrayEquals(BYTES, read(mount.fileSystem(), "apps/probe.bin"));
            assertEquals(before, source);
        } finally { mount.fileSystem().close(); }
    }

    @Test void acceptsTheActualRamSerializerWithItsImplicitRootDirectory() throws Exception {
        final var ram = new FileSystemRegistry().fromMemory(65536);
        ram.makeDirectory("apps"); final int output = ram.open("apps/probe.bin", Mode.Write);
        ram.getHandle(output).write(BYTES); ram.getHandle(output).close();
        for (final var path : List.of("", "apps", "apps/probe.bin")) ram.setLastModified(path, 1000);
        final var serialized = new CompoundTag(); ram.save(serialized); ram.close();
        assertFalse(serialized.contains("isDirectory"));
        final var source = new CompoundTag(); final var node = new CompoundTag(); node.putString("address", ID);
        source.put("node", node); source.put("fs", serialized); final var before = source.copy();
        final var mount = store().openManaged(source, 65536, false);
        try { assertArrayEquals(BYTES, read(mount.fileSystem(), "apps/probe.bin")); assertEquals(before, source); }
        finally { mount.fileSystem().close(); }
    }

    @Test void missingBoundVolumesAndPendingImportsWithoutSourceFailClosed() throws Exception {
        final var source = legacy(); source.getCompound("fs").remove("children");
        source.getCompound("fs").putString(NativeVolumeStore.STORAGE_TAG, ID);
        assertThrows(IOException.class, () -> store().openManaged(source, 65536, false));
        assertFalse(Files.exists(volume()));
        final var old = legacy(); final var mount = store().openManaged(old, 65536, false); mount.fileSystem().close();
        markPending();
        assertThrows(IOException.class, () -> store().openManaged(source, 65536, false));
        assertArrayEquals(BYTES, Files.readAllBytes(volume().resolve("apps/probe.bin")));
    }

    @Test void restoresSavedInputAndOutputPositionsInBothModes() throws Exception {
        for (boolean buffered : new boolean[]{false, true}) {
            final var root = world.resolve(buffered ? "buffered" : "direct");
            final var source = legacy();
            final var mount = new NativeVolumeStore(root, () -> NEW_ID).openManaged(source, 65536, buffered);
            final var fs = mount.fileSystem();
            try {
                fs.load(source.getCompound("fs"));
                assertEquals(1, fs.getHandle(11).position()); assertEquals(2, fs.getHandle(12).position());
                final byte[] bytes = new byte[4]; assertEquals(4, fs.getHandle(11).read(bytes));
                assertArrayEquals(new byte[]{0, (byte) 128, (byte) 255, 42}, bytes);
                fs.getHandle(12).write(new byte[]{17, 18});
                final var saved = new CompoundTag(); fs.save(saved);
                assertFalse(saved.contains("children"));
            } finally { fs.close(); }
            assertArrayEquals(new byte[]{97, 0, 17, 18, 42}, Files.readAllBytes(root.resolve("opencomputers/" + ID + "/apps/out.bin")));
        }
    }

    @Test void completedImportNeverReplaysAnOldInlineSnapshot() throws Exception {
        final var source = legacy();
        final var first = store().openManaged(source, 65536, true); first.fileSystem().close();
        Files.write(volume().resolve("apps/probe.bin"), new byte[]{9, 0, (byte) 255});
        final var reopened = store().openManaged(source, 65536, true);
        try { assertArrayEquals(new byte[]{9, 0, (byte) 255}, read(reopened.fileSystem(), "apps/probe.bin")); }
        finally { reopened.fileSystem().close(); }
    }

    @Test void foreignTargetsAndDifferentInlineGenerationsArePreserved() throws Exception {
        final var source = legacy(); final var before = source.copy();
        Files.createDirectories(volume()); Files.writeString(volume().resolve("foreign.txt"), "keep");
        assertThrows(IOException.class, () -> store().openManaged(source, 65536, false));
        assertEquals("keep", Files.readString(volume().resolve("foreign.txt"))); assertEquals(before, source);
        assertFalse(Files.exists(volume().resolve("apps")));
        final var other = world.resolve("other"); final var service = new NativeVolumeStore(other, () -> NEW_ID);
        final var mounted = service.openManaged(source, 65536, false); mounted.fileSystem().close();
        final var changed = source.copy();
        changed.getCompound("fs").getList("children", 10).getCompound(0).getList("children", 10).getCompound(0).putByteArray("data", new byte[]{8});
        assertThrows(IOException.class, () -> service.openManaged(changed, 65536, false));
        assertArrayEquals(BYTES, Files.readAllBytes(other.resolve("opencomputers/" + ID + "/apps/probe.bin")));
    }

    @Test void adoptsIdenticalExistingContentWithoutRewritingIt() throws Exception {
        Files.createDirectories(volume().resolve("apps")); Files.createDirectories(volume().resolve("empty"));
        Files.write(volume().resolve("apps/probe.bin"), BYTES); Files.write(volume().resolve("apps/out.bin"), BYTES);
        Files.setLastModifiedTime(volume().resolve("apps/probe.bin"), FileTime.fromMillis(2000));
        final var mount = store().openManaged(legacy(), 65536, false); mount.fileSystem().close();
        assertEquals(2000, Files.getLastModifiedTime(volume().resolve("apps/probe.bin")).toMillis());
    }

    @Test void pendingImportResumesMissingEntriesButNeverReplacesConflicts() throws Exception {
        final var source = legacy(); final var before = source.copy();
        final var first = store().openManaged(source, 65536, false); first.fileSystem().close();
        markPending(); Files.delete(volume().resolve("apps/probe.bin"));
        final var resumed = store().openManaged(source, 65536, false); resumed.fileSystem().close();
        assertArrayEquals(BYTES, Files.readAllBytes(volume().resolve("apps/probe.bin")));
        markPending(); Files.write(volume().resolve("apps/probe.bin"), new byte[]{7});
        assertThrows(IOException.class, () -> store().openManaged(source, 65536, false));
        assertArrayEquals(new byte[]{7}, Files.readAllBytes(volume().resolve("apps/probe.bin"))); assertEquals(before, source);
    }

    @Test void invalidTreesAndDuplicateNamesFailBeforePublishingAnything() throws Exception {
        int index = 0;
        for (final var name : List.of("../escape", "a/b", "", "C:\\escape")) {
            final var source = legacy(); source.getCompound("fs").getList("children", 10).getCompound(0).putString("name", name);
            final var root = world.resolve("invalid-" + index++);
            assertThrows(IOException.class, () -> new NativeVolumeStore(root, () -> NEW_ID).openManaged(source, 65536, false));
            assertFalse(Files.exists(root.resolve("opencomputers")));
        }
        final var duplicate = legacy(); final var children = duplicate.getCompound("fs").getList("children", 10);
        children.add(children.getCompound(0).copy());
        assertThrows(IOException.class, () -> store().openManaged(duplicate, 65536, false));
        final var malformed = legacy(); final var bad = new ListTag(); bad.add(StringTag.valueOf("not a tree")); malformed.getCompound("fs").put("children", bad);
        assertThrows(IOException.class, () -> store().openManaged(malformed, 65536, false));
        final var wrongRootType = legacy(); wrongRootType.putByteArray("fs", BYTES);
        assertThrows(IOException.class, () -> store().openManaged(wrongRootType, 65536, false));
        assertFalse(Files.exists(world.resolve("opencomputers")));
    }

    @Test void legacyAddressesCannotEscapeAndBindingsSurviveAddressRemap() throws Exception {
        final var source = legacy(); source.getCompound("node").putString("address", "../../outside");
        final var mount = store().openManaged(source, 65536, false);
        final String root = mount.storageRoot();
        try {
            assertEquals("../../outside", mount.nodeAddress()); assertTrue(root.matches("native-legacy/[0-9a-f]{64}"));
            assertArrayEquals(BYTES, Files.readAllBytes(world.resolve("opencomputers").resolve(root).resolve("apps/probe.bin")));
        } finally { mount.fileSystem().close(); }
        source.getCompound("node").putString("address", NEW_ID); source.getCompound("fs").putString(NativeVolumeStore.STORAGE_TAG, root);
        final var remapped = store().openManaged(source, 65536, false);
        try { assertEquals(NEW_ID, remapped.nodeAddress()); assertEquals(root, remapped.storageRoot()); }
        finally { remapped.fileSystem().close(); }
        source.getCompound("fs").putString(NativeVolumeStore.STORAGE_TAG, "../foreign");
        assertThrows(IOException.class, () -> store().openManaged(source, 65536, false));
    }

    @Test void newDisksAndExistingExternalVolumesNeedNoInlineTree() throws Exception {
        final var empty = new CompoundTag(); final var mount = store().openManaged(empty, 65536, false);
        try { assertEquals(NEW_ID, mount.nodeAddress()); assertEquals(NEW_ID, mount.storageRoot()); assertTrue(empty.isEmpty()); }
        finally { mount.fileSystem().close(); }
        final var source = new CompoundTag(); final var node = new CompoundTag(); node.putString("address", ID); source.put("node", node);
        Files.createDirectories(volume()); Files.write(volume().resolve("existing.bin"), BYTES);
        final var external = store().openManaged(source, 65536, false);
        try { assertArrayEquals(BYTES, read(external.fileSystem(), "existing.bin")); }
        finally { external.fileSystem().close(); }
    }

    @Test void corruptJournalDoesNotReplaceFilesOrSource() throws Exception {
        final var source = legacy(); final var before = source.copy();
        final var first = store().openManaged(source, 65536, false); first.fileSystem().close();
        final var journal = journal(); Files.write(journal, new byte[]{0, 1, 2});
        assertThrows(IOException.class, () -> store().openManaged(source, 65536, false));
        assertArrayEquals(new byte[]{0, 1, 2}, Files.readAllBytes(journal));
        assertArrayEquals(BYTES, Files.readAllBytes(volume().resolve("apps/probe.bin"))); assertEquals(before, source);
    }

    private NativeVolumeStore store() { return new NativeVolumeStore(world, () -> NEW_ID); }
    private Path volume() { return world.resolve("opencomputers/" + ID); }
    private Path journal() throws IOException {
        try (var files = Files.list(world.resolve("opencomputers/.native-imports"))) { return files.filter(path -> path.toString().endsWith(".nbt")).findFirst().orElseThrow(); }
    }
    private void markPending() throws IOException {
        final var path = journal(); final var tag = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap()); tag.putBoolean("complete", false); NbtIo.writeCompressed(tag, path);
    }
    private static byte[] read(li.cil.oc.api.fs.FileSystem fs, String path) throws IOException {
        final int id = fs.open(path, Mode.Read);
        try { final byte[] bytes = new byte[(int) fs.size(path)]; assertEquals(bytes.length, fs.getHandle(id).read(bytes)); return bytes; }
        finally { fs.getHandle(id).close(); }
    }
    private static CompoundTag legacy() {
        final var source = new CompoundTag(); final var node = new CompoundTag(); node.putString("address", ID); source.put("node", node);
        source.putString("oc:lock", "Keep owner"); source.putString("oc:fs.label", "Keep label");
        final var fs = directory("", directory("apps", file("probe.bin"), file("out.bin")), directory("empty"));
        final var inputs = new ListTag(); inputs.add(handle(11, "apps/probe.bin", 1)); fs.put("input", inputs);
        final var outputs = new ListTag(); outputs.add(handle(12, "apps/out.bin", 2)); fs.put("output", outputs);
        source.put("fs", fs); return source;
    }
    private static CompoundTag handle(int id, String path, long position) {
        final var tag = new CompoundTag(); tag.putInt("handle", id); tag.putString("path", path); tag.putLong("position", position); return tag;
    }
    private static CompoundTag directory(String name, CompoundTag... entries) {
        final var tag = new CompoundTag(); tag.putString("name", name); tag.putBoolean("isDirectory", true); tag.putLong("lastModified", 1000);
        final var children = new ListTag(); for (final var child : entries) children.add(child); tag.put("children", children); return tag;
    }
    private static CompoundTag file(String name) {
        final var tag = new CompoundTag(); tag.putString("name", name); tag.putBoolean("isDirectory", false); tag.putLong("lastModified", 1000); tag.putByteArray("data", BYTES.clone()); return tag;
    }
}
