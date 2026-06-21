package li.cil.oc.common.menu;

import li.cil.oc.common.ModMenus;
import li.cil.oc.common.blockentity.RaidBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class RaidMenu extends AbstractContainerMenu {
    public static final int RAID_SLOT_COUNT = RaidBlockEntity.CONTAINER_SIZE;
    public static final int PLAYER_SLOT_COUNT = 36;
    public static final int TOTAL_SLOT_COUNT = RAID_SLOT_COUNT + PLAYER_SLOT_COUNT;
    public static final int RAID_STATUS_INDEX = 0;
    public static final int RAID_CAPACITY_INDEX = 1;
    public static final int RAID_DATA_COUNT = 2;

    public static final int STATE_EMPTY = 0;
    public static final int STATE_INCOMPLETE = 1;
    public static final int STATE_READY = 2;

    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 84;
    private static final int PLAYER_HOTBAR_Y = 142;

    private final Container raidInventory;
    private final ContainerData raidData;

    public RaidMenu(final int containerId, final Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(RAID_SLOT_COUNT), new SimpleContainerData(RAID_DATA_COUNT));
    }

    public RaidMenu(final int containerId, final Inventory playerInventory, final Container raidInventory) {
        this(containerId, playerInventory, raidInventory, raidData(raidInventory));
    }

    public RaidMenu(final int containerId, final Inventory playerInventory, final Container raidInventory, final ContainerData raidData) {
        super(ModMenus.RAID.get(), containerId);
        checkContainerSize(raidInventory, RAID_SLOT_COUNT);
        checkContainerDataCount(raidData, RAID_DATA_COUNT);
        this.raidInventory = raidInventory;
        this.raidData = raidData;
        raidInventory.startOpen(playerInventory.player);
        addDataSlots(raidData);

        addSlot(new RaidSlot(raidInventory, 0, 62, 35));
        addSlot(new RaidSlot(raidInventory, 1, 80, 35));
        addSlot(new RaidSlot(raidInventory, 2, 98, 35));
        addPlayerInventory(playerInventory);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        ItemStack moved = ItemStack.EMPTY;
        final Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            final ItemStack stack = slot.getItem();
            moved = stack.copy();
            if (index < RAID_SLOT_COUNT) {
                if (!moveItemStackTo(stack, RAID_SLOT_COUNT, TOTAL_SLOT_COUNT, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, RAID_SLOT_COUNT, false)) {
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
        return raidInventory.stillValid(player);
    }

    public int raidState() {
        return raidData.get(RAID_STATUS_INDEX);
    }

    public int raidCapacity() {
        return raidData.get(RAID_CAPACITY_INDEX);
    }

    public static int raidStateFor(final Container raidInventory) {
        if (!(raidInventory instanceof RaidBlockEntity raid) || raid.isEmpty()) {
            return STATE_EMPTY;
        }
        return raid.isComplete() ? STATE_READY : STATE_INCOMPLETE;
    }

    public static int raidCapacityFor(final Container raidInventory) {
        return raidInventory instanceof RaidBlockEntity raid ? raid.capacity() : 0;
    }

    @Override
    public void removed(final Player player) {
        super.removed(player);
        raidInventory.stopOpen(player);
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

    private static ContainerData raidData(final Container raidInventory) {
        return new ContainerData() {
            @Override
            public int get(final int index) {
                return switch (index) {
                    case RAID_STATUS_INDEX -> raidStateFor(raidInventory);
                    case RAID_CAPACITY_INDEX -> raidCapacityFor(raidInventory);
                    default -> 0;
                };
            }

            @Override
            public void set(final int index, final int value) {
            }

            @Override
            public int getCount() {
                return RAID_DATA_COUNT;
            }
        };
    }

    private static final class RaidSlot extends Slot {
        private RaidSlot(final Container container, final int slot, final int x, final int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(final ItemStack stack) {
            return container.canPlaceItem(getSlotIndex(), stack);
        }
    }
}
