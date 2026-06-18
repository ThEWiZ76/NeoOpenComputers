package li.cil.oc.api.fs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class HandleTest {
    @Test
    void exposesFileHandleOperations() throws IOException {
        Handle handle = new TestHandle();

        assertEquals(1L, handle.position());
        assertEquals(2L, handle.length());
        assertEquals(3, handle.read(new byte[0]));
        assertEquals(4L, handle.seek(4));
    }

    @Test
    void ioOperationsDeclareIOException() throws NoSuchMethodException {
        assertThrowsIOException(Handle.class.getMethod("read", byte[].class));
        assertThrowsIOException(Handle.class.getMethod("seek", long.class));
        assertThrowsIOException(Handle.class.getMethod("write", byte[].class));
    }

    private static void assertThrowsIOException(final Method method) {
        assertArrayEquals(new Class<?>[]{IOException.class}, method.getExceptionTypes());
    }

    private static final class TestHandle implements Handle {
        @Override
        public long position() {
            return 1;
        }

        @Override
        public long length() {
            return 2;
        }

        @Override
        public void close() {
        }

        @Override
        public int read(final byte[] into) {
            return 3;
        }

        @Override
        public long seek(final long to) {
            return to;
        }

        @Override
        public void write(final byte[] value) {
        }
    }
}
