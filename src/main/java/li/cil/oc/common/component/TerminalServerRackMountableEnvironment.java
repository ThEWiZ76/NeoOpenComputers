package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.component.RackBusConnectable;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.api.util.StateAware;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.blockentity.ScreenItemEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.Map;

public final class TerminalServerRackMountableEnvironment extends AbstractManagedEnvironment implements RackMountable, DeviceInfo {
    private static final String TAG_KIND = "kind";
    private static final String TAG_SCREEN = "screen";
    private static final String TAG_KEYBOARD = "keyboard";

    private final ScreenItemEnvironment screen;
    private final KeyboardItemEnvironment keyboard;

    public TerminalServerRackMountableEnvironment() {
        OpenComputersApi.initialize();
        screen = new ScreenItemEnvironment(null, 1);
        keyboard = new KeyboardItemEnvironment();
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.create());
        }
        connectVirtualTerminal();
    }

    @Override
    public CompoundTag getData() {
        final CompoundTag data = new CompoundTag();
        data.putString(TAG_KIND, "terminal_server");
        return data;
    }

    @Override
    public void load(final CompoundTag nbt) {
        super.load(nbt);
        TerminalServerRegistry.remove(this);
        if (nbt.contains(TAG_SCREEN)) {
            screen.load(nbt.getCompound(TAG_SCREEN));
        }
        if (nbt.contains(TAG_KEYBOARD)) {
            keyboard.load(nbt.getCompound(TAG_KEYBOARD));
        }
        connectVirtualTerminal();
    }

    @Override
    public void save(final CompoundTag nbt) {
        super.save(nbt);
        final CompoundTag screenTag = new CompoundTag();
        screen.save(screenTag);
        nbt.put(TAG_SCREEN, screenTag);
        final CompoundTag keyboardTag = new CompoundTag();
        keyboard.save(keyboardTag);
        nbt.put(TAG_KEYBOARD, keyboardTag);
    }

    public void removeVirtualNodes() {
        TerminalServerRegistry.remove(this);
        if (screen.node() != null) {
            screen.node().remove();
        }
        if (keyboard.node() != null) {
            keyboard.node().remove();
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
            DeviceInfo.DeviceAttribute.Description, "Terminal server",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
            DeviceInfo.DeviceAttribute.Product, "Terminal Server"
        );
    }

    private void connectVirtualTerminal() {
        if (node() == null) {
            return;
        }
        if (node().network() == null) {
            Network.joinNewNetwork(node());
        }
        if (screen.node() != null && !screen.node().isNeighborOf(node())) {
            node().connect(screen.node());
        }
        if (keyboard.node() != null && !keyboard.node().isNeighborOf(node())) {
            node().connect(keyboard.node());
        }
        TerminalServerRegistry.add(this);
    }
}
