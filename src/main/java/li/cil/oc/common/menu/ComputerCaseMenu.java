package li.cil.oc.common.menu;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.common.ModMenus;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ComputerCaseMenu extends AbstractContainerMenu {
    public static final int MIN_COMPUTER_SLOT_COUNT = ComputerCaseBlockEntity.CONTAINER_SIZE;
    public static final int MAX_COMPUTER_SLOT_COUNT = 10;
    public static final int COMPUTER_SLOT_COUNT = MAX_COMPUTER_SLOT_COUNT;
    public static final int PLAYER_SLOT_COUNT = 36;
    public static final int TOTAL_SLOT_COUNT = COMPUTER_SLOT_COUNT + PLAYER_SLOT_COUNT;
    public static final int MAX_TOTAL_SLOT_COUNT = MAX_COMPUTER_SLOT_COUNT + PLAYER_SLOT_COUNT;
    public static final int COMPUTER_STATUS_INDEX = 0;
    public static final int COMPUTER_MISSING_REQUIREMENTS_INDEX = 1;
    public static final int COMPUTER_COMPONENT_COUNT_INDEX = 2;
    public static final int COMPUTER_MAX_COMPONENTS_INDEX = 3;
    public static final int COMPUTER_DATA_COUNT = 4;

    public static final int STATE_EMPTY = 0;
    public static final int STATE_READY = 1;
    public static final int STATE_RUNNING = 2;
    public static final int STATE_INCOMPLETE = 3;
    public static final int MISSING_CPU = 1;
    public static final int MISSING_MEMORY = 2;
    public static final int MISSING_EEPROM = 4;

    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 84;
    private static final int PLAYER_HOTBAR_Y = 142;
    private static final String SLOT_TYPE_EEPROM = "eeprom";
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
    private final ContainerData computerData;
    private final int computerSlotCount;

    public ComputerCaseMenu(final int containerId, final Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(MAX_COMPUTER_SLOT_COUNT), new SimpleContainerData(COMPUTER_DATA_COUNT));
    }

    public ComputerCaseMenu(final int containerId, final Inventory playerInventory, final Container computerInventory) {
        this(containerId, playerInventory, computerInventory, computerData(computerInventory));
    }

    public ComputerCaseMenu(final int containerId, final Inventory playerInventory, final Container computerInventory, final ContainerData computerData) {
        super(ModMenus.COMPUTER_CASE.get(), containerId);
        checkContainerSize(computerInventory, MIN_COMPUTER_SLOT_COUNT);
        this.computerInventory = computerInventory;
        this.computerData = computerData;
        computerSlotCount = COMPUTER_SLOT_COUNT;
        computerInventory.startOpen(playerInventory.player);
        addDataSlots(computerData);

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

    public int computerState() {
        return computerData.get(COMPUTER_STATUS_INDEX);
    }

    public int missingRequirements() {
        return computerData.get(COMPUTER_MISSING_REQUIREMENTS_INDEX);
    }

    public int componentCount() {
        return computerData.get(COMPUTER_COMPONENT_COUNT_INDEX);
    }

    public int maxComponents() {
        return computerData.get(COMPUTER_MAX_COMPONENTS_INDEX);
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

    public static int computerStateFor(final Container computerInventory) {
        if (!(computerInventory instanceof ComputerCaseBlockEntity computer)) {
            return STATE_EMPTY;
        }
        if (computer.machine().isRunning() || computer.machine().isPaused()) {
            return STATE_RUNNING;
        }
        return missingRequirementsFor(computerInventory) == 0 ? STATE_READY : STATE_INCOMPLETE;
    }

    public static int missingRequirementsFor(final Container computerInventory) {
        if (!(computerInventory instanceof ComputerCaseBlockEntity)) {
            return 0;
        }
        boolean hasCpu = false;
        boolean hasMemory = false;
        boolean hasEeprom = false;
        for (int slot = 0; slot < computerInventory.getContainerSize(); slot++) {
            final ItemStack stack = computerInventory.getItem(slot);
            if (stack.isEmpty() || !computerInventory.canPlaceItem(slot, stack)) {
                continue;
            }
            final DriverItem driver = Driver.driverFor(stack);
            if (driver == null) {
                continue;
            }
            final String slotType = driver.slot(stack);
            if (li.cil.oc.api.driver.item.Slot.CPU.equals(slotType)) {
                hasCpu = true;
            } else if (li.cil.oc.api.driver.item.Slot.Memory.equals(slotType)) {
                hasMemory = true;
            } else if (SLOT_TYPE_EEPROM.equals(slotType)) {
                hasEeprom = true;
            }
        }
        int missing = 0;
        if (!hasCpu) {
            missing |= MISSING_CPU;
        }
        if (!hasMemory) {
            missing |= MISSING_MEMORY;
        }
        if (!hasEeprom) {
            missing |= MISSING_EEPROM;
        }
        return missing;
    }

    public static int componentCountFor(final Container computerInventory) {
        return computerInventory instanceof ComputerCaseBlockEntity computer ? computer.machine().componentCount() : 0;
    }

    public static int maxComponentsFor(final Container computerInventory) {
        return computerInventory instanceof ComputerCaseBlockEntity computer ? computer.machine().maxComponents() : 0;
    }

    private static ContainerData computerData(final Container computerInventory) {
        return new ContainerData() {
            @Override
            public int get(final int index) {
                return switch (index) {
                    case COMPUTER_STATUS_INDEX -> computerStateFor(computerInventory);
                    case COMPUTER_MISSING_REQUIREMENTS_INDEX -> missingRequirementsFor(computerInventory);
                    case COMPUTER_COMPONENT_COUNT_INDEX -> componentCountFor(computerInventory);
                    case COMPUTER_MAX_COMPONENTS_INDEX -> maxComponentsFor(computerInventory);
                    default -> 0;
                };
            }

            @Override
            public void set(final int index, final int value) {
            }

            @Override
            public int getCount() {
                return COMPUTER_DATA_COUNT;
            }
        };
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
