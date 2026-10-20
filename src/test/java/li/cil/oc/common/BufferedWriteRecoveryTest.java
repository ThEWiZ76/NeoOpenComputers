package li.cil.oc.common;

import li.cil.oc.api.fs.Mode;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

class BufferedWriteRecoveryTest {
    @TempDir Path directory;

    @Test
    void repairedDestinationCanRetryWithoutLosingTheDirtyWriter() throws Exception {
        final var file = directory.resolve("probe"); Files.write(file, new byte[]{1, 2, 3});
        Files.setLastModifiedTime(file, FileTime.fromMillis(1000));
        final var fs = new BufferedFileSystem(directory, 8192);
        final int writer = fs.open("probe", Mode.Append); fs.getHandle(writer).write(new byte[]{4});
        Files.write(file, new byte[]{8});
        final var snapshot = new CompoundTag(); fs.save(snapshot);
        assertThrows(CompletionException.class, () -> fs.load(snapshot));
        assertArrayEquals(new byte[]{8}, Files.readAllBytes(file));
        assertEquals(4, fs.getHandle(writer).position());
        fs.getHandle(writer).write(new byte[]{5});
        Files.write(file, new byte[]{1, 2, 3}); Files.setLastModifiedTime(file, FileTime.fromMillis(1000));
        fs.save(new CompoundTag()); fs.close();
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, Files.readAllBytes(file));
    }

    @Test
    void retryRecognizesFilesPublishedBeforeALaterEntryFailed() throws Exception {
        for (final var name : java.util.List.of("a", "z")) {
            Files.write(directory.resolve(name), new byte[]{1});
            Files.setLastModifiedTime(directory.resolve(name), FileTime.fromMillis(1000));
        }
        final var fs = new BufferedFileSystem(directory, 8192);
        for (final var name : java.util.List.of("a", "z")) {
            final int writer = fs.open(name, Mode.Write); fs.getHandle(writer).write(new byte[]{2}); fs.getHandle(writer).close();
        }
        Files.write(directory.resolve("z"), new byte[]{99});
        final var snapshot = new CompoundTag(); fs.save(snapshot);
        assertThrows(CompletionException.class, () -> fs.load(snapshot));
        assertArrayEquals(new byte[]{2}, Files.readAllBytes(directory.resolve("a")));
        assertArrayEquals(new byte[]{99}, Files.readAllBytes(directory.resolve("z")));
        Files.write(directory.resolve("z"), new byte[]{1}); Files.setLastModifiedTime(directory.resolve("z"), FileTime.fromMillis(1000));
        fs.save(new CompoundTag()); fs.close();
        assertArrayEquals(new byte[]{2}, Files.readAllBytes(directory.resolve("a")));
        assertArrayEquals(new byte[]{2}, Files.readAllBytes(directory.resolve("z")));
    }
}
