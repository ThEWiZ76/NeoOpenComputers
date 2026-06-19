package li.cil.oc.common;

import li.cil.oc.api.Network;
import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.nbt.CompoundTag;

import java.util.Optional;

final class FileSystemEnvironment extends AbstractManagedEnvironment {
    private static final String FILE_SYSTEM_TAG = "fs";

    private final FileSystem fileSystem;
    private final Label label;
    private final Optional<EnvironmentHost> host;
    private final Optional<String> accessSound;
    private final int speed;

    FileSystemEnvironment(final FileSystem fileSystem, final Label label, final EnvironmentHost host, final String accessSound, final int speed) {
        this.fileSystem = fileSystem;
        this.label = label;
        this.host = Optional.ofNullable(host);
        this.accessSound = Optional.ofNullable(accessSound);
        this.speed = Math.max(1, Math.min(speed, 6));
        setNode(Network.newNode(this, Visibility.Network)
            .withComponent("filesystem", Visibility.Neighbors)
            .withConnector()
            .create());
    }

    FileSystem fileSystem() {
        return fileSystem;
    }

    Label label() {
        return label;
    }

    Optional<EnvironmentHost> host() {
        return host;
    }

    Optional<String> accessSound() {
        return accessSound;
    }

    int speed() {
        return speed;
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether the file system is read-only.")
    public Object[] isReadOnly(final Context context, final Arguments arguments) {
        return new Object[]{fileSystem.isReadOnly()};
    }

    @Callback(direct = true, doc = "function():number -- The overall capacity of the file system, in bytes.")
    public Object[] spaceTotal(final Context context, final Arguments arguments) {
        return new Object[]{fileSystem.spaceTotal()};
    }

    @Callback(direct = true, doc = "function():number -- The currently used capacity of the file system, in bytes.")
    public Object[] spaceUsed(final Context context, final Arguments arguments) {
        return new Object[]{fileSystem.spaceUsed()};
    }

    @Callback(direct = true, doc = "function(path:string):boolean -- Returns whether an object exists at the specified absolute path.")
    public Object[] exists(final Context context, final Arguments arguments) {
        return new Object[]{fileSystem.exists(clean(arguments.checkString(0)))};
    }

    @Override
    public void onDisconnect(final Node node) {
        if (node == node()) {
            fileSystem.close();
        }
    }

    @Override
    public void onMessage(final Message message) {
        if ("computer.stopped".equals(message.name()) || "computer.started".equals(message.name())) {
            fileSystem.close();
        }
    }

    @Override
    public void load(final CompoundTag nbt) {
        super.load(nbt);
        if (label != null) {
            label.load(nbt);
        }
        if (nbt.contains(FILE_SYSTEM_TAG)) {
            fileSystem.load(nbt.getCompound(FILE_SYSTEM_TAG));
        }
    }

    @Override
    public void save(final CompoundTag nbt) {
        super.save(nbt);
        if (label != null) {
            label.save(nbt);
        }
        CompoundTag fileSystemTag = new CompoundTag();
        fileSystem.save(fileSystemTag);
        nbt.put(FILE_SYSTEM_TAG, fileSystemTag);
    }

    private String clean(final String path) {
        if (path == null || path.isEmpty() || "/".equals(path) || ".".equals(path)) {
            return "";
        }
        final String[] rawSegments = path.replace('\\', '/').split("/");
        final java.util.ArrayDeque<String> segments = new java.util.ArrayDeque<>();
        for (String segment : rawSegments) {
            if (segment.isEmpty() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                if (segments.isEmpty()) {
                    throw new IllegalArgumentException("path escapes file system root");
                }
                segments.removeLast();
            } else {
                segments.addLast(segment);
            }
        }
        return String.join("/", segments);
    }
}
