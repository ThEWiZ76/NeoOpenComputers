package li.cil.oc.common.menu;

import li.cil.oc.common.ModMenus;
import li.cil.oc.common.blockentity.RelayBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class RelayMenu extends AbstractContainerMenu {
    public static final int RELAY_SLOT_COUNT = RelayBlockEntity.CONTAINER_SIZE;
    public static final int PLAYER_SLOT_COUNT = 36;
    public static final int TOTAL_SLOT_COUNT = RELAY_SLOT_COUNT + PLAYER_SLOT_COUNT;
    public static final int RELAY_MODE_INDEX = 0;
    public static final int RELAY_DELAY_INDEX = 1;
    public static final int RELAY_QUEUE_SIZE_INDEX = 2;
    public static final int RELAY_MAX_QUEUE_SIZE_INDEX = 3;
    public static final int RELAY_STRENGTH_INDEX = 4;
    public static final int RELAY_REPEATER_INDEX = 5;
    public static final int RELAY_DATA_COUNT = 6;

    public static final int MODE_WIRED = 0;
    public static final int MODE_WIRELESS = 1;
    public static final int MODE_LINKED = 2;

    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 84;
    private static final int PLAYER_HOTBAR_Y = 142;

    private final Container relayInventory;
    private final ContainerData relayData;

    public RelayMenu(final int containerId, final Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(RELAY_SLOT_COUNT), new SimpleContainerData(RELAY_DATA_COUNT));
    }

    public RelayMenu(final int containerId, final Inventory playerInventory, final Container relayInventory) {
        this(containerId, playerInventory, relayInventory, relayData(relayInventory));
    }

    public RelayMenu(final int containerId, final Inventory playerInventory, final Container relayInventory, final ContainerData relayData) {
        super(ModMenus.RELAY.get(), containerId);
        checkContainerSize(relayInventory, RELAY_SLOT_COUNT);
        checkContainerDataCount(relayData, RELAY_DATA_COUNT);
        this.relayInventory = relayInventory;
        this.relayData = relayData;
        relayInventory.startOpen(playerInventory.player);
        addDataSlots(relayData);

        addSlot(new RelaySlot(relayInventory, RelayBlockEntity.CPU_SLOT, 53, 26));
        addSlot(new RelaySlot(relayInventory, RelayBlockEntity.MEMORY_SLOT, 71, 26));
        addSlot(new RelaySlot(relayInventory, RelayBlockEntity.HDD_SLOT, 89, 26));
        addSlot(new RelaySlot(relayInventory, RelayBlockEntity.CARD_SLOT, 107, 26));
        addPlayerInventory(playerInventory);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        ItemStack moved = ItemStack.EMPTY;
        final Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            final ItemStack stack = slot.getItem();
            moved = stack.copy();
            if (index < RELAY_SLOT_COUNT) {
                if (!moveItemStackTo(stack, RELAY_SLOT_COUNT, TOTAL_SLOT_COUNT, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, RELAY_SLOT_COUNT, false)) {
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
        return relayInventory.stillValid(player);
    }

    public int relayMode() {
        return relayData.get(RELAY_MODE_INDEX);
    }

    public int relayDelay() {
        return relayData.get(RELAY_DELAY_INDEX);
    }

    public int relayQueueSize() {
        return relayData.get(RELAY_QUEUE_SIZE_INDEX);
    }

    public int relayMaxQueueSize() {
        return relayData.get(RELAY_MAX_QUEUE_SIZE_INDEX);
    }

    public int relayStrength() {
        return relayData.get(RELAY_STRENGTH_INDEX);
    }

    public int relayRepeater() {
        return relayData.get(RELAY_REPEATER_INDEX);
    }

    public static int relayModeFor(final Container relayInventory) {
        if (!(relayInventory instanceof RelayBlockEntity relay)) {
            return MODE_WIRED;
        }
        if (relay.isLinkedEnabled()) {
            return MODE_LINKED;
        }
        return relay.isWirelessEnabled() ? MODE_WIRELESS : MODE_WIRED;
    }

    public static int relayDelayFor(final Container relayInventory) {
        return relayInventory instanceof RelayBlockEntity relay ? relay.relayDelay() : 0;
    }

    public static int relayQueueSizeFor(final Container relayInventory) {
        return relayInventory instanceof RelayBlockEntity relay ? relay.queuedPackets() : 0;
    }

    public static int relayMaxQueueSizeFor(final Container relayInventory) {
        return relayInventory instanceof RelayBlockEntity relay ? relay.maxQueueSize() : 0;
    }

    public static int relayStrengthFor(final Container relayInventory) {
        return relayInventory instanceof RelayBlockEntity relay ? relay.wirelessStrength() : 0;
    }

    public static int relayRepeaterFor(final Container relayInventory) {
        return relayInventory instanceof RelayBlockEntity relay && relay.isRepeaterEnabled() ? 1 : 0;
    }

    @Override
    public void removed(final Player player) {
        super.removed(player);
        relayInventory.stopOpen(player);
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

    private static ContainerData relayData(final Container relayInventory) {
        return new ContainerData() {
            @Override
            public int get(final int index) {
                return switch (index) {
                    case RELAY_MODE_INDEX -> relayModeFor(relayInventory);
                    case RELAY_DELAY_INDEX -> relayDelayFor(relayInventory);
                    case RELAY_QUEUE_SIZE_INDEX -> relayQueueSizeFor(relayInventory);
                    case RELAY_MAX_QUEUE_SIZE_INDEX -> relayMaxQueueSizeFor(relayInventory);
                    case RELAY_STRENGTH_INDEX -> relayStrengthFor(relayInventory);
                    case RELAY_REPEATER_INDEX -> relayRepeaterFor(relayInventory);
                    default -> 0;
                };
            }

            @Override
            public void set(final int index, final int value) {
            }

            @Override
            public int getCount() {
                return RELAY_DATA_COUNT;
            }
        };
    }

    private static final class RelaySlot extends Slot {
        private RelaySlot(final Container container, final int slot, final int x, final int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(final ItemStack stack) {
            return container.canPlaceItem(getSlotIndex(), stack);
        }
    }
}
