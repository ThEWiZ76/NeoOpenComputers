package li.cil.oc.api;

import li.cil.oc.api.fs.Label;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;

public final class FileSystem {
    public static li.cil.oc.api.fs.FileSystem fromClass(final Class<?> clazz, final String domain, final String root) {
        if (API.fileSystem != null) {
            return API.fileSystem.fromClass(clazz, domain, root);
        }
        return null;
    }

    public static li.cil.oc.api.fs.FileSystem fromSaveDirectory(final String root, final long capacity, final boolean buffered) {
        if (API.fileSystem != null) {
            return API.fileSystem.fromSaveDirectory(root, capacity, buffered);
        }
        return null;
    }

    public static li.cil.oc.api.fs.FileSystem fromSaveDirectory(final String root, final long capacity) {
        return fromSaveDirectory(root, capacity, true);
    }

    public static li.cil.oc.api.fs.FileSystem fromMemory(final long capacity) {
        if (API.fileSystem != null) {
            return API.fileSystem.fromMemory(capacity);
        }
        return null;
    }

    public static li.cil.oc.api.fs.FileSystem asReadOnly(final li.cil.oc.api.fs.FileSystem fileSystem) {
        if (API.fileSystem != null) {
            return API.fileSystem.asReadOnly(fileSystem);
        }
        return null;
    }

    public static ManagedEnvironment asManagedEnvironment(final li.cil.oc.api.fs.FileSystem fileSystem, final Label label, final EnvironmentHost host, final String accessSound, final int speed) {
        if (API.fileSystem != null) {
            return API.fileSystem.asManagedEnvironment(fileSystem, label, host, accessSound, speed);
        }
        return null;
    }

    public static ManagedEnvironment asManagedEnvironment(final li.cil.oc.api.fs.FileSystem fileSystem, final String label, final EnvironmentHost host, final String accessSound, final int speed) {
        if (API.fileSystem != null) {
            return API.fileSystem.asManagedEnvironment(fileSystem, label, host, accessSound, speed);
        }
        return null;
    }

    public static ManagedEnvironment asManagedEnvironment(final li.cil.oc.api.fs.FileSystem fileSystem, final Label label, final EnvironmentHost host, final String accessSound) {
        return asManagedEnvironment(fileSystem, label, host, accessSound, 1);
    }

    public static ManagedEnvironment asManagedEnvironment(final li.cil.oc.api.fs.FileSystem fileSystem, final String label, final EnvironmentHost host, final String accessSound) {
        return asManagedEnvironment(fileSystem, label, host, accessSound, 1);
    }

    public static ManagedEnvironment asManagedEnvironment(final li.cil.oc.api.fs.FileSystem fileSystem, final Label label) {
        return asManagedEnvironment(fileSystem, label, null, null, 1);
    }

    public static ManagedEnvironment asManagedEnvironment(final li.cil.oc.api.fs.FileSystem fileSystem, final String label) {
        return asManagedEnvironment(fileSystem, label, null, null, 1);
    }

    public static ManagedEnvironment asManagedEnvironment(final li.cil.oc.api.fs.FileSystem fileSystem) {
        return asManagedEnvironment(fileSystem, (Label) null, null, null, 1);
    }

    private FileSystem() {
    }
}
