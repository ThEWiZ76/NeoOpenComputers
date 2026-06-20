package li.cil.oc.common;

import li.cil.oc.api.detail.FileSystemAPI;
import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.api.fs.Handle;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.fs.Mode;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

final class FileSystemRegistry implements FileSystemAPI {
    @Override
    public FileSystem fromClass(final Class<?> clazz, final String domain, final String root) {
        final ClassLoader classLoader = clazz.getClassLoader();
        final String resourceRoot = "assets/" + domain + "/" + cleanRoot(root);
        final URL resource = classLoader == null
            ? ClassLoader.getSystemResource(resourceRoot)
            : classLoader.getResource(resourceRoot);
        if (resource == null) {
            return null;
        }
        try {
            return switch (resource.getProtocol()) {
                case "file", "union" -> fromPathResource(resource);
                case "jar" -> ResourceFileSystem.fromJar(resource);
                default -> null;
            };
        } catch (IOException | URISyntaxException | RuntimeException e) {
            return null;
        }
    }

    @Override
    public FileSystem fromSaveDirectory(final String root, final long capacity, final boolean buffered) {
        return new DirectoryFileSystem(Path.of(root), capacity);
    }

    @Override
    public FileSystem fromMemory(final long capacity) {
        return new MemoryFileSystem(capacity);
    }

    @Override
    public FileSystem asReadOnly(final FileSystem fileSystem) {
        if (fileSystem == null || fileSystem.isReadOnly()) {
            return fileSystem;
        }
        return new ReadOnlyFileSystem(fileSystem);
    }

    @Override
    public ManagedEnvironment asManagedEnvironment(final FileSystem fileSystem, final Label label, final EnvironmentHost host, final String accessSound, final int speed) {
        if (fileSystem == null) {
            return null;
        }
        return new FileSystemEnvironment(fileSystem, label, host, accessSound, speed);
    }

    @Override
    public ManagedEnvironment asManagedEnvironment(final FileSystem fileSystem, final String label, final EnvironmentHost host, final String accessSound, final int speed) {
        return asManagedEnvironment(fileSystem, new ReadOnlyLabel(label), host, accessSound, speed);
    }

