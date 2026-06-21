package li.cil.oc.common.menu;

import li.cil.oc.common.ModMenus;
import li.cil.oc.common.component.TerminalScreenSnapshot;
import li.cil.oc.common.component.TerminalServerRackMountableEnvironment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class TerminalMenu extends AbstractContainerMenu {
    public static final int TERMINAL_SLOT_COUNT = 0;
    public static final int TOTAL_SLOT_COUNT = TERMINAL_SLOT_COUNT;

    private TerminalScreenSnapshot snapshot;
    private final TerminalServerRackMountableEnvironment terminalServer;

    public TerminalMenu(final int containerId, final Inventory playerInventory) {
        this(containerId, playerInventory, new TerminalScreenSnapshot(0, 0, new String[0]));
    }

    public TerminalMenu(final int containerId, final Inventory playerInventory, final TerminalScreenSnapshot snapshot) {
        this(containerId, playerInventory, snapshot, null);
    }

    public TerminalMenu(final int containerId, final Inventory playerInventory, final TerminalScreenSnapshot snapshot, final TerminalServerRackMountableEnvironment terminalServer) {
        super(ModMenus.TERMINAL.get(), containerId);
        this.snapshot = snapshot == null ? new TerminalScreenSnapshot(0, 0, new String[0]) : snapshot;
        this.terminalServer = terminalServer;
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
    public ItemStack quickMoveStack(final Player player, final int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(final Player player) {
        return true;
    }
}
