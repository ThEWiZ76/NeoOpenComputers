package li.cil.oc.api.fs;

import li.cil.oc.api.Persistable;

import java.io.FileNotFoundException;

public interface FileSystem extends Persistable {
    boolean isReadOnly();

    long spaceTotal();

    long spaceUsed();

    boolean exists(String path);

    long size(String path);

    boolean isDirectory(String path);

    long lastModified(String path);

    String[] list(String path);

    boolean delete(String path);

    boolean makeDirectory(String path);

    boolean rename(String from, String to) throws FileNotFoundException;

    boolean setLastModified(String path, long time);

    int open(String path, Mode mode) throws FileNotFoundException;

    Handle getHandle(int handle);

    void close();
}
