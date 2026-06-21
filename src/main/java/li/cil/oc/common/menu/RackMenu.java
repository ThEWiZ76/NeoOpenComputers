package li.cil.oc.common.menu;

import li.cil.oc.common.ModMenus;
import li.cil.oc.common.blockentity.RackBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class RackMenu extends AbstractContainerMenu {
    public static final int RACK_SLOT_COUNT = RackBlockEntity.CONTAINER_SIZE;
    public static final int PLAYER_SLOT_COUNT = 36;
    public static final int TOTAL_SLOT_COUNT = RACK_SLOT_COUNT + PLAYER_SLOT_COUNT;

    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 84;
    private static final int PLAYER_HOTBAR_Y = 142;

    private final Container rackInventory;

    public RackMenu(final int containerId, final Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(RACK_SLOT_COUNT));
    }

    public RackMenu(final int containerId, final Inventory playerInventory, final Container rackInventory) {
        super(ModMenus.RACK.get(), containerId);
        checkContainerSize(rackInventory, RACK_SLOT_COUNT);
        this.rackInventory = rackInventory;
        rackInventory.startOpen(playerInventory.player);

        addSlot(new RackSlot(rackInventory, 0, 53, 26));
        addSlot(new RackSlot(rackInventory, 1, 71, 26));
        addSlot(new RackSlot(rackInventory, 2, 89, 26));
        addSlot(new RackSlot(rackInventory, 3, 107, 26));
        addPlayerInventory(playerInventory);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        ItemStack moved = ItemStack.EMPTY;
        final Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            final ItemStack stack = slot.getItem();
            moved = stack.copy();
            if (index < RACK_SLOT_COUNT) {
                if (!moveItemStackTo(stack, RACK_SLOT_COUNT, TOTAL_SLOT_COUNT, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, RACK_SLOT_COUNT, false)) {
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
        return rackInventory.stillValid(player);
    }

    public Container rackInventory() {
        return rackInventory;
    }

    @Override
    public void removed(final Player player) {
        super.removed(player);
        rackInventory.stopOpen(player);
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

    private static final class RackSlot extends Slot {
        private RackSlot(final Container container, final int slot, final int x, final int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(final ItemStack stack) {
            return container.canPlaceItem(getSlotIndex(), stack);
        }
    }
}
