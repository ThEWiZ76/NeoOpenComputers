package li.cil.oc.common;

import li.cil.oc.api.fs.Mode;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import static org.junit.jupiter.api.Assertions.*;

class BufferedCaseSensitivityTest {
    @TempDir Path directory;

    @Test
    void aliasesPreserveSpellingAndRestoreOpenStreams() throws Exception {
        final var metadata = new CompoundTag();
        final var fs = new BufferedFileSystem(directory, 65536, true);
        final int reader;
        final int writer;
        try {
            assertTrue(fs.makeDirectory("Apps"));
            writer = fs.open("apps/Boot.lua", Mode.Write);
            fs.getHandle(writer).write(new byte[]{97, 0, (byte) 255});
            reader = fs.open("APPS/BOOT.LUA", Mode.Read);
            assertEquals(1, fs.getHandle(reader).read(new byte[1]));
            assertThrows(FileNotFoundException.class, () -> fs.open("apps/boot.lua", Mode.Write));
            fs.save(metadata);
        } finally { fs.close(); }

        final var restored = new BufferedFileSystem(directory, 65536, true);
        try {
            restored.load(metadata);
            restored.getHandle(writer).write(new byte[]{42});
            assertThrows(FileNotFoundException.class, () -> restored.open("APPS/boot.LUA", Mode.Append));
            restored.getHandle(writer).close();
            final var remaining = new byte[3];
            assertEquals(3, restored.getHandle(reader).read(remaining));
            assertArrayEquals(new byte[]{0, (byte) 255, 42}, remaining);
            restored.getHandle(reader).close();
            assertTrue(restored.rename("APPS/BOOT.LUA", "apps/boot.lua"));
            assertArrayEquals(new String[]{"Boot.lua"}, restored.list("APPS"));
            final int append = restored.open("apps/BOOT.lua", Mode.Append);
            restored.getHandle(append).write(new byte[]{43});
            restored.getHandle(append).close();
            assertTrue(restored.rename("apps/boot.LUA", "APPS/Start.lua"));
            assertTrue(restored.rename("APPS", "Programs"));
            restored.save(new CompoundTag());
        } finally { restored.close(); }
        assertArrayEquals(new byte[]{97, 0, (byte) 255, 42, 43}, Files.readAllBytes(directory.resolve("Programs/Start.lua")));
        assertFalse(Files.exists(directory.resolve("Apps")));

        final var cleanup = new BufferedFileSystem(directory, 65536, true);
        try {
            assertTrue(cleanup.delete("programs/START.LUA"));
            assertTrue(cleanup.delete("PROGRAMS"));
            cleanup.save(new CompoundTag());
        } finally { cleanup.close(); }
        try (var children = Files.list(directory)) { assertEquals(0, children.count()); }
    }

    @Test
    void ambiguousSnapshotsAreRejectedBeforeReplacingDataOrHandles() throws Exception {
        final var source = new FileSystemRegistry.MemoryFileSystem(65536);
        source.getHandle(source.open("Alpha", Mode.Write)).close();
        source.getHandle(source.open("ALPHA", Mode.Write)).close();
        final var ambiguous = new CompoundTag();
        source.save(ambiguous);
        source.close();
        final var target = new FileSystemRegistry.MemoryFileSystem(65536, true);
        try {
            final int writer = target.open("Keep.bin", Mode.Write);
            target.getHandle(writer).write(new byte[]{10, 20});
            target.getHandle(writer).close();
            final int reader = target.open("KEEP.BIN", Mode.Read);
            target.getHandle(reader).read(new byte[1]);
            assertThrows(IllegalArgumentException.class, () -> target.load(ambiguous));
            assertArrayEquals(new String[]{"Keep.bin"}, target.list(""));
            final var last = new byte[1];
            assertEquals(1, target.getHandle(reader).read(last));
            assertArrayEquals(new byte[]{20}, last);
        } finally { target.close(); }
    }

    @Test
    void ordinaryMemoryFilesystemsRemainCaseSensitive() throws Exception {
        final var fs = new FileSystemRegistry().fromMemory(65536);
        try {
            fs.getHandle(fs.open("Alpha", Mode.Write)).close();
            assertFalse(fs.exists("ALPHA"));
            fs.getHandle(fs.open("ALPHA", Mode.Write)).close();
            assertEquals(2, fs.list("").length);
        } finally { fs.close(); }
    }

    @Test
    void hostProbeLeavesNoFilesOrDirectoryTimestampChange() throws Exception {
        Files.write(directory.resolve("Boot.lua"), new byte[]{97});
        final boolean insensitive = Files.exists(directory.resolve("BOOT.LUA"));
        Files.setLastModifiedTime(directory, FileTime.fromMillis(1000));
        final var expected = Files.getLastModifiedTime(directory);
        final var fs = new BufferedFileSystem(directory, 65536, false);
        try {
            assertEquals(insensitive, fs.exists("BOOT.LUA"));
            assertEquals(expected, Files.getLastModifiedTime(directory));
            try (var children = Files.list(directory)) { assertEquals(1, children.count()); }
        } finally { fs.close(); }
    }
}
