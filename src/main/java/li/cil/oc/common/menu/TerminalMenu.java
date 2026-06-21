package li.cil.oc.common.menu;

import li.cil.oc.common.ModMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class TerminalMenu extends AbstractContainerMenu {
    public static final int TERMINAL_SLOT_COUNT = 0;
    public static final int TOTAL_SLOT_COUNT = TERMINAL_SLOT_COUNT;

    public TerminalMenu(final int containerId, final Inventory playerInventory) {
        super(ModMenus.TERMINAL.get(), containerId);
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
