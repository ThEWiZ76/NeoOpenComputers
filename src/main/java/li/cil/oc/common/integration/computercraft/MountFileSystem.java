package li.cil.oc.common.integration.computercraft;

import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;
import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.api.fs.Handle;
import li.cil.oc.api.fs.Mode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Owns channels, never the underlying mount, which may be shared by several computers. */
public final class MountFileSystem implements FileSystem {
    private final Mount mount;
    private final WritableMount writable;
    private final Map<Integer, ChannelHandle> handles = new LinkedHashMap<>();
    private int nextHandle = 1;

    public MountFileSystem(Mount mount, boolean allowWrite) {
        this.mount = java.util.Objects.requireNonNull(mount);
        writable = allowWrite && mount instanceof WritableMount value ? value : null;
    }

    private static String clean(String path) throws FileNotFoundException {
        if (path == null || path.indexOf('\0') >= 0) throw new FileNotFoundException("invalid path");
        String result = dan200.computercraft.core.filesystem.FileSystem.sanitizePath(path.replace('\\', '/'), false);
        if (result.equals("..") || result.startsWith("../")) throw new FileNotFoundException("invalid path outside mount");
        return result;
    }
    private WritableMount writable() throws FileNotFoundException {
        if (writable == null) throw new FileNotFoundException("filesystem is read-only");
        return writable;
    }
    private static FileNotFoundException failure(IOException exception) {
        final var result = new FileNotFoundException(exception.getMessage()); result.initCause(exception); return result;
    }
    @Override public boolean isReadOnly() { return writable == null; }
    @Override public long spaceTotal() { return mount instanceof WritableMount value ? value.getCapacity() : 0; }
    @Override public long spaceUsed() {
        try { return mount instanceof WritableMount value ? Math.max(0, value.getCapacity() - value.getRemainingSpace()) : 0; }
        catch (IOException exception) { return 0; }
    }
    @Override public boolean exists(String path) { try { return mount.exists(clean(path)); } catch (IOException exception) { return false; } }
    @Override public boolean isDirectory(String path) { try { return mount.isDirectory(clean(path)); } catch (IOException exception) { return false; } }
    @Override public long size(String path) { try { return mount.getSize(clean(path)); } catch (IOException exception) { return 0; } }
    @Override public long lastModified(String path) { try { return mount.getAttributes(clean(path)).lastModifiedTime().toMillis(); } catch (IOException exception) { return 0; } }
    @Override public String[] list(String path) {
        try {
            final String directory = clean(path); final var contents = new ArrayList<String>(); mount.list(directory, contents);
            final var result = new ArrayList<String>();
            for (String name : contents) result.add(name + (mount.isDirectory(directory.isEmpty() ? name : directory + "/" + name) ? "/" : ""));
            return result.toArray(String[]::new);
        } catch (IOException exception) { return null; }
    }
    @Override public boolean delete(String path) { try { writable().delete(clean(path)); return true; } catch (IOException exception) { return false; } }
    @Override public boolean makeDirectory(String path) { try { writable().makeDirectory(clean(path)); return true; } catch (IOException exception) { return false; } }
    @Override public boolean rename(String from, String to) throws FileNotFoundException {
        try { writable().rename(clean(from), clean(to)); return true; } catch (IOException exception) { throw failure(exception); }
    }
    @Override public boolean setLastModified(String path, long time) { return false; }

    private SeekableByteChannel channel(String path, Mode mode, boolean restoring) throws IOException {
        if (mode == Mode.Read) return mount.openForRead(path);
        final Set<OpenOption> options = restoring
            ? Set.of(StandardOpenOption.WRITE)
            : mode == Mode.Append ? Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
            : Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return writable().openFile(path, options);
    }
    @Override public synchronized int open(String path, Mode mode) throws FileNotFoundException {
        try {
            final String normalized = clean(path);
            final var channel = channel(normalized, mode, false);
            try {
                if (mode == Mode.Append) channel.position(channel.size());
                while (nextHandle <= 0 || handles.containsKey(nextHandle)) nextHandle = nextHandle <= 0 ? 1 : nextHandle + 1;
                final int id = nextHandle++;
                handles.put(id, new ChannelHandle(id, normalized, mode, channel)); return id;
            } catch (IOException | RuntimeException exception) { channel.close(); throw exception; }
        } catch (IOException exception) { throw failure(exception); }
    }
    @Override public synchronized Handle getHandle(int handle) { return handles.get(handle); }
    @Override public synchronized void close() {
        for (var handle : new ArrayList<>(handles.values())) handle.close();
        handles.clear();
    }
    @Override public synchronized void save(CompoundTag nbt) {
        final var list = new ListTag();
        for (var handle : handles.values()) {
            final var tag = new CompoundTag(); tag.putInt("id", handle.id); tag.putString("path", handle.path);
            tag.putString("mode", handle.mode.name()); tag.putLong("position", handle.position()); list.add(tag);
        }
        nbt.put("handles", list);
    }
    @Override public synchronized void load(CompoundTag nbt) {
        close();
        final var list = nbt.getList("handles", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            final var tag = list.getCompound(i); final int id = tag.getInt("id");
            if (id <= 0 || handles.containsKey(id)) continue;
            try {
                final String path = clean(tag.getString("path")); final var mode = Mode.valueOf(tag.getString("mode"));
                final var channel = channel(path, mode, true);
                try {
                    channel.position(Math.max(0, tag.getLong("position")));
                    handles.put(id, new ChannelHandle(id, path, mode, channel));
                } catch (IOException | RuntimeException exception) { channel.close(); throw exception; }
            } catch (IOException | IllegalArgumentException exception) {
                // A vanished file or unsupported non-truncating mode invalidates the handle, never the file.
            }
        }
    }

    private final class ChannelHandle implements Handle {
        final int id; final String path; final Mode mode; final SeekableByteChannel channel;
        ChannelHandle(int id, String path, Mode mode, SeekableByteChannel channel) { this.id = id; this.path = path; this.mode = mode; this.channel = channel; }
        @Override public long position() { try { return channel.position(); } catch (IOException exception) { return 0; } }
        @Override public long length() { try { return channel.size(); } catch (IOException exception) { return 0; } }
        @Override public void close() {
            try { channel.close(); } catch (IOException ignored) { }
            synchronized (MountFileSystem.this) { handles.remove(id); }
        }
        @Override public int read(byte[] into) throws IOException {
            if (mode != Mode.Read) throw new IOException("file is not open for reading");
            return channel.read(ByteBuffer.wrap(into));
        }
        @Override public long seek(long to) throws IOException {
            if (to < 0) throw new IOException("negative seek offset");
            return channel.position(to).position();
        }
        @Override public void write(byte[] value) throws IOException {
            if (mode == Mode.Read) throw new IOException("file is read-only");
            final var bytes = ByteBuffer.wrap(value);
            while (bytes.hasRemaining()) if (channel.write(bytes) == 0) throw new IOException("mount did not accept write");
        }
    }
}
