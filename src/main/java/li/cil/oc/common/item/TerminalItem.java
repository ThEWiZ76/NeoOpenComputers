package li.cil.oc.common.item;

import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.blockentity.RackBlockEntity;
import li.cil.oc.common.component.TerminalScreenSnapshot;
import li.cil.oc.common.component.TerminalServerRackMountableEnvironment;
import li.cil.oc.common.component.TerminalServerRegistry;
import li.cil.oc.common.menu.TerminalMenu;
import li.cil.oc.common.network.TerminalScreenSnapshotPayload;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.OptionalInt;

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
        final Vec3 clickLocation = context.getClickLocation();
        final float hitX = (float) (clickLocation.x - context.getClickedPos().getX());
        final float hitY = (float) (clickLocation.y - context.getClickedPos().getY());
        final float hitZ = (float) (clickLocation.z - context.getClickedPos().getZ());
        final Integer slot = rack.slotAt(context.getClickedFace(), hitX, hitY, hitZ);
        return slot != null && bindToTerminalServer(context.getItemInHand(), rack, slot) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack terminal = player.getItemInHand(hand);
        if (findBoundTerminalServer(terminal) == null) {
            return InteractionResultHolder.pass(terminal);
        }
        if (!level.isClientSide) {
            final OptionalInt openedContainerId = player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, menuPlayer) -> createMenuForBoundTerminal(containerId, playerInventory, terminal),
                net.minecraft.network.chat.Component.translatable("item.neoopencomputers.terminal")));
            if (openedContainerId.isPresent() && player instanceof ServerPlayer serverPlayer) {
                final TerminalScreenSnapshotPayload payload = createScreenSnapshotPayloadForBoundTerminal(openedContainerId.getAsInt(), terminal);
                if (payload != null) {
                    PacketDistributor.sendToPlayer(serverPlayer, payload);
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(terminal, level.isClientSide);
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

    public static TerminalMenu createMenuForBoundTerminal(final int containerId, final net.minecraft.world.entity.player.Inventory playerInventory, final ItemStack terminal) {
        final TerminalServerRackMountableEnvironment terminalServer = findBoundTerminalServer(terminal);
        if (terminalServer == null) {
            return null;
        }
        final TerminalScreenSnapshot snapshot = terminalServer.screenSnapshot();
        return new TerminalMenu(containerId, playerInventory, snapshot, terminalServer);
    }

    public static TerminalScreenSnapshotPayload createScreenSnapshotPayloadForBoundTerminal(final int containerId, final ItemStack terminal) {
        final TerminalServerRackMountableEnvironment terminalServer = findBoundTerminalServer(terminal);
        if (terminalServer == null) {
            return null;
        }
        return new TerminalScreenSnapshotPayload(containerId, terminalServer.screenSnapshot());
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