    private static String cleanRoot(final String root) {
        if (root == null) {
            return "";
        }
        String clean = root.trim().replace('\\', '/');
        while (clean.startsWith("/")) {
            clean = clean.substring(1);
        }
        while (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean;
    }

    private static ResourceFileSystem fromPathResource(final URL resource) throws IOException, URISyntaxException {
        return ResourceFileSystem.fromDirectory(Path.of(resource.toURI()));
    }

    private static String[] pathSegments(final String path) {
        if (path == null || path.isEmpty()) {
            return new String[0];
        }
        if (path.indexOf('\\') >= 0 || path.indexOf(':') >= 0 || path.indexOf('*') >= 0 ||
            path.indexOf('?') >= 0 || path.indexOf('"') >= 0 || path.indexOf('<') >= 0 ||
            path.indexOf('>') >= 0 || path.indexOf('|') >= 0) {
            throw new IllegalArgumentException("path contains invalid characters");
        }
        return Arrays.stream(path.split("/"))
            .filter(segment -> !segment.isEmpty())
            .peek(segment -> {
                if (".".equals(segment) || "..".equals(segment)) {
                    throw new IllegalArgumentException("path contains invalid segments");
                }
            })
            .toArray(String[]::new);
    }

    private static final class ReadOnlyLabel implements Label {
        private static final String LABEL_TAG = "oc:fs.label";

        private final String label;

        private ReadOnlyLabel(final String label) {
            this.label = label;
        }

        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public void setLabel(final String value) {
            throw new IllegalArgumentException("label is read only");
        }

        @Override
        public void load(final CompoundTag nbt) {
        }

        @Override
        public void save(final CompoundTag nbt) {
            if (label != null) {
                nbt.putString(LABEL_TAG, label);
            }
        }
    }

    private static final class ReadOnlyFileSystem implements FileSystem {
        private final FileSystem inner;

        private ReadOnlyFileSystem(final FileSystem inner) {
            this.inner = inner;
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }

        @Override
        public long spaceTotal() {
            return inner.spaceTotal();
        }

        @Override
        public long spaceUsed() {
            return inner.spaceUsed();
        }

        @Override
        public boolean exists(final String path) {
            return inner.exists(path);
        }

        @Override
        public long size(final String path) {
            return inner.size(path);
        }

        @Override
        public boolean isDirectory(final String path) {
            return inner.isDirectory(path);
        }

        @Override
        public long lastModified(final String path) {
            return inner.lastModified(path);
        }

        @Override
        public String[] list(final String path) {
            return inner.list(path);
        }

        @Override
        public boolean delete(final String path) {
            return false;
        }

        @Override
        public boolean makeDirectory(final String path) {
            return false;
        }

        @Override
        public boolean rename(final String from, final String to) {
            return false;
        }

        @Override
        public boolean setLastModified(final String path, final long time) {
            return false;
        }

        @Override
        public int open(final String path, final Mode mode) throws FileNotFoundException {
            if (mode == Mode.Read) {
                return inner.open(path, mode);
            }
            throw new FileNotFoundException("read-only filesystem; cannot open for " + mode.name().toLowerCase() + ": " + path);
        }

        @Override
        public Handle getHandle(final int handle) {
            return inner.getHandle(handle);
        }

        @Override
        public void close() {
            inner.close();
        }

        @Override
        public void load(final CompoundTag nbt) {
            inner.load(nbt);
        }

        @Override
        public void save(final CompoundTag nbt) {
            inner.save(nbt);
        }
    }

    private static final class ResourceFileSystem implements FileSystem {
        private static final String INPUT_TAG = "input";
        private static final String HANDLE_TAG = "handle";
        private static final String PATH_TAG = "path";
        private static final String POSITION_TAG = "position";

        private final ResourceDirectory root = new ResourceDirectory(0);
        private final Map<Integer, ResourceHandle> handles = new LinkedHashMap<>();
        private int nextHandle = 1;

        private static ResourceFileSystem fromDirectory(final Path rootPath) throws IOException {
            if (!Files.isDirectory(rootPath)) {
                return null;
            }
            final ResourceFileSystem fileSystem = new ResourceFileSystem();
            try (Stream<Path> stream = Files.walk(rootPath)) {
                for (Path path : stream.filter(path -> !path.equals(rootPath)).toList()) {
                    final String relativePath = rootPath.relativize(path).toString().replace('\\', '/');
                    if (Files.isDirectory(path)) {
                        fileSystem.addDirectory(relativePath, Files.getLastModifiedTime(path).toMillis());
                    } else if (Files.isRegularFile(path)) {
                        fileSystem.addFile(relativePath, Files.readAllBytes(path), Files.getLastModifiedTime(path).toMillis());
                    }
                }
            }
            return fileSystem;
        }

        private static ResourceFileSystem fromJar(final URL resource) throws IOException {
            final JarURLConnection connection = (JarURLConnection) resource.openConnection();
            connection.setUseCaches(false);
            final String rootEntry = connection.getEntryName().replace('\\', '/').replaceAll("/+$", "") + "/";
            final ResourceFileSystem fileSystem = new ResourceFileSystem();
            try (JarFile jarFile = connection.getJarFile()) {
                final java.util.Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    final JarEntry entry = entries.nextElement();
                    final String entryName = entry.getName().replace('\\', '/');
                    if (!entryName.startsWith(rootEntry) || entryName.equals(rootEntry)) {
                        continue;
                    }
                    final String relativePath = entryName.substring(rootEntry.length()).replaceAll("/+$", "");
                    if (relativePath.isEmpty()) {
                        continue;
                    }
                    if (entry.isDirectory()) {
                        fileSystem.addDirectory(relativePath, entry.getTime());
                    } else {
                        try (InputStream input = jarFile.getInputStream(entry)) {
                            fileSystem.addFile(relativePath, input.readAllBytes(), entry.getTime());
                        }
                    }
                }
            }
            return fileSystem;
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }

        @Override
        public long spaceTotal() {
            return spaceUsed();
        }

        @Override
        public long spaceUsed() {
            return root.spaceUsed();
        }

        @Override
        public boolean exists(final String path) {
            return find(path) != null;
        }

        @Override
        public long size(final String path) {
            final ResourceEntry entry = find(path);
            return entry instanceof ResourceFile file ? file.data.length : 0;
        }

        @Override
        public boolean isDirectory(final String path) {
            return find(path) instanceof ResourceDirectory;
        }

        @Override
        public long lastModified(final String path) {
            final ResourceEntry entry = find(path);
            return entry == null ? 0 : entry.lastModified;
        }

        @Override
        public String[] list(final String path) {
            final ResourceEntry entry = find(path);
            if (!(entry instanceof ResourceDirectory directory)) {
                return null;
            }
            return directory.children.entrySet().stream()
                .map(child -> child.getValue() instanceof ResourceDirectory ? child.getKey() + "/" : child.getKey())
                .sorted()
                .toArray(String[]::new);
        }

        @Override
        public boolean delete(final String path) {
            return false;
        }

        @Override
        public boolean makeDirectory(final String path) {
            return false;
        }

        @Override
        public boolean rename(final String from, final String to) {
            return false;
        }

        @Override
        public boolean setLastModified(final String path, final long time) {
            return false;
        }

        @Override
        public int open(final String path, final Mode mode) throws FileNotFoundException {
            if (mode != Mode.Read) {
                throw new FileNotFoundException("read-only filesystem; cannot open for " + mode.name().toLowerCase() + ": " + path);
            }
            final ResourceEntry entry = find(path);
            if (!(entry instanceof ResourceFile file)) {
                throw new FileNotFoundException(path);
            }
            final int id = newHandleId();
            handles.put(id, new ResourceHandle(id, path, file.data));
            return id;
        }

        @Override
        public Handle getHandle(final int handle) {
            return handles.get(handle);
        }

        @Override
        public void close() {
            for (ResourceHandle handle : handles.values().toArray(ResourceHandle[]::new)) {
                handle.close();
            }
            handles.clear();
        }

        @Override
        public void load(final CompoundTag nbt) {
            handles.clear();
            nextHandle = 1;
            final ListTag savedHandles = nbt.getList(INPUT_TAG, Tag.TAG_COMPOUND);
            for (int index = 0; index < savedHandles.size(); index++) {
                final CompoundTag savedHandle = savedHandles.getCompound(index);
                final ResourceEntry entry = find(savedHandle.getString(PATH_TAG));
                if (entry instanceof ResourceFile file) {
                    final int handle = savedHandle.getInt(HANDLE_TAG);
                    final ResourceHandle loadedHandle = new ResourceHandle(handle, savedHandle.getString(PATH_TAG), file.data);
                    loadedHandle.position = Math.min(savedHandle.getLong(POSITION_TAG), file.data.length);
                    handles.put(handle, loadedHandle);
                    nextHandle = Math.max(nextHandle, handle + 1);
                }
            }
        }

        @Override
        public void save(final CompoundTag nbt) {
            final ListTag savedHandles = new ListTag();
            for (ResourceHandle handle : handles.values()) {
                if (!handle.closed) {
                    final CompoundTag savedHandle = new CompoundTag();
                    savedHandle.putInt(HANDLE_TAG, handle.id);
                    savedHandle.putString(PATH_TAG, handle.path);
                    savedHandle.putLong(POSITION_TAG, handle.position);
                    savedHandles.add(savedHandle);
                }
            }
            nbt.put(INPUT_TAG, savedHandles);
        }

        private ResourceEntry find(final String path) {
            ResourceEntry current = root;
            for (String segment : pathSegments(path)) {
                if (!(current instanceof ResourceDirectory directory)) {
                    return null;
                }
                current = directory.children.get(segment);
                if (current == null) {
                    return null;
                }
            }
            return current;
        }

        private void addDirectory(final String path, final long lastModified) {
            directory(pathSegments(path), lastModified);
        }

        private void addFile(final String path, final byte[] data, final long lastModified) {
            final String[] segments = pathSegments(path);
            if (segments.length == 0) {
                return;
            }
            final ResourceDirectory parent = directory(Arrays.copyOf(segments, segments.length - 1), lastModified);
            parent.children.put(segments[segments.length - 1], new ResourceFile(data, lastModified));
        }

        private ResourceDirectory directory(final String[] segments, final long lastModified) {
            ResourceDirectory current = root;
            for (String segment : segments) {
                final ResourceEntry child = current.children.get(segment);
                if (child instanceof ResourceDirectory childDirectory) {
                    current = childDirectory;
                } else {
                    final ResourceDirectory directory = new ResourceDirectory(lastModified);
                    current.children.put(segment, directory);
                    current = directory;
                }
            }
            return current;
        }

        private int newHandleId() {
            while (handles.containsKey(nextHandle)) {
                nextHandle++;
            }
            return nextHandle++;
        }

        private abstract static class ResourceEntry {
            private final long lastModified;

            private ResourceEntry(final long lastModified) {
                this.lastModified = lastModified;
            }

            abstract long spaceUsed();
        }

        private static final class ResourceDirectory extends ResourceEntry {
            private final Map<String, ResourceEntry> children = new LinkedHashMap<>();

            private ResourceDirectory(final long lastModified) {
                super(lastModified);
            }

            @Override
            long spaceUsed() {
                return children.values().stream().mapToLong(ResourceEntry::spaceUsed).sum();
            }
        }

        private static final class ResourceFile extends ResourceEntry {
            private final byte[] data;

            private ResourceFile(final byte[] data, final long lastModified) {
                super(lastModified);
                this.data = data;
            }

            @Override
            long spaceUsed() {
                return data.length;
            }
        }

        private final class ResourceHandle implements Handle {
            private final int id;
            private final String path;
            private final byte[] data;
            private boolean closed;
            private long position;

            private ResourceHandle(final int id, final String path, final byte[] data) {
                this.id = id;
                this.path = path;
                this.data = data;
            }

            @Override
            public long position() {
                return position;
            }

            @Override
            public long length() {
                return data.length;
            }

            @Override
            public void close() {
                if (!closed) {
                    closed = true;
                    handles.remove(id);
                }
            }

            @Override
            public int read(final byte[] into) throws IOException {
                checkOpen();
                if (position >= data.length) {
                    return -1;
                }
                final int count = Math.min(into.length, data.length - (int) position);
                System.arraycopy(data, (int) position, into, 0, count);
                position += count;
                return count;
            }

            @Override
            public long seek(final long to) throws IOException {
                checkOpen();
                if (to < 0) {
                    throw new IOException("invalid offset");
                }
                position = Math.min(to, data.length);
                return position;
            }

            @Override
            public void write(final byte[] value) throws IOException {
                throw new IOException("bad file descriptor");
            }

            private void checkOpen() throws IOException {
                if (closed) {
                    throw new IOException("file is closed");
                }
            }
        }
    }

