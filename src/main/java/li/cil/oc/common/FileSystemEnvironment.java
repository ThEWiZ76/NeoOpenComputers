package li.cil.oc.common;

import li.cil.oc.api.Network;
import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.api.fs.Label;
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
}
