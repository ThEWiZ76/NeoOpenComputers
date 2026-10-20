package li.cil.oc.common.integration.computercraft;

import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.media.MediaCapability;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.ModSounds;
import li.cil.oc.common.item.ItemDriverData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;

public final class MediaDriver implements DriverItem {
    private static IMedia media(ItemStack stack) { return stack == null || stack.isEmpty() ? null : stack.getCapability(MediaCapability.get()); }
    @Override public boolean worksWith(ItemStack stack) { return media(stack) != null; }
    @Override public String slot(ItemStack stack) { return Slot.Floppy; }
    @Override public int tier(ItemStack stack) { return 0; }
    @Override public CompoundTag dataTag(ItemStack stack) { return ItemDriverData.dataTag(stack); }

    @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host == null || !(host.world() instanceof ServerLevel world)) return null;
        final var media = media(stack);
        if (media == null) return null;
        final var before = stack.copy();
        final var mount = media.createDataMount(stack, world);
        if (!ItemStack.isSameItemSameComponents(before, stack)) host.markChanged();
        if (mount == null) return null;
        final var environment = li.cil.oc.api.FileSystem.asManagedEnvironment(new MountFileSystem(mount, mount instanceof WritableMount), new MediaLabel(media, stack, host, world), host, ModSounds.FLOPPY_ACCESS_ID);
        if (environment == null) return null;
        final var data = dataTag(stack).copy();
        final var node = data.getCompound("node");
        if (node.getString("address").isEmpty()) {
            node.putString("address", UUID.randomUUID().toString()); data.put("node", node);
            ItemDriverData.writeDataTag(stack, data); host.markChanged();
        }
        environment.load(data);
        return new MediaEnvironment(environment, stack);
    }

    private record MediaLabel(IMedia media, ItemStack stack, EnvironmentHost host, ServerLevel world) implements Label {
        @Override public String getLabel() { return media.getLabel(world.registryAccess(), stack); }
        @Override public void setLabel(String value) {
            if (!world.getServer().isSameThread()) throw new li.cil.oc.api.machine.LimitReachedException();
            if (!media.setLabel(stack, value)) throw new IllegalArgumentException("media label is read-only");
            host.markChanged();
        }
        @Override public void load(CompoundTag nbt) { }
        @Override public void save(CompoundTag nbt) { }
    }

    private record MediaEnvironment(ManagedEnvironment delegate, ItemStack stack) implements ManagedEnvironment, DeviceInfo {
        @Override public Node node() { return delegate.node(); }
        @Override public boolean canUpdate() { return delegate.canUpdate(); }
        @Override public void update() { delegate.update(); }
        @Override public void onConnect(Node node) { delegate.onConnect(node); }
        @Override public void onDisconnect(Node node) { delegate.onDisconnect(node); }
        @Override public void onMessage(Message message) { delegate.onMessage(message); }
        @Override public void load(CompoundTag nbt) { delegate.load(nbt); }
        @Override public void save(CompoundTag nbt) { delegate.save(nbt); ItemDriverData.writeDataTag(stack, nbt); }
        @Override public Map<String, String> getDeviceInfo() { return delegate instanceof DeviceInfo info ? info.getDeviceInfo() : Map.of(); }
    }
}
