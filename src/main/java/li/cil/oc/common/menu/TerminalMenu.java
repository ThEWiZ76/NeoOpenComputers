package li.cil.oc.common.menu;

import li.cil.oc.common.ModMenus;
import li.cil.oc.common.component.TerminalScreenDelta;
import li.cil.oc.common.component.TerminalScreenSnapshot;
import li.cil.oc.common.component.TerminalServerRegistry;
import li.cil.oc.common.component.TerminalServerRackMountableEnvironment;
import li.cil.oc.common.network.TerminalScreenDeltaPayload;
import li.cil.oc.common.network.TerminalScreenSnapshotPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class TerminalMenu extends AbstractContainerMenu {
    public static final int TERMINAL_SLOT_COUNT = 0;
    public static final int TOTAL_SLOT_COUNT = TERMINAL_SLOT_COUNT;

    private TerminalScreenSnapshot snapshot;
    private final TerminalServerRackMountableEnvironment terminalServer;
    private final String terminalKey;
    private final Player player;

    public TerminalMenu(final int containerId, final Inventory playerInventory) {
        this(containerId, playerInventory, new TerminalScreenSnapshot(0, 0, new String[0]));
    }

    public TerminalMenu(final int containerId, final Inventory playerInventory, final TerminalScreenSnapshot snapshot) {
        this(containerId, playerInventory, snapshot, null);
    }

    public TerminalMenu(final int containerId, final Inventory playerInventory, final TerminalScreenSnapshot snapshot, final TerminalServerRackMountableEnvironment terminalServer) {
        this(containerId, playerInventory, snapshot, terminalServer, null);
    }

    public TerminalMenu(final int containerId, final Inventory playerInventory, final TerminalScreenSnapshot snapshot, final TerminalServerRackMountableEnvironment terminalServer, final String terminalKey) {
        super(ModMenus.TERMINAL.get(), containerId);
        this.snapshot = snapshot == null ? new TerminalScreenSnapshot(0, 0, new String[0]) : snapshot;
        this.terminalServer = terminalServer;
        this.terminalKey = terminalKey == null || terminalKey.isBlank() ? null : terminalKey;
        this.player = playerInventory == null ? null : playerInventory.player;
    }

    public TerminalScreenSnapshot snapshot() {
        return snapshot;
    }

    public TerminalServerRackMountableEnvironment terminalServer() {
        return terminalServer;
    }

    public void updateSnapshot(final TerminalScreenSnapshot snapshot) {
        this.snapshot = snapshot == null ? new TerminalScreenSnapshot(0, 0, new String[0]) : snapshot;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        final CustomPacketPayload payload = changedScreenPayload();
        if (payload != null && player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, payload);
        }
    }

    TerminalScreenSnapshotPayload changedSnapshotPayload() {
        if (terminalServer == null) {
            return null;
        }
        final TerminalScreenSnapshot currentSnapshot = terminalServer.screenSnapshot();
        if (snapshot.contentEquals(currentSnapshot)) {
            return null;
        }
        updateSnapshot(currentSnapshot);
        return new TerminalScreenSnapshotPayload(containerId, currentSnapshot);
    }

    CustomPacketPayload changedScreenPayload() {
        if (terminalServer == null) {
            return null;
        }
        final TerminalScreenSnapshot currentSnapshot = terminalServer.screenSnapshot();
        if (snapshot.contentEquals(currentSnapshot)) {
            return null;
        }
        if (snapshot.width() == currentSnapshot.width() && snapshot.height() == currentSnapshot.height()) {
            final TerminalScreenDelta delta = TerminalScreenDelta.between(snapshot, currentSnapshot);
            updateSnapshot(currentSnapshot);
            return new TerminalScreenDeltaPayload(containerId, delta);
        }
        updateSnapshot(currentSnapshot);
        return new TerminalScreenSnapshotPayload(containerId, currentSnapshot);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(final Player player) {
        if (terminalServer == null) {
            return true;
        }
        return terminalServer.node() != null
            && TerminalServerRegistry.find(terminalServer.node().address()) == terminalServer
            && (terminalKey == null || terminalServer.allowsTerminalKey(terminalKey))
            && terminalServer.isUsableBy(player);
    }
}
