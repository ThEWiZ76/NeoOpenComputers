package li.cil.oc.common.item;

import li.cil.oc.api.FileSystem;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.DriveEnvironment;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.NativeVolumeStore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.function.Consumer;

record DiskItemEnvironment(ManagedEnvironment delegate, Consumer<CompoundTag> saveData, String lockInfo, String storageRoot) implements ManagedEnvironment, DeviceInfo {

    DiskItemEnvironment(ManagedEnvironment delegate, Consumer<CompoundTag> saveData, String lockInfo) {
        this(delegate, saveData, lockInfo, null);
    }

    static ManagedEnvironment create(long capacity, int platters, int speed, String sound,
                                     CompoundTag data, Consumer<CompoundTag> saveData, EnvironmentHost host, String fallbackLabel) {
        final var server = ServerLifecycleHooks.getCurrentServer();
        final var storage = server == null ? null : new NativeVolumeStore(server.getWorldPath(LevelResource.ROOT));
        try { return create(capacity, platters, speed, sound, data, saveData, host, fallbackLabel, storage); }
        catch (UncheckedIOException | IllegalArgumentException failure) {
            li.cil.oc.NeoOpenComputers.LOGGER.warn("Cannot mount native disk for {}; item data retained",
                host == null ? "detached host" : host.getClass().getSimpleName(), failure);
            return null;
        }
    }

    static ManagedEnvironment create(long capacity, int platters, int speed, String sound,
                                     CompoundTag data, Consumer<CompoundTag> saveData, EnvironmentHost host, String fallbackLabel,
                                     NativeVolumeStore storage) {
        if (storage == null) return null;
        final String lockInfo = HardDiskDriveItem.lockInfo(data);
        final ManagedEnvironment environment;
        final var source = data == null ? new CompoundTag() : data.copy();
        String storageRoot = null;
        if (HardDiskDriveItem.isUnmanaged(data)) {
            if (capacity > Integer.MAX_VALUE) throw new IllegalArgumentException("Raw drive exceeds supported byte-array capacity");
            final NativeVolumeStore.RawMount mount;
            try { mount = storage.openRaw(source, (int) capacity); }
            catch (IOException failure) { throw new UncheckedIOException("Cannot mount native raw drive", failure); }
            final var node = source.getCompound("node"); node.putString("address", mount.nodeAddress()); source.put("node", node);
            source.putString(NativeVolumeStore.RAW_STORAGE_TAG, mount.storageRoot());
            environment = new DriveEnvironment((int) capacity, platters, new ItemDiskLabel(fallbackLabel), host, sound, speed, lockInfo, mount);
        } else {
            final NativeVolumeStore.Mount mount;
            try { mount = storage.openManaged(source, capacity, ModSettings.bufferChanges()); }
            catch (IOException failure) { throw new UncheckedIOException("Cannot mount native managed disk", failure); }
            final var fileSystem = mount.fileSystem();
            storageRoot = mount.storageRoot();
            final var node = source.getCompound("node");
            node.putString("address", mount.nodeAddress()); source.put("node", node);
            final var metadata = source.getCompound("fs");
            metadata.putString(NativeVolumeStore.STORAGE_TAG, storageRoot); source.put("fs", metadata);
            final String label = data != null && data.contains(ItemDiskLabel.TAG) ? data.getString(ItemDiskLabel.TAG) : fallbackLabel;
            environment = lockInfo.isEmpty()
                ? FileSystem.asManagedEnvironment(fileSystem, new ItemDiskLabel(fallbackLabel), host, sound, speed)
                : FileSystem.asManagedEnvironment(FileSystem.asReadOnly(fileSystem), label, host, sound, speed);
            if (environment == null) { fileSystem.close(); return null; }
            try { environment.load(source); }
            catch (RuntimeException failure) { fileSystem.close(); throw failure; }
        }
        if (environment == null) return null;
        if (storageRoot == null && !source.isEmpty()) environment.load(source);
        return new DiskItemEnvironment(environment, saveData, lockInfo, storageRoot);
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
        final String binding = nbt.getCompound("fs").getString(NativeVolumeStore.STORAGE_TAG);
        if (storageRoot != null && !binding.isEmpty() && !storageRoot.equals(binding)) {
            throw new IllegalArgumentException("Cannot replace an open native disk's storage binding");
        }
        delegate.load(nbt);
    }

    @Override
    public void save(final CompoundTag nbt) {
        delegate.save(nbt);
        if (storageRoot != null) nbt.getCompound("fs").putString(NativeVolumeStore.STORAGE_TAG, storageRoot);
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
