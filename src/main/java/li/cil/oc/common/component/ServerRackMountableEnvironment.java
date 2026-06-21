package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.component.RackBusConnectable;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.api.util.StateAware;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.Map;

public final class ServerRackMountableEnvironment extends AbstractManagedEnvironment implements RackMountable, DeviceInfo {
    private static final String TAG_KIND = "kind";
    private static final String TAG_TERMINAL = "terminal";
    private static final String TAG_TIER = "tier";

    private final int tier;
    private final boolean terminal;

    public ServerRackMountableEnvironment(final int tier, final boolean terminal) {
        this.tier = Math.max(0, Math.min(2, tier));
        this.terminal = terminal;
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.create());
        }
    }

    @Override
    public CompoundTag getData() {
        final CompoundTag data = new CompoundTag();
        data.putString(TAG_KIND, terminal ? "terminal_server" : "server");
        data.putBoolean(TAG_TERMINAL, terminal);
        data.putInt(TAG_TIER, tier);
        return data;
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
            DeviceInfo.DeviceAttribute.Description, terminal ? "Terminal server" : "Server",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
            DeviceInfo.DeviceAttribute.Product, terminal ? "Terminal Server" : "Server Tier " + (tier + 1)
        );
    }
}
