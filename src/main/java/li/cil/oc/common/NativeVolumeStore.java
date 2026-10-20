package li.cil.oc.common;

import li.cil.oc.api.fs.FileSystem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Supplier;

/** Imports old inline volumes before callers omit their contents from subsequent snapshots. */
public final class NativeVolumeStore {
    public static final String STORAGE_TAG = "oc:storage";
    private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    private static final Object OPEN_LOCK = new Object();
    private final Path world;
    private final Supplier<String> addresses;

    public NativeVolumeStore(Path world) { this(world, () -> UUID.randomUUID().toString()); }

    NativeVolumeStore(Path world, Supplier<String> addresses) {
        this.world = Objects.requireNonNull(world).toAbsolutePath().normalize();
        this.addresses = Objects.requireNonNull(addresses);
    }

    public record Mount(String storageRoot, String nodeAddress, FileSystem fileSystem) { }

    public Mount openManaged(CompoundTag original, long capacity, boolean buffered) throws IOException {
        synchronized (OPEN_LOCK) {
            final var source = original.copy();
            if (source.contains("fs") && !source.contains("fs", Tag.TAG_COMPOUND) || source.contains("node") && !source.contains("node", Tag.TAG_COMPOUND)) throw new IOException("Malformed native volume metadata");
            if (source.getCompound("node").contains("address") && !source.getCompound("node").contains("address", Tag.TAG_STRING)) throw new IOException("Malformed native volume address");
            String address = source.getCompound("node").getString("address");
            if (address.isEmpty()) address = Objects.requireNonNull(addresses.get());
            final var metadata = source.getCompound("fs");
            final boolean bound = metadata.contains(STORAGE_TAG);
            final String root = bound ? metadata.getString(STORAGE_TAG) : address.matches(UUID_PATTERN) ? address : "native-legacy/" + hash(address);
            if (!root.matches(UUID_PATTERN) && !root.matches("native-legacy/[0-9a-f]{64}")) throw new IOException("Invalid native storage binding");
            final boolean inline = metadata.contains("children");
            final var entries = inline ? tree(metadata) : null;
            final String digest = inline ? digest(entries) : null;
            final var target = checked(world.resolve("opencomputers").resolve(root));
            final var journals = checked(world.resolve("opencomputers/.native-imports"));
            final var journal = checked(journals.resolve(hash(root) + ".nbt"));
            // Inspect/import between buffered commits, never while their temporary files are being replaced.
            try {
                BufferedFileSystem.awaitIO(() -> {
                    final var previous = Files.exists(journal, LinkOption.NOFOLLOW_LINKS) ? readJournal(journal, root) : null;
                    if (previous != null) {
                        if (inline && !previous.getString("digest").equals(digest)) throw new IOException("Conflicting inline snapshot for native volume " + root);
                        if (previous.getBoolean("complete")) {
                            if (!Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Completed native volume is missing: " + root);
                            verifyExisting(target, null, false);
                        } else {
                            if (!inline) throw new IOException("Pending native import requires its original inline snapshot: " + root);
                            verifyExisting(target, entries, true);
                            importEntries(target, journals, entries);
                            writeJournal(journal, root, digest, true, true);
                        }
                    } else if (inline) {
                        final boolean existed = Files.exists(target, LinkOption.NOFOLLOW_LINKS);
                        verifyExisting(target, entries, !existed);
                        Files.createDirectories(journals);
                        writeJournal(journal, root, digest, false, false);
                        if (!existed) importEntries(target, journals, entries);
                        writeJournal(journal, root, digest, true, true);
                    } else {
                        if (bound && !Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Bound native volume is missing: " + root);
                        verifyExisting(target, null, true);
                    }
                    return null;
                });
            } catch (IllegalStateException failure) {
                if (failure.getCause() instanceof IOException io) throw io;
                throw failure;
            }
            final var fileSystem = new FileSystemRegistry(() -> world).fromSaveDirectory(root, capacity, buffered);
            if (fileSystem == null) throw new IOException("Cannot open native volume " + root);
            return new Mount(root, address, fileSystem);
        }
    }

    private static Map<String, Entry> tree(CompoundTag root) throws IOException {
        final var result = new TreeMap<String, Entry>();
        collect(result, "", root, true);
        return result;
    }

    private record Entry(boolean directory, byte[] bytes, long modified) { }

    private static void collect(Map<String, Entry> entries, String path, CompoundTag tag, boolean root) throws IOException {
        final boolean directory = root || tag.getBoolean("isDirectory");
        if (root && tag.contains("isDirectory") && !tag.getBoolean("isDirectory")) throw new IOException("Inline volume root is not a directory");
        if (directory && tag.contains("data") || !directory && (!tag.contains("data", Tag.TAG_BYTE_ARRAY) || tag.contains("children"))) throw new IOException("Malformed inline volume entry");
        if (entries.putIfAbsent(path, new Entry(directory, directory ? new byte[0] : tag.getByteArray("data"), tag.getLong("lastModified"))) != null) throw new IOException("Duplicate inline volume path");
        if (!directory) return;
        if (!(tag.get("children") instanceof ListTag children) || !children.isEmpty() && children.getElementType() != Tag.TAG_COMPOUND) throw new IOException("Malformed inline directory children");
        for (int index = 0; index < children.size(); index++) {
            final var child = children.getCompound(index);
            final String name = child.getString("name");
            try {
                final var segments = FileSystemRegistry.pathSegments(name);
                if (segments.length != 1 || !segments[0].equals(name)) throw new IllegalArgumentException();
            } catch (IllegalArgumentException failure) { throw new IOException("Invalid inline volume filename", failure); }
            collect(entries, path.isEmpty() ? name : path + "/" + name, child, false);
        }
    }

    private void verifyExisting(Path target, Map<String, Entry> expected, boolean allowMissing) throws IOException {
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            if (allowMissing) return;
            throw new IOException("Native volume is missing");
        }
        if (!Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Native volume path is occupied");
        final var remaining = expected == null ? null : new java.util.HashSet<>(expected.keySet());
        try (var paths = Files.walk(target)) {
            for (final var path : paths.toList()) {
                checked(path);
                final boolean directory = Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS);
                if (!directory && !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Unsupported native volume entry");
                if (expected == null) continue;
                final String relative = target.relativize(path).toString().replace('\\', '/');
                final var entry = expected.get(relative);
                if (entry == null || entry.directory != directory || !directory && !Arrays.equals(entry.bytes, Files.readAllBytes(path))) throw new IOException("Native import destination conflicts with inline data");
                remaining.remove(relative);
            }
        }
        if (!allowMissing && remaining != null && !remaining.isEmpty()) throw new IOException("Native import destination differs from inline data");
    }

    private void importEntries(Path target, Path journals, Map<String, Entry> entries) throws IOException {
        Files.createDirectories(journals);
        for (final var pair : entries.entrySet()) {
            final var path = checked(target.resolve(pair.getKey()));
            final var entry = pair.getValue();
            if (entry.directory) Files.createDirectories(path);
            else if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                final var temporary = Files.createTempFile(journals, ".native-import-", ".tmp");
                try {
                    Files.write(temporary, entry.bytes);
                    Files.setLastModifiedTime(temporary, FileTime.fromMillis(entry.modified));
                    force(temporary);
                    if (!Files.getFileStore(temporary).equals(Files.getFileStore(path.getParent()))) throw new IOException("Native import staging and destination use different filesystems");
                    // No REPLACE_EXISTING: preserve files created outside this import.
                    Files.move(temporary, path);
                } finally { Files.deleteIfExists(temporary); }
            }
        }
        verifyExisting(target, entries, false);
        for (final var pair : entries.entrySet().stream().sorted(Map.Entry.<String, Entry>comparingByKey().reversed()).toList()) {
            if (pair.getValue().directory) Files.setLastModifiedTime(checked(target.resolve(pair.getKey())), FileTime.fromMillis(pair.getValue().modified));
        }
    }

    private Path checked(Path path) throws IOException {
        if (!path.normalize().startsWith(world)) throw new IOException("Native storage path escapes the world");
        Path current = world;
        final Path realWorld = Files.exists(world) ? world.toRealPath() : null;
        for (final var segment : world.relativize(path)) {
            current = current.resolve(segment);
            if (Files.isSymbolicLink(current) || realWorld != null && Files.exists(current, LinkOption.NOFOLLOW_LINKS) && !current.toRealPath().startsWith(realWorld)) throw new IOException("Linked path in native storage");
        }
        return path;
    }

    private static CompoundTag readJournal(Path path, String root) throws IOException {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.size(path) > 65536) throw new IOException("Invalid native import journal");
        try {
            final var journal = NbtIo.readCompressed(path, NbtAccounter.create(65536));
            if (journal.getInt("version") != 1 || !journal.getString("root").equals(root) || !journal.getString("digest").matches("[0-9a-f]{64}") || !journal.contains("complete", Tag.TAG_BYTE)) throw new IOException("Unrecognized native import journal");
            return journal;
        } catch (RuntimeException failure) { throw new IOException("Cannot read native import journal", failure); }
    }

