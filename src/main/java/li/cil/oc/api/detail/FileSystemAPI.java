package li.cil.oc.api.detail;

import li.cil.oc.api.fs.Label;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;

public interface FileSystemAPI {
    li.cil.oc.api.fs.FileSystem fromClass(Class<?> clazz, String domain, String root);

    li.cil.oc.api.fs.FileSystem fromSaveDirectory(String root, long capacity, boolean buffered);

    li.cil.oc.api.fs.FileSystem fromMemory(long capacity);

    li.cil.oc.api.fs.FileSystem asReadOnly(li.cil.oc.api.fs.FileSystem fileSystem);

    ManagedEnvironment asManagedEnvironment(li.cil.oc.api.fs.FileSystem fileSystem, Label label, EnvironmentHost host, String accessSound, int speed);

    ManagedEnvironment asManagedEnvironment(li.cil.oc.api.fs.FileSystem fileSystem, String label, EnvironmentHost host, String accessSound, int speed);

    /**
     * @deprecated Use {@link li.cil.oc.api.FileSystem} convenience overloads instead.
     */
    @Deprecated
    default ManagedEnvironment asManagedEnvironment(final li.cil.oc.api.fs.FileSystem fileSystem, final Label label, final EnvironmentHost host, final String accessSound) {
        return asManagedEnvironment(fileSystem, label, host, accessSound, 1);
    }

    /**
     * @deprecated Use {@link li.cil.oc.api.FileSystem} convenience overloads instead.
     */
    @Deprecated
    default ManagedEnvironment asManagedEnvironment(final li.cil.oc.api.fs.FileSystem fileSystem, final String label, final EnvironmentHost host, final String accessSound) {
        return asManagedEnvironment(fileSystem, label, host, accessSound, 1);
    }

    /**
     * @deprecated Use {@link li.cil.oc.api.FileSystem} convenience overloads instead.
     */
    @Deprecated
    default ManagedEnvironment asManagedEnvironment(final li.cil.oc.api.fs.FileSystem fileSystem, final Label label) {
        return asManagedEnvironment(fileSystem, label, null, null, 1);
    }

    /**
     * @deprecated Use {@link li.cil.oc.api.FileSystem} convenience overloads instead.
     */
    @Deprecated
    default ManagedEnvironment asManagedEnvironment(final li.cil.oc.api.fs.FileSystem fileSystem, final String label) {
        return asManagedEnvironment(fileSystem, label, null, null, 1);
    }

    /**
     * @deprecated Use {@link li.cil.oc.api.FileSystem} convenience overloads instead.
     */
    @Deprecated
    default ManagedEnvironment asManagedEnvironment(final li.cil.oc.api.fs.FileSystem fileSystem) {
        return asManagedEnvironment(fileSystem, (Label) null, null, null, 1);
    }
}
