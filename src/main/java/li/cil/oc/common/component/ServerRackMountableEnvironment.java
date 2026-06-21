package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.component.RackBusConnectable;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.internal.Server;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.api.util.StateAware;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public final class ServerRackMountableEnvironment extends AbstractManagedEnvironment implements Server, DeviceInfo {
    private static final String TAG_KIND = "kind";
    private static final String TAG_MACHINE = "machine";
    private static final String TAG_TIER = "tier";

    private final Rack rack;
    private final int slot;
    private final int tier;
    private final Machine machine;

    public ServerRackMountableEnvironment(final Rack rack, final int slot, final int tier) {
        OpenComputersApi.initialize();
        this.rack = rack;
        this.slot = slot;
        this.tier = Math.max(0, Math.min(2, tier));
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.create());
        }
        machine = li.cil.oc.api.Machine.create(this);
    }

    @Override
    public CompoundTag getData() {
        final CompoundTag data = new CompoundTag();
        data.putString(TAG_KIND, "server");
        data.putInt(TAG_TIER, tier);
        return data;
    }

    @Override
    public Rack rack() {
        return rack;
    }

    @Override
    public int slot() {
        return slot;
    }

    @Override
    public int tier() {
        return tier;
    }

    @Override
    public Machine machine() {
        return machine;
    }

    @Override
    public Iterable<ItemStack> internalComponents() {
        return List.of();
    }

    @Override
    public int componentSlot(final String address) {
        return -1;
    }

    @Override
    public void onMachineConnect(final Node node) {
    }

    @Override
    public void onMachineDisconnect(final Node node) {
    }

    @Override
    public Level world() {
        return rack == null ? null : rack.world();
    }

    @Override
    public double xPosition() {
        return rack == null ? 0D : rack.xPosition();
    }

    @Override
    public double yPosition() {
        return rack == null ? 0D : rack.yPosition();
    }

    @Override
    public double zPosition() {
        return rack == null ? 0D : rack.zPosition();
    }

    @Override
    public void markChanged() {
        if (rack != null && slot >= 0) {
            rack.markChanged(slot);
        }
    }

    @Override
    public int getConnectableCount() {
        return 0;
    }

    @Override
    public RackBusConnectable getConnectableAt(final int index) {
        return null;
    }

    @Override
    public boolean onActivate(final Player player, final InteractionHand hand, final ItemStack heldItem, final float hitX, final float hitY) {
        return false;
    }

    @Override
    public EnumSet<StateAware.State> getCurrentState() {
        return EnumSet.of(StateAware.State.CanWork);
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.System,
            DeviceInfo.DeviceAttribute.Description, "Server",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
            DeviceInfo.DeviceAttribute.Product, "Server Tier " + (tier + 1)
        );
    }

    @Override
    public void load(final CompoundTag nbt) {
        super.load(nbt);
        if (machine != null && nbt.contains(TAG_MACHINE)) {
            machine.load(nbt.getCompound(TAG_MACHINE));
        }
    }

    @Override
    public void save(final CompoundTag nbt) {
        super.save(nbt);
        if (machine != null) {
            final CompoundTag machineTag = new CompoundTag();
            machine.save(machineTag);
            nbt.put(TAG_MACHINE, machineTag);
        }
    }
}
