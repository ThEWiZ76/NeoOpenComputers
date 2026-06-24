package li.cil.oc.common.menu;

import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.util.StateAware;
import li.cil.oc.common.ModMenus;
import li.cil.oc.common.blockentity.RackBlockEntity;
import li.cil.oc.common.component.ServerRackMountableEnvironment;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class RackMenu extends AbstractContainerMenu {
    public static final int RACK_SLOT_COUNT = RackBlockEntity.CONTAINER_SIZE;
    public static final int PLAYER_SLOT_COUNT = 36;
    public static final int TOTAL_SLOT_COUNT = RACK_SLOT_COUNT + PLAYER_SLOT_COUNT;
    public static final int RACK_STATE_COUNT = RACK_SLOT_COUNT;
    public static final int RACK_MISSING_REQUIREMENTS_COUNT = RACK_SLOT_COUNT;
    public static final int RACK_NODE_MAPPING_COUNT = RACK_SLOT_COUNT * 4;
    public static final int RACK_NODE_PRESENCE_COUNT = RACK_SLOT_COUNT * 4;
    public static final int RACK_MISSING_REQUIREMENTS_OFFSET = RACK_STATE_COUNT;
    public static final int RACK_NODE_MAPPING_OFFSET = RACK_MISSING_REQUIREMENTS_OFFSET + RACK_MISSING_REQUIREMENTS_COUNT;
    public static final int RACK_NODE_PRESENCE_OFFSET = RACK_NODE_MAPPING_OFFSET + RACK_NODE_MAPPING_COUNT;
    public static final int RACK_DATA_COUNT = RACK_NODE_PRESENCE_OFFSET + RACK_NODE_PRESENCE_COUNT;
    public static final int NO_SIDE = -1;
    public static final int STATE_EMPTY = 0;
    public static final int STATE_READY = 1;
    public static final int STATE_RUNNING = 2;
    public static final int STATE_INCOMPLETE = 3;
    public static final int MISSING_CPU = ServerRackMountableEnvironment.MISSING_CPU;
    public static final int MISSING_MEMORY = ServerRackMountableEnvironment.MISSING_MEMORY;
    public static final int MISSING_EEPROM = ServerRackMountableEnvironment.MISSING_EEPROM;

    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 84;
    private static final int PLAYER_HOTBAR_Y = 142;

    private final Container rackInventory;
    private final ContainerData rackData;

    public RackMenu(final int containerId, final Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(RACK_SLOT_COUNT), new SimpleContainerData(RACK_DATA_COUNT));
    }

    public RackMenu(final int containerId, final Inventory playerInventory, final Container rackInventory) {
        this(containerId, playerInventory, rackInventory, rackData(rackInventory));
    }

    public RackMenu(final int containerId, final Inventory playerInventory, final Container rackInventory, final ContainerData rackData) {
        super(ModMenus.RACK.get(), containerId);
        checkContainerSize(rackInventory, RACK_SLOT_COUNT);
        checkContainerDataCount(rackData, RACK_DATA_COUNT);
        this.rackInventory = rackInventory;
        this.rackData = rackData;
        rackInventory.startOpen(playerInventory.player);

        addSlot(new RackSlot(rackInventory, 0, 53, 26));
        addSlot(new RackSlot(rackInventory, 1, 71, 26));
        addSlot(new RackSlot(rackInventory, 2, 89, 26));
        addSlot(new RackSlot(rackInventory, 3, 107, 26));
        addPlayerInventory(playerInventory);
        addDataSlots(rackData);
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

    public int rackState(final int slot) {
        if (slot < 0 || slot >= RACK_STATE_COUNT) {
            return STATE_EMPTY;
        }
        return rackData.get(slot);
    }

    public int rackMissingRequirements(final int slot) {
        if (slot < 0 || slot >= RACK_STATE_COUNT) {
            return 0;
        }
        return rackData.get(RACK_MISSING_REQUIREMENTS_OFFSET + slot);
    }

    public int rackNodeMapping(final int slot, final int connectableIndex) {
        if (!isValidNodeIndex(slot, connectableIndex)) {
            return NO_SIDE;
        }
        return rackData.get(RACK_NODE_MAPPING_OFFSET + slot * 4 + connectableIndex);
    }

    public boolean rackNodePresent(final int slot, final int connectableIndex) {
        return isValidNodeIndex(slot, connectableIndex)
            && rackData.get(RACK_NODE_PRESENCE_OFFSET + slot * 4 + connectableIndex) != 0;
    }

    public static int rackStateFor(final Container rackInventory, final int slot) {
        if (!(rackInventory instanceof RackBlockEntity rack) || slot < 0 || slot >= RACK_STATE_COUNT) {
            return STATE_EMPTY;
        }

        final RackMountable mountable = rack.getMountable(slot);
        if (mountable == null) {
            return STATE_EMPTY;
        }

        final var states = mountable.getCurrentState();
        if ((mountable instanceof ServerRackMountableEnvironment server && (server.machine().isRunning() || server.machine().isPaused()))
                || states.contains(StateAware.State.IsWorking)) {
            return STATE_RUNNING;
        }
        if (states.contains(StateAware.State.CanWork)) {
            return STATE_READY;
        }
        return STATE_INCOMPLETE;
    }

    public static int rackMissingRequirementsFor(final Container rackInventory, final int slot) {
        if (!(rackInventory instanceof RackBlockEntity rack) || slot < 0 || slot >= RACK_STATE_COUNT) {
            return 0;
        }
        return rack.getMountable(slot) instanceof ServerRackMountableEnvironment server ? server.missingRequiredComponents() : 0;
    }

    public static int rackNodeMappingFor(final Container rackInventory, final int slot, final int connectableIndex) {
        if (!(rackInventory instanceof RackBlockEntity rack) || !isValidNodeIndex(slot, connectableIndex)) {
            return NO_SIDE;
        }
        final Direction side = rack.mappedSide(slot, connectableIndex - 1);
        return side == null ? NO_SIDE : side.ordinal();
    }

    public static boolean rackNodePresentFor(final Container rackInventory, final int slot, final int connectableIndex) {
        if (!(rackInventory instanceof RackBlockEntity rack) || !isValidNodeIndex(slot, connectableIndex)) {
            return false;
        }
        final RackMountable mountable = rack.getMountable(slot);
        if (mountable == null) {
            return false;
        }
        if (connectableIndex == 0) {
            return mountable.node() != null;
        }
        final int rackConnectableIndex = connectableIndex - 1;
        return rackConnectableIndex < mountable.getConnectableCount()
            && mountable.getConnectableAt(rackConnectableIndex) != null;
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

    private static ContainerData rackData(final Container rackInventory) {
        if (!(rackInventory instanceof RackBlockEntity rack)) {
            return new SimpleContainerData(RACK_DATA_COUNT);
        }

        return new ContainerData() {
            @Override
            public int get(final int index) {
                if (index < RACK_STATE_COUNT) {
                    return rackStateFor(rack, index);
                }
                if (index < RACK_NODE_MAPPING_OFFSET) {
                    return rackMissingRequirementsFor(rack, index - RACK_MISSING_REQUIREMENTS_OFFSET);
                }
                if (index < RACK_NODE_PRESENCE_OFFSET) {
                    final int nodeIndex = index - RACK_NODE_MAPPING_OFFSET;
                    return rackNodeMappingFor(rack, nodeIndex / 4, nodeIndex % 4);
                }
                if (index < RACK_DATA_COUNT) {
                    final int nodeIndex = index - RACK_NODE_PRESENCE_OFFSET;
                    return rackNodePresentFor(rack, nodeIndex / 4, nodeIndex % 4) ? 1 : 0;
                }
                return 0;
            }

            @Override
            public void set(final int index, final int value) {
            }

            @Override
            public int getCount() {
                return RACK_DATA_COUNT;
            }
        };
    }

    private static boolean isValidNodeIndex(final int slot, final int connectableIndex) {
        return slot >= 0 && slot < RACK_SLOT_COUNT && connectableIndex >= 0 && connectableIndex < 4;
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