    private static void writeJournal(Path path, String root, String digest, boolean complete, boolean replace) throws IOException {
        if (replace) {
            final var previous = readJournal(path, root);
            if (!previous.getString("digest").equals(digest)) throw new IOException("Native import journal changed");
        }
        final var journal = new CompoundTag(); journal.putInt("version", 1); journal.putString("root", root); journal.putString("digest", digest); journal.putBoolean("complete", complete);
        final var temporary = Files.createTempFile(path.getParent(), ".native-journal-", ".tmp");
        try {
            NbtIo.writeCompressed(journal, temporary); force(temporary);
            if (!replace) Files.move(temporary, path);
            else {
                try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                catch (AtomicMoveNotSupportedException failure) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
            }
        } finally { Files.deleteIfExists(temporary); }
    }

    private static void force(Path path) throws IOException {
        try (var channel = FileChannel.open(path, StandardOpenOption.WRITE)) { channel.force(true); }
    }

    private static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException failure) { throw new IllegalStateException(failure); }
    }

    private static String hash(String value) { return HexFormat.of().formatHex(sha256().digest(value.getBytes(StandardCharsets.UTF_8))); }

    private static String digest(Map<String, Entry> entries) throws IOException {
        final var hash = sha256();
        try (var output = new DataOutputStream(new DigestOutputStream(OutputStream.nullOutputStream(), hash))) {
            for (final var pair : entries.entrySet()) {
                final byte[] name = pair.getKey().getBytes(StandardCharsets.UTF_8); final var entry = pair.getValue();
                output.writeInt(name.length); output.write(name); output.writeBoolean(entry.directory); output.writeLong(entry.modified);
                output.writeInt(entry.bytes.length); output.write(entry.bytes);
            }
        }
        return HexFormat.of().formatHex(hash.digest());
    }
}
