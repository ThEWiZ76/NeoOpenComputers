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
    public static final int MIN_COMPUTER_SLOT_COUNT = ComputerCaseBlockEntity.CONTAINER_SIZE;
    public static final int MAX_COMPUTER_SLOT_COUNT = 10;
    public static final int COMPUTER_SLOT_COUNT = MAX_COMPUTER_SLOT_COUNT;
    public static final int PLAYER_SLOT_COUNT = 36;
    public static final int TOTAL_SLOT_COUNT = COMPUTER_SLOT_COUNT + PLAYER_SLOT_COUNT;
    public static final int MAX_TOTAL_SLOT_COUNT = MAX_COMPUTER_SLOT_COUNT + PLAYER_SLOT_COUNT;

    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 84;
    private static final int PLAYER_HOTBAR_Y = 142;
    private static final int[][] COMPUTER_SLOT_POSITIONS = {
        {35, 17},
        {53, 17},
        {80, 17},
        {107, 17},
        {62, 44},
        {89, 44},
        {116, 44},
        {35, 44},
        {143, 17},
        {143, 44}
    };

    private final Container computerInventory;
    private final int computerSlotCount;

    public ComputerCaseMenu(final int containerId, final Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(MAX_COMPUTER_SLOT_COUNT));
    }

    public ComputerCaseMenu(final int containerId, final Inventory playerInventory, final Container computerInventory) {
        super(ModMenus.COMPUTER_CASE.get(), containerId);
        checkContainerSize(computerInventory, MIN_COMPUTER_SLOT_COUNT);
        this.computerInventory = computerInventory;
        computerSlotCount = COMPUTER_SLOT_COUNT;
        computerInventory.startOpen(playerInventory.player);

        for (int slot = 0; slot < computerSlotCount; slot++) {
            final int computerSlot = slot;
            final int[] position = COMPUTER_SLOT_POSITIONS[slot];
            addSlot(new Slot(computerInventory, computerSlot, position[0], position[1]) {
                @Override
                public boolean mayPlace(final ItemStack stack) {
                    return computerInventory.canPlaceItem(computerSlot, stack);
                }
            });
        }
        addPlayerInventory(playerInventory);
    }

    public static int computerSlotX(final int slot) {
        return COMPUTER_SLOT_POSITIONS[slot][0];
    }

    public static int computerSlotY(final int slot) {
        return COMPUTER_SLOT_POSITIONS[slot][1];
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        ItemStack moved = ItemStack.EMPTY;
        final Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            final ItemStack stack = slot.getItem();
            moved = stack.copy();
            if (index < computerSlotCount) {
                if (!moveItemStackTo(stack, computerSlotCount, computerSlotCount + PLAYER_SLOT_COUNT, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, computerSlotCount, false)) {
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
