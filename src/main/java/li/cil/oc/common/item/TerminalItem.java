package li.cil.oc.common.item;

import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.blockentity.RackBlockEntity;
import li.cil.oc.common.component.TerminalServerRackMountableEnvironment;
import li.cil.oc.common.component.TerminalServerRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

public class TerminalItem extends Item {
    public static final String DATA_TAG = "oc:terminal";
    public static final String TERMINAL_SERVER_TAG = "terminalServer";
    public static final String SCREEN_TAG = "screen";
    public static final String KEYBOARD_TAG = "keyboard";
    public static final String KEY_TAG = "key";

    public TerminalItem(final Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final BlockEntity blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
        if (!(blockEntity instanceof RackBlockEntity rack)) {
            return InteractionResult.PASS;
        }
        return bindToFirstTerminalServer(context.getItemInHand(), rack) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    public static boolean bindToTerminalServer(final ItemStack terminal, final RackBlockEntity rack, final int slot) {
        if (terminal == null || terminal.isEmpty() || rack == null) {
            return false;
        }
        final RackMountable mountable = rack.getMountable(slot);
        if (mountable == null || mountable.node() == null) {
            return false;
        }
        String screenAddress = null;
        String keyboardAddress = null;
        for (final Node node : mountable.node().neighbors()) {
            if (node instanceof Component component && "screen".equals(component.name())) {
                screenAddress = node.address();
            } else if (node instanceof Component component && "keyboard".equals(component.name())) {
                keyboardAddress = node.address();
            }
        }
        final String terminalServerAddress = mountable.node().address();
        if (terminalServerAddress == null || screenAddress == null || keyboardAddress == null) {
            return false;
        }
        final String key = mountable instanceof TerminalServerRackMountableEnvironment terminalServer
            ? terminalServer.bindTerminal(terminal)
            : null;
        if (key == null || key.isBlank()) {
            return false;
        }
        final CustomData customData = terminal.get(DataComponents.CUSTOM_DATA);
        final CompoundTag root = customData == null ? new CompoundTag() : customData.copyTag();
        final CompoundTag data = new CompoundTag();
        data.putString(TERMINAL_SERVER_TAG, terminalServerAddress);
        data.putString(SCREEN_TAG, screenAddress);
        data.putString(KEYBOARD_TAG, keyboardAddress);
        data.putString(KEY_TAG, key);
        root.put(DATA_TAG, data);
        terminal.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return true;
    }

    public static TerminalServerRackMountableEnvironment findBoundTerminalServer(final ItemStack terminal) {
        final CompoundTag data = terminalData(terminal);
        if (data == null) {
            return null;
        }
        final String address = data.getString(TERMINAL_SERVER_TAG);
        if (address.isBlank()) {
            return null;
        }
        final TerminalServerRackMountableEnvironment terminalServer = TerminalServerRegistry.find(address);
        return terminalServer != null && terminalServer.allowsTerminal(terminal) ? terminalServer : null;
    }

    private static boolean bindToFirstTerminalServer(final ItemStack terminal, final RackBlockEntity rack) {
        for (int slot = 0; slot < rack.getContainerSize(); slot++) {
            if (bindToTerminalServer(terminal, rack, slot)) {
                return true;
            }
        }
        return false;
    }

    private static CompoundTag terminalData(final ItemStack terminal) {
        if (terminal == null || terminal.isEmpty()) {
            return null;
        }
        final CustomData customData = terminal.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        return customData.copyTag().getCompound(DATA_TAG);
    }
}
