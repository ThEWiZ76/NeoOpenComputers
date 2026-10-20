package li.cil.oc.common;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** File bytes live in memory until save; host NBT contains only stream metadata. */
final class BufferedFileSystem extends FileSystemRegistry.MemoryFileSystem {
    static final ExecutorService IO = Executors.newSingleThreadExecutor(task -> {
        final var thread = new Thread(task, "OpenComputers filesystem IO");
        thread.setDaemon(true);
        return thread;
    });

    static {
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> awaitIO(() -> null));
    }

    private final Path directory;
    // Accessed by the serial IO worker; includes progress from partially completed saves.
    private Map<String, CompoundTag> previous;
    private CompletableFuture<Void> pending = CompletableFuture.completedFuture(null);

    BufferedFileSystem(Path directory, long capacity) {
        this(directory, capacity, ModSettings.forceCaseInsensitiveFS());
    }

    BufferedFileSystem(Path directory, long capacity, boolean forceCaseInsensitive) {
        super(capacity, forceCaseInsensitive || hostIgnoresCase(directory));
        this.directory = directory.toAbsolutePath().normalize();
        try {
            final var contents = awaitIO(() -> {
                Files.createDirectories(this.directory);
                return readTree(this.directory);
            });
            previous = entries(contents);
            super.load(contents.copy());
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException("invalid buffered save directory: " + directory, failure);
        }
    }

    @Override public void save(CompoundTag nbt) {
        final var contents = new CompoundTag();
        super.save(contents);
        final var snapshot = contents.copy();
        pending = CompletableFuture.runAsync(() -> {
            try {
                persist(snapshot);
                previous = entries(snapshot);
            }
            catch (IOException failure) {
                NeoOpenComputers.LOGGER.error("Cannot save buffered filesystem {}", directory, failure);
                throw new UncheckedIOException(failure);
            }
        }, IO);
        nbt.remove("children");
        nbt.put("input", snapshot.getList("input", Tag.TAG_COMPOUND).copy());
        nbt.put("output", snapshot.getList("output", Tag.TAG_COMPOUND).copy());
        nbt.putLong("capacity.used", snapshot.getLong("capacity.used"));
    }

    @Override public void load(CompoundTag nbt) {
        pending.join();
        final var contents = awaitIO(() -> readTree(directory));
        validateLoad(contents);
        previous = entries(contents);
        final var handles = new HashSet<Integer>();
        copyHandles(nbt, contents, "input", handles);
        copyHandles(nbt, contents, "output", handles);
        super.close();
        super.load(contents);
    }

    @Override public void close() {
        try { pending.join(); }
        finally { super.close(); }
    }

    void flush() {
        pending.join();
    }

    private static boolean hostIgnoresCase(Path directory) {
        try {
            return awaitIO(() -> {
                if (Files.isSymbolicLink(directory)) throw new IOException("symbolic link in filesystem root");
                Files.createDirectories(directory);
                final var modified = Files.getLastModifiedTime(directory);
                final var before = directoryFingerprint(directory);
                final var probe = Files.createTempFile(directory, ".oc-case-", ".tmp");
                try {
                    final var alias = probe.resolveSibling(probe.getFileName().toString().toUpperCase(Locale.ROOT));
                    return Files.exists(alias) && Files.isSameFile(probe, alias);
                } finally {
                    Files.deleteIfExists(probe);
                    // Do not overwrite a concurrent external directory change with the probe's old timestamp.
                    if (before.equals(directoryFingerprint(directory))) Files.setLastModifiedTime(directory, modified);
                }
            });
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException("cannot determine save directory case policy: " + directory, failure);
        }
    }

    private record DirectoryEntry(Object key, FileTime modified, long size, boolean directory, boolean link) {}

    private static Map<String, DirectoryEntry> directoryFingerprint(Path directory) throws IOException {
        final var result = new LinkedHashMap<String, DirectoryEntry>();
        try (var children = Files.list(directory)) {
            for (final var child : children.toList()) {
                final var attrs = Files.readAttributes(child, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                result.put(child.getFileName().toString(), new DirectoryEntry(attrs.fileKey(), attrs.lastModifiedTime(),
                    attrs.size(), attrs.isDirectory(), attrs.isSymbolicLink()));
            }
        }
        return result;
    }

    private static void copyHandles(CompoundTag source, CompoundTag target, String key, HashSet<Integer> handles) {
        final var valid = new ListTag();
        final var saved = source.getList(key, Tag.TAG_COMPOUND);
        for (int index = 0; index < saved.size(); index++) {
            final var handle = saved.getCompound(index);
            final int id = handle.getInt("handle");
            if (id <= 0 || id == Integer.MAX_VALUE || handle.getLong("position") < 0 || handles.contains(id)) continue;
            try { FileSystemRegistry.pathSegments(handle.getString("path")); }
            catch (IllegalArgumentException failure) { continue; }
            handles.add(id);
            valid.add(handle.copy());
        }
        target.put(key, valid);
    }

    private void persist(CompoundTag after) throws IOException {
        final var oldEntries = new LinkedHashMap<>(previous);
        final var newEntries = entries(after);
        final var removed = oldEntries.keySet().stream().filter(path -> !path.isEmpty() &&
            (!newEntries.containsKey(path) || oldEntries.get(path).getBoolean("isDirectory") != newEntries.get(path).getBoolean("isDirectory")))
            .sorted(Comparator.comparingInt(String::length).reversed()).toList();
        for (final var relative : removed) {
            if (newEntries.containsKey(relative)) removeEntry(relative, oldEntries.get(relative));
        }
        for (final var entry : newEntries.entrySet()) {
            final var path = diskPath(entry.getKey());
            final var current = entry.getValue();
            if (current.getBoolean("isDirectory")) {
                Files.createDirectories(path);
                final var durable = current.copy();
                durable.putLong("lastModified", Files.getLastModifiedTime(path).toMillis());
                previous.put(entry.getKey(), durable);
            } else if (!current.equals(oldEntries.get(entry.getKey())) || !Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                final var old = oldEntries.get(entry.getKey());
                if (Files.exists(path, LinkOption.NOFOLLOW_LINKS) && (old == null || !unchangedOnDisk(path, old))) {
                    throw new IOException("Filesystem destination changed outside the buffer: " + path);
                }
                writeFile(entry.getKey(), path, current);
            }
        }
        // Keep rename sources durable until every destination has been written.
        for (final var relative : removed) {
            if (!newEntries.containsKey(relative)) removeEntry(relative, oldEntries.get(relative));
        }
        for (final var relative : newEntries.keySet().stream().sorted(Comparator.comparingInt(String::length).reversed()).toList()) {
            final var entry = newEntries.get(relative);
            if (entry.getBoolean("isDirectory")) {
                Files.setLastModifiedTime(diskPath(relative), FileTime.fromMillis(entry.getLong("lastModified")));
                previous.put(relative, entry.copy());
            }
        }
    }

    private void removeEntry(String relative, CompoundTag old) throws IOException {
        final var path = diskPath(relative);
        if (old.getBoolean("isDirectory")) {
            if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                final boolean empty;
                try (var children = Files.newDirectoryStream(path)) { empty = !children.iterator().hasNext(); }
                if (empty) Files.delete(path);
            }
        } else if (unchangedOnDisk(path, old)) Files.delete(path);
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) previous.remove(relative);
    }

    private static boolean unchangedOnDisk(Path path, CompoundTag entry) throws IOException {
        return !entry.getBoolean("isDirectory") && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
            && Files.getLastModifiedTime(path).toMillis() == entry.getLong("lastModified")
            && Arrays.equals(Files.readAllBytes(path), entry.getByteArray("data"));
    }

    private void writeFile(String relative, Path path, CompoundTag entry) throws IOException {
        final var temporary = Files.createTempFile(path.getParent(), ".oc-save-", ".tmp");
        try {
            Files.write(temporary, entry.getByteArray("data"));
            Files.setLastModifiedTime(temporary, FileTime.fromMillis(entry.getLong("lastModified")));
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException failure) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
            previous.put(relative, entry.copy());
        } finally { Files.deleteIfExists(temporary); }
    }

    private Path diskPath(String relative) throws IOException {
        Path path = directory;
        if (Files.isSymbolicLink(path)) throw new IOException("symbolic link in filesystem root");
        for (final var segment : FileSystemRegistry.pathSegments(relative)) {
            path = path.resolve(segment);
            if (Files.isSymbolicLink(path)) throw new IOException("symbolic link in filesystem path");
        }
        return path;
    }

    private static CompoundTag readTree(Path path) throws IOException {
        if (Files.isSymbolicLink(path)) throw new IOException("symbolic link in filesystem root");
        final var tag = new CompoundTag();
        final boolean directory = Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS);
        tag.putBoolean("isDirectory", directory);
        tag.putLong("lastModified", Files.getLastModifiedTime(path).toMillis());
        if (directory) {
            final var children = new ListTag();
            try (var entries = Files.list(path)) {
                for (final var child : entries.sorted().toList()) {
                    if (!Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS) && !Files.isRegularFile(child, LinkOption.NOFOLLOW_LINKS)) continue;
                    final var name = child.getFileName().toString();
                    try { FileSystemRegistry.pathSegments(name); }
                    catch (IllegalArgumentException failure) { continue; }
                    final var data = readTree(child);
                    data.putString("name", name);
                    children.add(data);
                }
            }
            tag.put("children", children);
        } else tag.putByteArray("data", Files.readAllBytes(path));
        return tag;
    }

    private static Map<String, CompoundTag> entries(CompoundTag tree) {
        final var result = new LinkedHashMap<String, CompoundTag>();
        flatten(result, "", tree, true);
        return result;
    }

    private static void flatten(Map<String, CompoundTag> entries, String path, CompoundTag tag, boolean directory) {
        final var entry = tag.copy();
        entry.remove("children");
        entry.remove("input");
        entry.remove("output");
        entry.remove("capacity.used");
        entry.putBoolean("isDirectory", directory);
        entries.put(path, entry);
        if (!directory) return;
        final var children = tag.getList("children", Tag.TAG_COMPOUND);
        for (int index = 0; index < children.size(); index++) {
            final var child = children.getCompound(index);
            final var name = path.isEmpty() ? child.getString("name") : path + "/" + child.getString("name");
            flatten(entries, name, child, child.getBoolean("isDirectory"));
        }
    }

    static <T> T awaitIO(Callable<T> work) {
        try { return IO.submit(work).get(); }
        catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted waiting for filesystem IO", failure);
        } catch (ExecutionException failure) { throw new IllegalStateException("Filesystem IO failed", failure.getCause()); }
    }
}
