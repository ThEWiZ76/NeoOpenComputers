package li.cil.oc.common.menu;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.internal.Drone;
import li.cil.oc.common.ModMenus;
import li.cil.oc.common.entity.DroneEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DroneMenu extends AbstractContainerMenu {
    public static final int MIN_DRONE_SLOT_COUNT = DroneEntity.slotCount(0);
    public static final int MAX_DRONE_SLOT_COUNT = DroneEntity.slotCount(2);
    public static final int PLAYER_SLOT_COUNT = 36;
    public static final int DRONE_STATUS_INDEX = 0;
    public static final int DRONE_MISSING_REQUIREMENTS_INDEX = 1;
    public static final int DRONE_COMPONENT_COUNT_INDEX = 2;
    public static final int DRONE_MAX_COMPONENTS_INDEX = 3;
    public static final int DRONE_TIER_INDEX = 4;
    public static final int DRONE_DATA_COUNT = 5;
    public static final int STATE_EMPTY = ComputerCaseMenu.STATE_EMPTY;
    public static final int STATE_READY = ComputerCaseMenu.STATE_READY;
    public static final int STATE_RUNNING = ComputerCaseMenu.STATE_RUNNING;
    public static final int STATE_INCOMPLETE = ComputerCaseMenu.STATE_INCOMPLETE;
    public static final int MISSING_CPU = ComputerCaseMenu.MISSING_CPU;
    public static final int MISSING_MEMORY = ComputerCaseMenu.MISSING_MEMORY;
    public static final int MISSING_EEPROM = ComputerCaseMenu.MISSING_EEPROM;

    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 84;
    private static final int PLAYER_HOTBAR_Y = 142;
    private static final int[][] DRONE_SLOT_POSITIONS = {
        {8, 16}, {26, 16}, {44, 16}, {62, 16}, {80, 16}, {98, 16}, {116, 16}, {134, 16},
        {8, 34}, {26, 34}, {44, 34}, {62, 34}, {80, 34}, {98, 34}, {116, 34}, {134, 34}
    };

    private final Container droneInventory;
    private final ContainerData droneData;
    private final int droneSlotCount;

    public DroneMenu(final int containerId, final Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(MAX_DRONE_SLOT_COUNT), new SimpleContainerData(DRONE_DATA_COUNT));
    }

    public DroneMenu(final int containerId, final Inventory playerInventory, final RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, new SimpleContainer(MAX_DRONE_SLOT_COUNT), clientDroneData(extraData));
    }

    public DroneMenu(final int containerId, final Inventory playerInventory, final DroneEntity drone) {
        this(containerId, playerInventory, drone, droneData(drone));
    }

    public DroneMenu(final int containerId, final Inventory playerInventory, final Container droneInventory, final ContainerData droneData) {
        super(ModMenus.DRONE.get(), containerId);
        checkContainerSize(droneInventory, MIN_DRONE_SLOT_COUNT);
        this.droneInventory = droneInventory;
        this.droneData = droneData;
        droneSlotCount = droneSlotCountForTier(droneTier());
        droneInventory.startOpen(playerInventory.player);
        addDataSlots(droneData);

        for (int slot = 0; slot < droneSlotCount; slot++) {
            final int[] position = slotPosition(slot);
            addSlot(new DroneSlot(droneInventory, slot, position[0], position[1]));
        }
        addPlayerInventory(playerInventory);
    }

    public static int droneSlotX(final int tier, final int slot) {
        return slot < droneSlotCountForTier(tier) ? slotPosition(slot)[0] : -1;
    }

    public static int droneSlotY(final int tier, final int slot) {
        return slot < droneSlotCountForTier(tier) ? slotPosition(slot)[1] : -1;
    }

    public static int droneSlotCountForTier(final int tier) {
        return DroneEntity.slotCount(tier);
    }

    public static String droneSlotKind(final int tier, final int slot) {
        return DroneEntity.slotType(tier, slot);
    }

    public static int droneSlotTierLimit(final int tier, final int slot) {
        return DroneEntity.slotTier(tier, slot);
    }

    public int droneTier() {
        return droneData.get(DRONE_TIER_INDEX);
    }

    public int droneState() {
        return droneData.get(DRONE_STATUS_INDEX);
    }

    public int missingRequirements() {
        return droneData.get(DRONE_MISSING_REQUIREMENTS_INDEX);
    }

    public int componentCount() {
        return droneData.get(DRONE_COMPONENT_COUNT_INDEX);
    }

    public int maxComponents() {
        return droneData.get(DRONE_MAX_COMPONENTS_INDEX);
    }

    public Container droneInventory() {
        return droneInventory;
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        ItemStack moved = ItemStack.EMPTY;
        final Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            final ItemStack stack = slot.getItem();
            moved = stack.copy();
            if (index < droneSlotCount) {
                if (!moveItemStackTo(stack, droneSlotCount, droneSlotCount + PLAYER_SLOT_COUNT, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, droneSlotCount, false)) {
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
        return droneInventory.stillValid(player);
    }

    @Override
    public void removed(final Player player) {
        super.removed(player);
        droneInventory.stopOpen(player);
    }

    public static int droneStateFor(final Container droneInventory) {
        if (!(droneInventory instanceof DroneEntity drone)) {
            return STATE_EMPTY;
        }
        if (drone.machine().isRunning() || drone.machine().isPaused()) {
            return STATE_RUNNING;
        }
        return missingRequirementsFor(droneInventory) == 0 ? STATE_READY : STATE_INCOMPLETE;
    }

    public static int missingRequirementsFor(final Container droneInventory) {
        if (!(droneInventory instanceof DroneEntity)) {
            return 0;
        }
        boolean hasCpu = false;
        boolean hasMemory = false;
        boolean hasEeprom = false;
        for (int slot = 0; slot < droneInventory.getContainerSize(); slot++) {
            final ItemStack stack = droneInventory.getItem(slot);
            if (stack.isEmpty() || !droneInventory.canPlaceItem(slot, stack)) {
                continue;
            }
            final DriverItem driver = Driver.driverFor(stack, Drone.class);
            if (driver == null) {
                continue;
            }
            final String slotType = driver.slot(stack);
            if (li.cil.oc.api.driver.item.Slot.CPU.equals(slotType)) {
                hasCpu = true;
            } else if (li.cil.oc.api.driver.item.Slot.Memory.equals(slotType)) {
                hasMemory = true;
            } else if ("eeprom".equals(slotType)) {
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

    public static int componentCountFor(final Container droneInventory) {
        return droneInventory instanceof DroneEntity drone ? drone.machine().componentCount() : 0;
    }

    public static int maxComponentsFor(final Container droneInventory) {
        return droneInventory instanceof DroneEntity drone ? drone.machine().maxComponents() : 0;
    }

    public static int droneTierFor(final Container droneInventory) {
        return droneInventory instanceof DroneEntity drone ? drone.tier() : 0;
    }

    private static ContainerData droneData(final Container droneInventory) {
        return new ServerDroneData(droneInventory);
    }

    private static ContainerData clientDroneData(final RegistryFriendlyByteBuf extraData) {
        final SimpleContainerData data = new SimpleContainerData(DRONE_DATA_COUNT);
        if (extraData != null) {
            data.set(DRONE_TIER_INDEX, extraData.readVarInt());
        }
        return data;
    }

    private static int[] slotPosition(final int slot) {
        return slot >= 0 && slot < DRONE_SLOT_POSITIONS.length ? DRONE_SLOT_POSITIONS[slot] : new int[]{-1, -1};
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

    static final class DroneSlot extends Slot {
        DroneSlot(final Container container, final int slot, final int x, final int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(final ItemStack stack) {
            return container.canPlaceItem(getSlotIndex(), stack);
        }
    }

    static final class ServerDroneData implements ContainerData {
        private final Container droneInventory;

        ServerDroneData(final Container droneInventory) {
            this.droneInventory = droneInventory;
        }

        @Override
        public int get(final int index) {
            return switch (index) {
                case DRONE_STATUS_INDEX -> droneStateFor(droneInventory);
                case DRONE_MISSING_REQUIREMENTS_INDEX -> missingRequirementsFor(droneInventory);
                case DRONE_COMPONENT_COUNT_INDEX -> componentCountFor(droneInventory);
                case DRONE_MAX_COMPONENTS_INDEX -> maxComponentsFor(droneInventory);
                case DRONE_TIER_INDEX -> droneTierFor(droneInventory);
                default -> 0;
            };
        }

        @Override
        public void set(final int index, final int value) {
        }

        @Override
        public int getCount() {
            return DRONE_DATA_COUNT;
        }
    }
}
