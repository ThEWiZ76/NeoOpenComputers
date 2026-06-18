package li.cil.oc.api.fs;

import java.io.IOException;

/**
 * Handle to an opened file.
 */
public interface Handle {
    long position();

    long length();

    void close();

    int read(byte[] into) throws IOException;

    long seek(long to) throws IOException;

    void write(byte[] value) throws IOException;
}
