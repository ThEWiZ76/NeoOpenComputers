package li.cil.oc.common.menu;

import li.cil.oc.common.ModMenus;
import li.cil.oc.common.blockentity.DiskDriveBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DiskDriveMenu extends AbstractContainerMenu {
    public static final int DRIVE_SLOT_COUNT = DiskDriveBlockEntity.CONTAINER_SIZE;
    public static final int PLAYER_SLOT_COUNT = 36;
    public static final int TOTAL_SLOT_COUNT = DRIVE_SLOT_COUNT + PLAYER_SLOT_COUNT;

    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 84;
    private static final int PLAYER_HOTBAR_Y = 142;

    private final Container diskInventory;

    public DiskDriveMenu(final int containerId, final Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(DRIVE_SLOT_COUNT));
    }

    public DiskDriveMenu(final int containerId, final Inventory playerInventory, final Container diskInventory) {
        super(ModMenus.DISK_DRIVE.get(), containerId);
        checkContainerSize(diskInventory, DRIVE_SLOT_COUNT);
        this.diskInventory = diskInventory;
        diskInventory.startOpen(playerInventory.player);

        addSlot(new Slot(diskInventory, DiskDriveBlockEntity.SLOT_FLOPPY, 80, 35) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return diskInventory.canPlaceItem(DiskDriveBlockEntity.SLOT_FLOPPY, stack);
            }

            @Override
            public int getMaxStackSize() {
                return diskInventory.getMaxStackSize();
            }
        });
        addPlayerInventory(playerInventory);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        ItemStack moved = ItemStack.EMPTY;
        final Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            final ItemStack stack = slot.getItem();
            moved = stack.copy();
            if (index < DRIVE_SLOT_COUNT) {
                if (!moveItemStackTo(stack, DRIVE_SLOT_COUNT, TOTAL_SLOT_COUNT, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, DRIVE_SLOT_COUNT, false)) {
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
        return diskInventory.stillValid(player);
    }

    @Override
    public void removed(final Player player) {
        super.removed(player);
        diskInventory.stopOpen(player);
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
