package li.cil.oc.common.menu;

import li.cil.oc.common.ModMenus;
import li.cil.oc.common.component.TerminalScreenSnapshot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class TerminalMenu extends AbstractContainerMenu {
    public static final int TERMINAL_SLOT_COUNT = 0;
    public static final int TOTAL_SLOT_COUNT = TERMINAL_SLOT_COUNT;

    private final TerminalScreenSnapshot snapshot;

    public TerminalMenu(final int containerId, final Inventory playerInventory) {
        this(containerId, playerInventory, new TerminalScreenSnapshot(0, 0, new String[0]));
    }

    public TerminalMenu(final int containerId, final Inventory playerInventory, final TerminalScreenSnapshot snapshot) {
        super(ModMenus.TERMINAL.get(), containerId);
        this.snapshot = snapshot == null ? new TerminalScreenSnapshot(0, 0, new String[0]) : snapshot;
    }

    public TerminalScreenSnapshot snapshot() {
        return snapshot;
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
