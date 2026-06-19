package li.cil.oc.common.menu;

import li.cil.oc.common.ModMenus;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ComputerCaseMenu extends AbstractContainerMenu {
    public static final int COMPUTER_SLOT_COUNT = ComputerCaseBlockEntity.CONTAINER_SIZE;
    public static final int PLAYER_SLOT_COUNT = 36;
    public static final int TOTAL_SLOT_COUNT = COMPUTER_SLOT_COUNT + PLAYER_SLOT_COUNT;

    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 84;
    private static final int PLAYER_HOTBAR_Y = 142;

    private final Container computerInventory;

    public ComputerCaseMenu(final int containerId, final Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(COMPUTER_SLOT_COUNT));
    }

    public ComputerCaseMenu(final int containerId, final Inventory playerInventory, final Container computerInventory) {
        super(ModMenus.COMPUTER_CASE.get(), containerId);
        checkContainerSize(computerInventory, COMPUTER_SLOT_COUNT);
        this.computerInventory = computerInventory;
        computerInventory.startOpen(playerInventory.player);

        addSlot(new Slot(computerInventory, ComputerCaseBlockEntity.SLOT_CPU, 80, 17));
        addSlot(new Slot(computerInventory, ComputerCaseBlockEntity.SLOT_MEMORY_0, 62, 44));
        addSlot(new Slot(computerInventory, ComputerCaseBlockEntity.SLOT_MEMORY_1, 98, 44));
        addPlayerInventory(playerInventory);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        ItemStack moved = ItemStack.EMPTY;
        final Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            final ItemStack stack = slot.getItem();
            moved = stack.copy();
            if (index < COMPUTER_SLOT_COUNT) {
                if (!moveItemStackTo(stack, COMPUTER_SLOT_COUNT, TOTAL_SLOT_COUNT, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, COMPUTER_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return moved;
    }

    @Override
    public boolean stillValid(final Player player) {
        return computerInventory.stillValid(player);
    }

    @Override
    public void removed(final Player player) {
        super.removed(player);
        computerInventory.stopOpen(player);
    }

    private void addPlayerInventory(final Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, PLAYER_INVENTORY_X + column * 18, PLAYER_INVENTORY_Y + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, PLAYER_INVENTORY_X + column * 18, PLAYER_HOTBAR_Y));
        }
    }
}
