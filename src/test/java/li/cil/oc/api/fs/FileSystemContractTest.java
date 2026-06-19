package li.cil.oc.api.fs;

import li.cil.oc.api.Persistable;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FileSystemContractTest {
    @Test
    void exposesFileSystemMetadataDirectoryMutationAndHandleAccess() throws Exception {
        TestFileSystem fs = new TestFileSystem();

        int handle = fs.open("/file", Mode.Read);

        assertInstanceOf(Persistable.class, fs);
        assertEquals(false, fs.isReadOnly());
        assertEquals(1024, fs.spaceTotal());
        assertEquals(128, fs.spaceUsed());
        assertTrue(fs.exists("/"));
        assertEquals(4, fs.size("/file"));
        assertTrue(fs.isDirectory("/"));
        assertEquals(5, fs.lastModified("/file"));
        assertArrayEquals(new String[]{"file"}, fs.list("/"));
        assertTrue(fs.delete("/old"));
        assertTrue(fs.makeDirectory("/dir"));
        assertTrue(fs.rename("/from", "/to"));
        assertTrue(fs.setLastModified("/file", 6));
        assertEquals(fs.handle, fs.getHandle(handle));
        assertNull(fs.getHandle(999));
        fs.save(new CompoundTag());
        fs.load(new CompoundTag());
        fs.close();
        assertTrue(fs.closed);
    }

    private static final class TestFileSystem implements FileSystem {
        private final Map<Integer, Handle> handles = new HashMap<>();
        private final Handle handle = new TestHandle();
        private boolean closed;

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public long spaceTotal() {
            return 1024;
        }

        @Override
        public long spaceUsed() {
            return 128;
        }

        @Override
        public boolean exists(final String path) {
            return "/".equals(path) || "/file".equals(path);
        }

        @Override
        public long size(final String path) {
            return "/file".equals(path) ? 4 : 0;
        }

        @Override
        public boolean isDirectory(final String path) {
            return "/".equals(path);
        }

        @Override
        public long lastModified(final String path) {
            return "/file".equals(path) ? 5 : 0;
        }

        @Override
        public String[] list(final String path) {
            return "/".equals(path) ? new String[]{"file"} : null;
        }

        @Override
        public boolean delete(final String path) {
            return "/old".equals(path);
        }

        @Override
        public boolean makeDirectory(final String path) {
            return "/dir".equals(path);
        }

        @Override
        public boolean rename(final String from, final String to) throws FileNotFoundException {
            return "/from".equals(from) && "/to".equals(to);
        }

        @Override
        public boolean setLastModified(final String path, final long time) {
            return "/file".equals(path) && time == 6;
        }

        @Override
        public int open(final String path, final Mode mode) throws FileNotFoundException {
            handles.put(7, handle);
            return 7;
        }

        @Override
        public Handle getHandle(final int handle) {
            return handles.get(handle);
        }

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public void load(final CompoundTag nbt) {
        }

        @Override
        public void save(final CompoundTag nbt) {
        }
    }

    private static final class TestHandle implements Handle {
        @Override
        public long position() {
            return 0;
        }

        @Override
        public long length() {
            return 0;
        }

        @Override
        public int read(final byte[] into) {
            return into.length;
        }

        @Override
        public void write(final byte[] value) {
        }

        @Override
        public long seek(final long to) {
            return to;
        }

        @Override
        public void close() {
        }
    }
}
