package li.cil.oc.common.item;

import li.cil.oc.api.FileSystem;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.DriveEnvironment;
import net.minecraft.nbt.CompoundTag;
import java.util.Map;
import java.util.function.Consumer;

record DiskItemEnvironment(ManagedEnvironment delegate, Consumer<CompoundTag> saveData, String lockInfo) implements ManagedEnvironment, DeviceInfo {

    static ManagedEnvironment create(long capacity, int platters, int speed, String sound,
                                     CompoundTag data, Consumer<CompoundTag> saveData, EnvironmentHost host, String fallbackLabel) {
        final String lockInfo = HardDiskDriveItem.lockInfo(data);
        final ManagedEnvironment environment;
        if (HardDiskDriveItem.isUnmanaged(data)) {
            environment = new DriveEnvironment((int) capacity, platters, new ItemDiskLabel(fallbackLabel), host, sound, speed, lockInfo);
        } else {
            final var fileSystem = FileSystem.fromMemory(capacity);
            if (fileSystem == null) return null;
            final String label = data != null && data.contains(ItemDiskLabel.TAG) ? data.getString(ItemDiskLabel.TAG) : fallbackLabel;
            environment = lockInfo.isEmpty()
                ? FileSystem.asManagedEnvironment(fileSystem, new ItemDiskLabel(fallbackLabel), host, sound, speed)
                : FileSystem.asManagedEnvironment(FileSystem.asReadOnly(fileSystem), label, host, sound, speed);
        }
        if (environment == null) return null;
        if (data != null && !data.isEmpty()) environment.load(data);
        return new DiskItemEnvironment(environment, saveData, lockInfo);
    }

    @Override
    public Node node() {
        return delegate.node();
    }

    @Override
    public void onConnect(final Node node) {
        delegate.onConnect(node);
    }

    @Override
    public void onDisconnect(final Node node) {
        delegate.onDisconnect(node);
    }

    @Override
    public void onMessage(final Message message) {
        delegate.onMessage(message);
    }

    @Override
    public boolean canUpdate() {
        return delegate.canUpdate();
    }

    @Override
    public void update() {
        delegate.update();
    }

    @Override
    public void load(final CompoundTag nbt) {
        delegate.load(nbt);
    }

    @Override
    public void save(final CompoundTag nbt) {
        delegate.save(nbt);
        if (!lockInfo.isEmpty()) nbt.putString("oc:lock", lockInfo);
        if (saveData != null) {
            saveData.accept(nbt.copy());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return delegate instanceof DeviceInfo info ? info.getDeviceInfo() : Map.of();
    }
}
