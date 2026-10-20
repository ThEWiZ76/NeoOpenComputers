package li.cil.oc.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class NativeRawVolumeStoreTest {
    private static final String ID = "11111111-2222-3333-4444-555555555555";
    @TempDir Path world;

    @Test
    void importsInlineBytesWithoutMutatingSourceAndNeverReplaysACompletedImport() throws Exception {
        final var source = source("old raw fixture", image(17));
        source.putString("oc:fs.label", "Keep me"); source.putString("oc:lock", "Owner"); source.putInt("headPos", 7);
        final var before = source.copy();
        final var store = new NativeVolumeStore(world);
        final var mount = store.openRaw(source, 512);
        assertEquals("old raw fixture", mount.nodeAddress());
        assertTrue(mount.storageRoot().matches("native-legacy/[0-9a-f]{64}\\.bin"));
        assertArrayEquals(image(17), read(mount.storageRoot()));
        assertArrayEquals(image(17), mount.volume().load(source));
        mount.volume().save(image(42));
        assertArrayEquals(image(42), store.openRaw(source, 512).volume().load(source));
        assertEquals(before, source);
    }

    @Test
    void adoptsAnExistingUpstreamGzipWithoutRewritingItsBytesOrTimestamp() throws Exception {
        gzip(ID + ".bin", image(17));
        final var path = world.resolve("opencomputers/" + ID + ".bin");
        Files.setLastModifiedTime(path, FileTime.fromMillis(1000));
        final var before = Files.readAllBytes(path);
        final var mount = new NativeVolumeStore(world).openRaw(source(ID, null), 512);
        assertArrayEquals(image(17), mount.volume().load());
        mount.volume().save(image(17));
        assertArrayEquals(before, Files.readAllBytes(path));
        assertEquals(FileTime.fromMillis(1000), Files.getLastModifiedTime(path));
    }

    @Test
    void conflictingInlineBytesCannotOverwriteAnExistingVolume() throws Exception {
        gzip(ID + ".bin", image(42));
        final var source = source(ID, image(17)); final var before = source.copy();
        assertThrows(IOException.class, () -> new NativeVolumeStore(world).openRaw(source, 512));
        assertArrayEquals(image(42), read(ID + ".bin"));
        assertEquals(before, source);
    }

    @Test
    void pendingImportRecoversMissingOrAlreadyPublishedMatchingBytes() throws Exception {
        for (final boolean published : java.util.List.of(false, true)) {
            final var isolated = world.resolve(published ? "published" : "missing");
            final var root = ID + ".bin";
            final var journals = isolated.resolve("opencomputers/.native-imports"); Files.createDirectories(journals);
            final var pending = new CompoundTag(); pending.putInt("version", 1); pending.putString("root", root);
            pending.putString("digest", hash(image(17))); pending.putBoolean("complete", false);
            final var journal = journals.resolve(hash(root.getBytes(StandardCharsets.UTF_8)) + ".nbt");
            NbtIo.writeCompressed(pending, journal);
            if (published) {
                try (var stream = new GZIPOutputStream(Files.newOutputStream(isolated.resolve("opencomputers/" + root)))) { stream.write(image(17)); }
            }
            final var store = new NativeVolumeStore(isolated);
            assertThrows(IOException.class, () -> store.openRaw(source(ID, null), 512));
            assertArrayEquals(image(17), store.openRaw(source(ID, image(17)), 512).volume().load());
            assertTrue(NbtIo.readCompressed(journal, NbtAccounter.create(65536)).getBoolean("complete"));
        }
    }

    @Test
    void bindingSurvivesNodeRemappingAndMissingBoundFilesFailExplicitly() throws Exception {
        final var store = new NativeVolumeStore(world);
        final var first = store.openRaw(source(ID, null), 512);
        first.volume().save(image(17));
        final var remapped = source("new visible address", null);
        remapped.putString(NativeVolumeStore.RAW_STORAGE_TAG, first.storageRoot());
        final var second = store.openRaw(remapped, 512);
        assertEquals("new visible address", second.nodeAddress());
        assertEquals(first.storageRoot(), second.storageRoot());
        assertArrayEquals(image(17), second.volume().load());
        Files.delete(world.resolve("opencomputers/" + first.storageRoot()));
        assertThrows(IOException.class, () -> store.openRaw(remapped, 512));
        assertThrows(IOException.class, () -> second.volume().load());
    }

    @Test
    void externalChangesBlockSavingUntilAnExplicitReload() throws Exception {
        final var mount = new NativeVolumeStore(world).openRaw(source(ID, image(17)), 512);
        gzip(mount.storageRoot(), image(42));
        assertThrows(IOException.class, () -> mount.volume().save(image(99)));
        assertArrayEquals(image(42), read(mount.storageRoot()));
        assertArrayEquals(image(42), mount.volume().load());
        mount.volume().save(image(99));
        assertArrayEquals(image(99), read(mount.storageRoot()));
    }

    @Test
    void twoCopiesCannotSilentlyOverwriteEachOthersCommittedBytes() throws Exception {
        final var store = new NativeVolumeStore(world);
        final var first = store.openRaw(source(ID, image(17)), 512);
        final var copied = source("copied node", null); copied.putString(NativeVolumeStore.RAW_STORAGE_TAG, first.storageRoot());
        final var second = store.openRaw(copied, 512);
        first.volume().save(image(42));
        assertThrows(IOException.class, () -> second.volume().save(image(99)));
        assertArrayEquals(image(42), read(first.storageRoot()));
        assertArrayEquals(image(42), second.volume().load());
        second.volume().save(image(99));
        assertArrayEquals(image(99), first.volume().load());
    }

    @Test
    void corruptionAndOversizedVolumesAreNotSilentlyTruncatedOrEmptied() throws Exception {
        final var root = ID + ".bin"; final var store = new NativeVolumeStore(world);
        gzip(root, new byte[1024]);
        assertThrows(IOException.class, () -> store.openRaw(source(ID, null), 512));
        assertEquals(1024, read(root).length);
        final var path = world.resolve("opencomputers/" + root); Files.write(path, new byte[]{1, 2, 3});
        assertThrows(IOException.class, () -> store.openRaw(source(ID, null), 512));
        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(path));
        gzip(root, image(17)); final var compressed = Files.readAllBytes(path);
        Files.write(path, Arrays.copyOf(compressed, compressed.length - 4));
        assertThrows(IOException.class, () -> store.openRaw(source(ID, null), 512));
    }

    @Test
    void malformedMetadataAndEscapingBindingsFailBeforePublishingAnything() {
        final var store = new NativeVolumeStore(world);
        final var invalid = source(ID, null); invalid.putString("data", "not bytes");
        assertThrows(IOException.class, () -> store.openRaw(invalid, 512));
        final var escaping = source(ID, null); escaping.putString(NativeVolumeStore.RAW_STORAGE_TAG, "../foreign.bin");
        assertThrows(IOException.class, () -> store.openRaw(escaping, 512));
        assertFalse(Files.exists(world.resolve("opencomputers")));
    }

    @Test
    void aLateInlineSnapshotCannotBeSilentlyIgnoredByAnAlreadyMountedDrive() throws Exception {
        OpenComputersApi.initialize();
        final var mount = new NativeVolumeStore(world).openRaw(source(ID, null), 512);
        final var drive = new DriveEnvironment(512, 1, null, null, null, 0, "", mount);
        final var late = new CompoundTag(); late.putByteArray("data", image(17));
        assertThrows(java.io.UncheckedIOException.class, () -> drive.load(late));
        assertArrayEquals(image(17), late.getByteArray("data"));
    }

    private static CompoundTag source(String address, byte[] bytes) {
        final var tag = new CompoundTag(); final var node = new CompoundTag(); node.putString("address", address); tag.put("node", node);
        if (bytes != null) tag.putByteArray("data", bytes);
        return tag;
    }
    private static byte[] image(int value) { final var bytes = new byte[512]; bytes[0] = (byte) value; bytes[1] = (byte) 255; return bytes; }
    private static String hash(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private void gzip(String root, byte[] bytes) throws IOException {
        final var path = world.resolve("opencomputers/" + root); Files.createDirectories(path.getParent());
        try (var stream = new GZIPOutputStream(Files.newOutputStream(path))) { stream.write(bytes); }
    }
    private byte[] read(String root) throws IOException {
        try (var stream = new GZIPInputStream(Files.newInputStream(world.resolve("opencomputers/" + root)))) { return stream.readAllBytes(); }
    }
}
