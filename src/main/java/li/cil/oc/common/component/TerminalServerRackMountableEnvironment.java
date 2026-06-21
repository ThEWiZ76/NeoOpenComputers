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

public final class TerminalServerRackMountableEnvironment extends AbstractManagedEnvironment implements RackMountable, DeviceInfo {
    private static final String TAG_KIND = "kind";

    public TerminalServerRackMountableEnvironment() {
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.create());
        }
    }

    @Override
    public CompoundTag getData() {
        final CompoundTag data = new CompoundTag();
        data.putString(TAG_KIND, "terminal_server");
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
            DeviceInfo.DeviceAttribute.Description, "Terminal server",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
            DeviceInfo.DeviceAttribute.Product, "Terminal Server"
        );
    }
}