    private static final class DirectoryFileSystem implements FileSystem {
        private final Path root;
        private final long capacity;
        private final Map<Integer, DirectoryHandle> handles = new LinkedHashMap<>();
        private int nextHandle = 1;

        private DirectoryFileSystem(final Path root, final long capacity) {
            try {
                this.root = root.toAbsolutePath().normalize();
                this.capacity = capacity;
                Files.createDirectories(this.root);
            } catch (IOException e) {
                throw new IllegalArgumentException("invalid save directory: " + root, e);
            }
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public long spaceTotal() {
            return capacity;
        }

        @Override
        public long spaceUsed() {
            try (Stream<Path> stream = Files.walk(root)) {
                return stream
                    .filter(Files::isRegularFile)
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException ignored) {
                            return 0;
                        }
                    })
                    .sum();
            } catch (IOException ignored) {
                return 0;
            }
        }

        @Override
        public boolean exists(final String path) {
            return Files.exists(resolve(path));
        }

        @Override
        public long size(final String path) {
            final Path resolved = resolve(path);
            try {
                return Files.isRegularFile(resolved) ? Files.size(resolved) : 0;
            } catch (IOException ignored) {
                return 0;
            }
        }

        @Override
        public boolean isDirectory(final String path) {
            return Files.isDirectory(resolve(path));
        }

        @Override
        public long lastModified(final String path) {
            try {
                return Files.getLastModifiedTime(resolve(path)).toMillis();
            } catch (IOException ignored) {
                return 0;
            }
        }

        @Override
        public String[] list(final String path) {
            final Path directory = resolve(path);
            if (!Files.isDirectory(directory)) {
                return null;
            }
            final List<String> entries = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
                for (Path child : stream) {
                    final String name = child.getFileName().toString();
                    entries.add(Files.isDirectory(child) ? name + "/" : name);
                }
            } catch (IOException e) {
                return null;
            }
            entries.sort(Comparator.naturalOrder());
            return entries.toArray(String[]::new);
        }

        @Override
        public boolean delete(final String path) {
            final Path resolved = resolve(path);
            if (resolved.equals(root)) {
                return true;
            }
            try {
                return Files.deleteIfExists(resolved);
            } catch (DirectoryNotEmptyException e) {
                return false;
            } catch (IOException e) {
                return false;
            }
        }

        @Override
        public boolean makeDirectory(final String path) {
            final Path resolved = resolve(path);
            if (resolved.equals(root) || Files.exists(resolved)) {
                return false;
            }
            try {
                Files.createDirectory(resolved);
                return true;
            } catch (IOException e) {
                return false;
            }
        }

        @Override
        public boolean rename(final String from, final String to) throws FileNotFoundException {
            final Path source = resolve(from);
            final Path target = resolve(to);
            if (source.equals(root) || !Files.exists(source) || target.equals(root)) {
                throw new FileNotFoundException(from);
            }
            try {
                if (Files.exists(target) && Files.isDirectory(target) && !isEmptyDirectory(target)) {
                    return false;
                }
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (IOException e) {
                return false;
            }
        }

        @Override
        public boolean setLastModified(final String path, final long time) {
            if (time < 0) {
                return false;
            }
            try {
                Files.setLastModifiedTime(resolve(path), FileTime.fromMillis(time));
                return true;
            } catch (IOException e) {
                return false;
            }
        }

        @Override
        public int open(final String path, final Mode mode) throws FileNotFoundException {
            final Path resolved = resolve(path);
            if (resolved.equals(root)) {
                throw new FileNotFoundException(path);
            }
            return switch (mode) {
                case Read -> openRead(path, resolved);
                case Write, Append -> openWrite(path, resolved, mode);
            };
        }

        @Override
        public Handle getHandle(final int handle) {
            return handles.get(handle);
        }

        @Override
        public void close() {
            for (DirectoryHandle handle : handles.values().toArray(DirectoryHandle[]::new)) {
                handle.close();
            }
            handles.clear();
        }

        @Override
        public void load(final CompoundTag nbt) {
        }

        @Override
        public void save(final CompoundTag nbt) {
        }

        private int openRead(final String originalPath, final Path resolved) throws FileNotFoundException {
            if (!Files.isRegularFile(resolved)) {
                throw new FileNotFoundException(originalPath);
            }
            return openHandle(resolved, false, 0);
        }

        private int openWrite(final String originalPath, final Path resolved, final Mode mode) throws FileNotFoundException {
            if (!Files.isDirectory(resolved.getParent()) || Files.isDirectory(resolved)) {
                throw new FileNotFoundException(originalPath);
            }
            if (!Files.exists(resolved) && capacity >= 0 && spaceUsed() >= capacity) {
                throw new FileNotFoundException(originalPath);
            }
            final DirectoryHandle handle = createHandle(resolved, true);
            try {
                if (mode == Mode.Write) {
                    handle.file.setLength(0);
                    handle.file.seek(0);
                } else {
                    handle.file.seek(handle.file.length());
                }
            } catch (IOException e) {
                handle.close();
                throw new FileNotFoundException(originalPath);
            }
            handles.put(handle.id, handle);
            return handle.id;
        }

        private int openHandle(final Path path, final boolean writable, final long position) throws FileNotFoundException {
            final DirectoryHandle handle = createHandle(path, writable);
            try {
                handle.seek(position);
            } catch (IOException e) {
                handle.close();
                throw new FileNotFoundException(path.toString());
            }
            handles.put(handle.id, handle);
            return handle.id;
        }

        private DirectoryHandle createHandle(final Path path, final boolean writable) throws FileNotFoundException {
            try {
                final int id = newHandleId();
                return new DirectoryHandle(id, path, new RandomAccessFile(path.toFile(), writable ? "rw" : "r"), writable);
            } catch (IOException e) {
                throw new FileNotFoundException(path.toString());
            }
        }

        private int newHandleId() {
            while (handles.containsKey(nextHandle)) {
                nextHandle++;
            }
            return nextHandle++;
        }

        private boolean isEmptyDirectory(final Path path) throws IOException {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                return !stream.iterator().hasNext();
            }
        }

        private Path resolve(final String path) {
            Path resolved = root;
            for (String segment : pathSegments(path)) {
                resolved = resolved.resolve(segment);
            }
            resolved = resolved.normalize();
            if (!resolved.startsWith(root)) {
                throw new IllegalArgumentException("path escapes filesystem root");
            }
            return resolved;
        }

        private final class DirectoryHandle implements Handle {
            private final int id;
            private final Path path;
            private final RandomAccessFile file;
            private final boolean writable;
            private boolean closed;

            private DirectoryHandle(final int id, final Path path, final RandomAccessFile file, final boolean writable) {
                this.id = id;
                this.path = path;
                this.file = file;
                this.writable = writable;
            }

            @Override
            public long position() {
                try {
                    return file.getFilePointer();
                } catch (IOException ignored) {
                    return 0;
                }
            }

            @Override
            public long length() {
                try {
                    return file.length();
                } catch (IOException ignored) {
                    return 0;
                }
            }

            @Override
            public void close() {
                if (!closed) {
                    closed = true;
                    handles.remove(id);
                    try {
                        file.close();
                    } catch (IOException ignored) {
                    }
                }
            }

            @Override
            public int read(final byte[] into) throws IOException {
                checkOpen();
                if (writable) {
                    throw new IOException("bad file descriptor");
                }
                return file.read(into);
            }

            @Override
            public long seek(final long to) throws IOException {
                checkOpen();
                if (to < 0) {
                    throw new IOException("invalid offset");
                }
                file.seek(to);
                return file.getFilePointer();
            }

            @Override
            public void write(final byte[] value) throws IOException {
                checkOpen();
                if (!writable) {
                    throw new IOException("bad file descriptor");
                }
                final long currentLength = file.length();
                final long end = file.getFilePointer() + value.length;
                final long growth = Math.max(0L, end - currentLength);
                if (capacity >= 0 && capacity - spaceUsed() < growth) {
                    throw new IOException("not enough space");
                }
                file.write(value);
                Files.setLastModifiedTime(path, FileTime.fromMillis(System.currentTimeMillis()));
            }

            private void checkOpen() throws IOException {
                if (closed) {
                    throw new IOException("file is closed");
                }
            }
        }
    }

    private static final class MemoryFileSystem implements FileSystem {
        private static final String CHILDREN_TAG = "children";
        private static final String DATA_TAG = "data";
        private static final String DIRECTORY_TAG = "isDirectory";
        private static final String LAST_MODIFIED_TAG = "lastModified";
        private static final String NAME_TAG = "name";
        private static final String INPUT_TAG = "input";
        private static final String OUTPUT_TAG = "output";
        private static final String HANDLE_TAG = "handle";
        private static final String PATH_TAG = "path";
        private static final String POSITION_TAG = "position";
        private static final String CAPACITY_USED_TAG = "capacity.used";

        private final Directory root = new Directory();
        private final long capacity;
        private final Map<Integer, OpenHandle> handles = new LinkedHashMap<>();
        private int nextHandle = 1;

        private MemoryFileSystem(final long capacity) {
            this.capacity = capacity;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public long spaceTotal() {
            return capacity;
        }

        @Override
        public long spaceUsed() {
            return root.spaceUsed();
        }

        @Override
        public boolean exists(final String path) {
            return find(path) != null;
        }

        @Override
        public long size(final String path) {
            final Entry entry = find(path);
            return entry == null ? 0 : entry.size();
        }

        @Override
        public boolean isDirectory(final String path) {
            return find(path) instanceof Directory;
        }

        @Override
        public long lastModified(final String path) {
            final Entry entry = find(path);
            return entry == null ? 0 : entry.lastModified;
        }

        @Override
        public String[] list(final String path) {
            final Entry entry = find(path);
            if (!(entry instanceof Directory directory)) {
                return null;
            }
            return directory.children.entrySet().stream()
                .map(child -> child.getValue() instanceof Directory ? child.getKey() + "/" : child.getKey())
                .toArray(String[]::new);
        }

        @Override
        public boolean delete(final String path) {
            final String[] segments = segments(path);
            if (segments.length == 0) {
                return true;
            }
            final Directory parent = parent(segments);
            if (parent == null) {
                return false;
            }
            final Entry child = parent.children.get(segments[segments.length - 1]);
            if (child == null || !child.canDelete()) {
                return false;
            }
            parent.children.remove(segments[segments.length - 1]);
            parent.touch();
            return true;
        }

        @Override
        public boolean makeDirectory(final String path) {
            final String[] segments = segments(path);
            if (segments.length == 0 || spaceUsed() >= capacity) {
                return false;
            }
            final Directory parent = parent(segments);
            if (parent == null || parent.children.containsKey(segments[segments.length - 1])) {
                return false;
            }
            parent.children.put(segments[segments.length - 1], new Directory());
            parent.touch();
            return true;
        }

        @Override
        public boolean rename(final String from, final String to) throws FileNotFoundException {
            final String[] fromSegments = segments(from);
            if (fromSegments.length == 0 || !exists(from)) {
                throw new FileNotFoundException(from);
            }
            final String[] toSegments = segments(to);
            final Directory fromParent = parent(fromSegments);
            final Directory toParent = parent(toSegments);
            if (fromParent == null || toParent == null || toSegments.length == 0) {
                return false;
            }

            final String fromName = fromSegments[fromSegments.length - 1];
            final String toName = toSegments[toSegments.length - 1];
            final Entry oldTarget = toParent.children.get(toName);
            if (oldTarget != null && !oldTarget.canDelete()) {
                return false;
            }

            final Entry moved = fromParent.children.remove(fromName);
            if (moved == null) {
                throw new FileNotFoundException(from);
            }
            if (oldTarget != null) {
                toParent.children.remove(toName);
            }
            toParent.children.put(toName, moved);
            fromParent.touch();
            toParent.touch();
            moved.touch();
            return true;
        }

        @Override
        public boolean setLastModified(final String path, final long time) {
            final Entry entry = find(path);
            if (entry == null || time < 0) {
                return false;
            }
            entry.lastModified = time;
            return true;
        }

        @Override
        public int open(final String path, final Mode mode) throws FileNotFoundException {
            return switch (mode) {
                case Read -> openRead(path);
                case Write, Append -> openWrite(path, mode);
            };
        }

        @Override
        public Handle getHandle(final int handle) {
            return handles.get(handle);
        }

        @Override
        public void close() {
            for (OpenHandle handle : handles.values().toArray(OpenHandle[]::new)) {
                handle.close();
            }
            handles.clear();
        }

        @Override
        public void load(final CompoundTag nbt) {
            root.children.clear();
            root.load(nbt);
            handles.clear();
            nextHandle = 1;
            loadInputHandles(nbt);
            loadOutputHandles(nbt);
        }

        @Override
        public void save(final CompoundTag nbt) {
            root.save(nbt);
            saveHandles(nbt, INPUT_TAG, false);
            saveHandles(nbt, OUTPUT_TAG, true);
            nbt.putLong(CAPACITY_USED_TAG, spaceUsed());
        }

        private int openRead(final String path) throws FileNotFoundException {
            final Entry entry = find(path);
            if (!(entry instanceof File file)) {
                throw new FileNotFoundException(path);
            }
            final int id = newHandleId();
            handles.put(id, new OpenHandle(id, file, false, 0));
            return id;
        }

        private int openWrite(final String path, final Mode mode) throws FileNotFoundException {
            final String[] segments = segments(path);
            if (segments.length == 0) {
                throw new FileNotFoundException(path);
            }
            final Directory parent = parent(segments);
            if (parent == null) {
                throw new FileNotFoundException(path);
            }

            final String fileName = segments[segments.length - 1];
            final Entry existing = parent.children.get(fileName);
            if (existing instanceof Directory) {
                throw new FileNotFoundException(path);
            }

            final File file;
            if (existing instanceof File existingFile) {
                file = existingFile;
                if (mode == Mode.Write) {
                    file.data = new byte[0];
                    file.touch();
                }
            } else {
                if (spaceUsed() >= capacity) {
                    throw new FileNotFoundException(path);
                }
                file = new File();
                parent.children.put(fileName, file);
                parent.touch();
            }

            final int id = newHandleId();
            handles.put(id, new OpenHandle(id, file, true, mode == Mode.Append ? file.data.length : 0));
            return id;
        }

        private void loadInputHandles(final CompoundTag nbt) {
            final ListTag savedHandles = nbt.getList(INPUT_TAG, Tag.TAG_COMPOUND);
            for (int index = 0; index < savedHandles.size(); index++) {
                final CompoundTag savedHandle = savedHandles.getCompound(index);
                final Entry entry = find(savedHandle.getString(PATH_TAG));
                if (entry instanceof File file) {
                    final int handle = savedHandle.getInt(HANDLE_TAG);
                    handles.put(handle, new OpenHandle(handle, file, false, savedHandle.getLong(POSITION_TAG)));
                    nextHandle = Math.max(nextHandle, handle + 1);
                }
            }
        }

        private void loadOutputHandles(final CompoundTag nbt) {
            final ListTag savedHandles = nbt.getList(OUTPUT_TAG, Tag.TAG_COMPOUND);
            for (int index = 0; index < savedHandles.size(); index++) {
                final CompoundTag savedHandle = savedHandles.getCompound(index);
                final Entry entry = find(savedHandle.getString(PATH_TAG));
                if (entry instanceof File file) {
                    final int handle = savedHandle.getInt(HANDLE_TAG);
                    handles.put(handle, new OpenHandle(handle, file, true, savedHandle.getLong(POSITION_TAG)));
                    nextHandle = Math.max(nextHandle, handle + 1);
                }
            }
        }

        private void saveHandles(final CompoundTag nbt, final String tagName, final boolean writable) {
            final ListTag savedHandles = new ListTag();
            for (OpenHandle handle : handles.values()) {
                if (handle.writable == writable && !handle.closed) {
                    final CompoundTag savedHandle = new CompoundTag();
                    savedHandle.putInt(HANDLE_TAG, handle.id);
                    savedHandle.putString(PATH_TAG, pathTo(handle.file));
                    savedHandle.putLong(POSITION_TAG, handle.position);
                    savedHandles.add(savedHandle);
                }
            }
            nbt.put(tagName, savedHandles);
        }

        private int newHandleId() {
            while (handles.containsKey(nextHandle)) {
                nextHandle++;
            }
            return nextHandle++;
        }

        private Entry find(final String path) {
            Entry current = root;
            for (String segment : segments(path)) {
                if (!(current instanceof Directory directory)) {
                    return null;
                }
                current = directory.children.get(segment);
                if (current == null) {
                    return null;
                }
            }
            return current;
        }

        private Directory parent(final String[] segments) {
            Entry current = root;
            for (int index = 0; index < segments.length - 1; index++) {
                current = current instanceof Directory directory ? directory.children.get(segments[index]) : null;
                if (current == null) {
                    return null;
                }
            }
            return current instanceof Directory directory ? directory : null;
        }

        private String[] segments(final String path) {
            if (path == null || path.isEmpty()) {
                return new String[0];
            }
            if (path.indexOf('\\') >= 0 || path.indexOf(':') >= 0 || path.indexOf('*') >= 0 ||
                path.indexOf('?') >= 0 || path.indexOf('"') >= 0 || path.indexOf('<') >= 0 ||
                path.indexOf('>') >= 0 || path.indexOf('|') >= 0) {
                throw new IllegalArgumentException("path contains invalid characters");
            }
            return Arrays.stream(path.split("/"))
                .filter(segment -> !segment.isEmpty())
                .toArray(String[]::new);
        }

        private String pathTo(final File file) {
            final String path = pathTo(root, file, "");
            return path == null ? "" : path;
        }

        private String pathTo(final Directory directory, final File file, final String prefix) {
            for (Map.Entry<String, Entry> child : directory.children.entrySet()) {
                final String childPath = prefix + child.getKey();
                if (child.getValue() == file) {
                    return childPath;
                }
                if (child.getValue() instanceof Directory childDirectory) {
                    final String nested = pathTo(childDirectory, file, childPath + "/");
                    if (nested != null) {
                        return nested;
                    }
                }
            }
            return null;
        }

        private abstract static class Entry {
            private long lastModified = System.currentTimeMillis();

            abstract long size();

            abstract long spaceUsed();

            abstract boolean canDelete();

            void touch() {
                lastModified = System.currentTimeMillis();
            }

            void load(final CompoundTag nbt) {
                if (nbt.contains(LAST_MODIFIED_TAG)) {
                    lastModified = nbt.getLong(LAST_MODIFIED_TAG);
                }
            }

            void save(final CompoundTag nbt) {
                nbt.putLong(LAST_MODIFIED_TAG, lastModified);
            }
        }

        private static final class Directory extends Entry {
            private final Map<String, Entry> children = new LinkedHashMap<>();

            @Override
            long size() {
                return 0;
            }

            @Override
            long spaceUsed() {
                return children.values().stream().mapToLong(Entry::spaceUsed).sum();
            }

            @Override
            boolean canDelete() {
                return children.isEmpty();
            }

            @Override
            void load(final CompoundTag nbt) {
                super.load(nbt);
                children.clear();
                final ListTag savedChildren = nbt.getList(CHILDREN_TAG, Tag.TAG_COMPOUND);
                for (int index = 0; index < savedChildren.size(); index++) {
                    final CompoundTag savedChild = savedChildren.getCompound(index);
                    final Entry child = savedChild.getBoolean(DIRECTORY_TAG) ? new Directory() : new File();
                    child.load(savedChild);
                    children.put(savedChild.getString(NAME_TAG), child);
                }
            }

            @Override
            void save(final CompoundTag nbt) {
                super.save(nbt);
                final ListTag savedChildren = new ListTag();
                for (Map.Entry<String, Entry> child : children.entrySet()) {
                    final CompoundTag savedChild = new CompoundTag();
                    savedChild.putBoolean(DIRECTORY_TAG, child.getValue() instanceof Directory);
                    savedChild.putString(NAME_TAG, child.getKey());
                    child.getValue().save(savedChild);
                    savedChildren.add(savedChild);
                }
                nbt.put(CHILDREN_TAG, savedChildren);
            }
        }

        private static final class File extends Entry {
            private byte[] data = new byte[0];

            @Override
            long size() {
                return data.length;
            }

            @Override
            long spaceUsed() {
                return data.length;
            }

            @Override
            boolean canDelete() {
                return true;
            }

            @Override
            void load(final CompoundTag nbt) {
                super.load(nbt);
                data = nbt.getByteArray(DATA_TAG);
            }

            @Override
            void save(final CompoundTag nbt) {
                super.save(nbt);
                nbt.putByteArray(DATA_TAG, data);
            }
        }

        private final class OpenHandle implements Handle {
            private final int id;
            private final File file;
            private final boolean writable;
            private boolean closed;
            private long position;

            private OpenHandle(final int id, final File file, final boolean writable, final long position) {
                this.id = id;
                this.file = file;
                this.writable = writable;
                this.position = position;
            }

            @Override
            public long position() {
                return position;
            }

            @Override
            public long length() {
                return file.data.length;
            }

            @Override
            public void close() {
                if (!closed) {
                    closed = true;
                    handles.remove(id);
                }
            }

            @Override
            public int read(final byte[] into) throws IOException {
                checkOpen();
                if (writable) {
                    throw new IOException("bad file descriptor");
                }
                if (position >= file.data.length) {
                    return -1;
                }
                final int count = Math.min(into.length, file.data.length - (int) position);
                System.arraycopy(file.data, (int) position, into, 0, count);
                position += count;
                return count;
            }

            @Override
            public long seek(final long to) throws IOException {
                checkOpen();
                if (to < 0) {
                    throw new IOException("invalid offset");
                }
                position = to;
                return position;
            }

            @Override
            public void write(final byte[] value) throws IOException {
                checkOpen();
                if (!writable) {
                    throw new IOException("bad file descriptor");
                }
                final int end = Math.toIntExact(position + value.length);
                final long growth = Math.max(0L, (long) end - file.data.length);
                if (capacity - spaceUsed() < growth) {
                    throw new IOException("not enough space");
                }
                if (end > file.data.length) {
                    file.data = Arrays.copyOf(file.data, end);
                }
                System.arraycopy(value, 0, file.data, (int) position, value.length);
                position = end;
                file.touch();
            }

            private void checkOpen() throws IOException {
                if (closed) {
                    throw new IOException("file is closed");
                }
            }
        }
    }
}
