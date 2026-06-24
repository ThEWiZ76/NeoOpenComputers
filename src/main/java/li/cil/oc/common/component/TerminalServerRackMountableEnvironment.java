package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.component.RackBusConnectable;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.api.util.StateAware;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.blockentity.ScreenItemEnvironment;
import li.cil.oc.common.item.TerminalItem;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TerminalServerRackMountableEnvironment extends AbstractManagedEnvironment implements RackMountable, DeviceInfo, Analyzable {
    private static final String TAG_KIND = "kind";
    private static final String TAG_SCREEN = "screen";
    private static final String TAG_KEYBOARD = "keyboard";
    private static final String TAG_KEYS = "oc:keys";
    private static final int MAX_TERMINALS = 4;

    private final ScreenItemEnvironment screen;
    private final KeyboardItemEnvironment keyboard;
    private final List<String> keys = new ArrayList<>();

    public TerminalServerRackMountableEnvironment() {
        OpenComputersApi.initialize();
        screen = new ScreenItemEnvironment(null, 1);
        screen.setMaximumResolution(ModSettings.screenWidthByTier(2), ModSettings.screenHeightByTier(2));
        screen.setMaximumColorDepth(ModSettings.screenDepthByTier(2));
        keyboard = new KeyboardItemEnvironment();
        final var builder = Network.newNode(this, Visibility.None);
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
        keys.clear();
        final ListTag keyTags = nbt.getList(TAG_KEYS, StringTag.TAG_STRING);
        for (int index = 0; index < keyTags.size(); index++) {
            keys.add(keyTags.getString(index));
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
        final ListTag keyTags = new ListTag();
        for (final String key : keys) {
            keyTags.add(StringTag.valueOf(key));
        }
        nbt.put(TAG_KEYS, keyTags);
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

    public String bindTerminal(final ItemStack terminal) {
        if (terminal == null || terminal.isEmpty() || node() == null || node().address() == null) {
            return null;
        }
        final String oldKey = terminalKey(terminal);
        if (oldKey != null) {
            keys.remove(oldKey);
        }
        while (keys.size() >= MAX_TERMINALS) {
            keys.removeFirst();
        }
        final String key = UUID.randomUUID().toString();
        keys.add(key);
        return key;
    }

    public boolean allowsTerminal(final ItemStack terminal) {
        final String key = terminalKey(terminal);
        return key != null && keys.contains(key);
    }

    public TextBuffer screen() {
        return screen;
    }

    public TerminalScreenSnapshot screenSnapshot() {
        final String[] lines = new String[screen.renderHeight()];
        final int[][] foreground = new int[screen.renderHeight()][screen.renderWidth()];
        final int[][] background = new int[screen.renderHeight()][screen.renderWidth()];
        for (int row = 0; row < lines.length; row++) {
            lines[row] = line(row);
            for (int column = 0; column < screen.renderWidth(); column++) {
                foreground[row][column] = screen.getForegroundColor(column, row);
                background[row][column] = screen.getBackgroundColor(column, row);
            }
        }
        return new TerminalScreenSnapshot(screen.renderWidth(), screen.renderHeight(), lines, foreground, background);
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
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
            DeviceInfo.DeviceAttribute.Description, "Terminal server",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
            DeviceInfo.DeviceAttribute.Product, "RemoteViewing EX"
        );
    }

    @Override
    public Node[] onAnalyze(final Player player, final Direction side, final float hitX, final float hitY, final float hitZ) {
        return new Node[]{screen.node(), keyboard.node()};
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
        if (screen.node() != null && keyboard.node() != null && !screen.node().isNeighborOf(keyboard.node())) {
            screen.node().connect(keyboard.node());
        }
        TerminalServerRegistry.add(this);
    }

    private String line(final int row) {
        final StringBuilder builder = new StringBuilder(screen.renderWidth());
        for (int column = 0; column < screen.renderWidth(); column++) {
            builder.appendCodePoint(screen.getCodePoint(column, row));
        }
        return builder.toString();
    }

    private static String terminalKey(final ItemStack terminal) {
        if (terminal == null || terminal.isEmpty()) {
            return null;
        }
        final CustomData customData = terminal.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        final String key = customData.copyTag().getCompound(TerminalItem.DATA_TAG).getString(TerminalItem.KEY_TAG);
        return key.isBlank() ? null : key;
    }
}
