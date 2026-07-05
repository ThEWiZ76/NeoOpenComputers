package li.cil.oc.common.menu;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ModMenus;
import li.cil.oc.common.blockentity.RobotBlockEntity;
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

public class RobotMenu extends AbstractContainerMenu {
    public static final int MIN_ROBOT_SLOT_COUNT = RobotBlockEntity.slotCount(0);
    public static final int MAX_ROBOT_SLOT_COUNT = RobotBlockEntity.slotCount(2);
    public static final int PLAYER_SLOT_COUNT = 36;
    public static final int ROBOT_STATUS_INDEX = 0;
    public static final int ROBOT_MISSING_REQUIREMENTS_INDEX = 1;
    public static final int ROBOT_COMPONENT_COUNT_INDEX = 2;
    public static final int ROBOT_MAX_COMPONENTS_INDEX = 3;
    public static final int ROBOT_TIER_INDEX = 4;
    public static final int ROBOT_ENERGY_INDEX = 5;
    public static final int ROBOT_MAX_ENERGY_INDEX = 6;
    public static final int ROBOT_DATA_COUNT = 7;
    public static final int STATE_EMPTY = ComputerCaseMenu.STATE_EMPTY;
    public static final int STATE_READY = ComputerCaseMenu.STATE_READY;
    public static final int STATE_RUNNING = ComputerCaseMenu.STATE_RUNNING;
    public static final int STATE_INCOMPLETE = ComputerCaseMenu.STATE_INCOMPLETE;
    public static final int MISSING_CPU = ComputerCaseMenu.MISSING_CPU;
    public static final int MISSING_MEMORY = ComputerCaseMenu.MISSING_MEMORY;
    public static final int MISSING_EEPROM = ComputerCaseMenu.MISSING_EEPROM;

    private static final int PLAYER_INVENTORY_X = 6;
    private static final int PLAYER_INVENTORY_Y = 174;
    private static final int PLAYER_HOTBAR_Y = 232;
    private static final int[][] ROBOT_SLOT_POSITIONS = {
        {170, 232}, {188, 232}, {206, 232}, {224, 232},
        {170, 156}, {188, 156}, {206, 156}, {224, 156},
        {170, 174}, {188, 174}, {206, 174}, {224, 174},
        {170, 192}, {188, 192}, {206, 192}, {224, 192},
        {170, 210}, {188, 210}, {206, 210}, {224, 210},
        {152, 232}
    };

    private final Container robotInventory;
    private final ContainerData robotData;
    private final int robotSlotCount;

    public RobotMenu(final int containerId, final Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(MAX_ROBOT_SLOT_COUNT), new SimpleContainerData(ROBOT_DATA_COUNT));
    }

    public RobotMenu(final int containerId, final Inventory playerInventory, final RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, new SimpleContainer(MAX_ROBOT_SLOT_COUNT), clientRobotData(extraData));
    }

    public RobotMenu(final int containerId, final Inventory playerInventory, final RobotBlockEntity robot) {
        this(containerId, playerInventory, robot, robotData(robot));
    }

    public RobotMenu(final int containerId, final Inventory playerInventory, final Container robotInventory, final ContainerData robotData) {
        super(ModMenus.ROBOT.get(), containerId);
        checkContainerSize(robotInventory, MIN_ROBOT_SLOT_COUNT);
        this.robotInventory = robotInventory;
        this.robotData = robotData;
        robotSlotCount = robotSlotCountForTier(robotTier());
        robotInventory.startOpen(playerInventory.player);
        addDataSlots(robotData);

        for (int slot = 0; slot < robotSlotCount; slot++) {
            final int[] position = slotPosition(slot);
            addSlot(new RobotSlot(robotInventory, slot, position[0], position[1]));
        }
        addPlayerInventory(playerInventory);
    }

    public static int robotSlotX(final int tier, final int slot) {
        return slot < robotSlotCountForTier(tier) ? slotPosition(slot)[0] : -1;
    }

    public static int robotSlotY(final int tier, final int slot) {
        return slot < robotSlotCountForTier(tier) ? slotPosition(slot)[1] : -1;
    }

    public static int robotSlotCountForTier(final int tier) {
        return RobotBlockEntity.slotCount(tier);
    }

    public static String robotSlotKind(final int tier, final int slot) {
        return RobotBlockEntity.slotType(tier, slot);
    }

    public static int robotSlotTierLimit(final int tier, final int slot) {
        return RobotBlockEntity.slotTier(tier, slot);
    }

    public int robotTier() {
        return robotData.get(ROBOT_TIER_INDEX);
    }

    public int robotState() {
        return robotData.get(ROBOT_STATUS_INDEX);
    }

    public int missingRequirements() {
        return robotData.get(ROBOT_MISSING_REQUIREMENTS_INDEX);
    }

    public int componentCount() {
        return robotData.get(ROBOT_COMPONENT_COUNT_INDEX);
    }

    public int maxComponents() {
        return robotData.get(ROBOT_MAX_COMPONENTS_INDEX);
    }

    public int energy() {
        return robotData.get(ROBOT_ENERGY_INDEX);
    }

    public int maxEnergy() {
        return robotData.get(ROBOT_MAX_ENERGY_INDEX);
    }

    public Container robotInventory() {
        return robotInventory;
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        ItemStack moved = ItemStack.EMPTY;
        final Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            final ItemStack stack = slot.getItem();
            moved = stack.copy();
            if (index < robotSlotCount) {
                if (!moveItemStackTo(stack, robotSlotCount, robotSlotCount + PLAYER_SLOT_COUNT, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!movePlayerStackToRobot(stack)) {
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

    private boolean movePlayerStackToRobot(final ItemStack stack) {
        final int menuTier = robotTier();
        return movePlayerStackToRobotSlots(stack, menuTier, false)
            || movePlayerStackToRobotSlots(stack, menuTier, true);
    }

    private boolean movePlayerStackToRobotSlots(final ItemStack stack, final int menuTier, final boolean containerSlots) {
        for (int slot = 0; slot < robotSlotCount && !stack.isEmpty(); slot++) {
            final boolean isContainerSlot = li.cil.oc.api.driver.item.Slot.Container.equals(RobotBlockEntity.slotType(menuTier, slot));
            if (isContainerSlot != containerSlots) {
                continue;
            }
            if (getSlot(slot).mayPlace(stack) && moveItemStackTo(stack, slot, slot + 1, false)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean stillValid(final Player player) {
        return robotInventory.stillValid(player);
    }

    @Override
    public void removed(final Player player) {
        super.removed(player);
        robotInventory.stopOpen(player);
    }

    public static int robotStateFor(final Container robotInventory) {
        if (!(robotInventory instanceof RobotBlockEntity robot)) {
            return STATE_EMPTY;
        }
        if (robot.machine().isRunning() || robot.machine().isPaused()) {
            return STATE_RUNNING;
        }
        return missingRequirementsFor(robotInventory) == 0 ? STATE_READY : STATE_INCOMPLETE;
    }

    public static int missingRequirementsFor(final Container robotInventory) {
        if (!(robotInventory instanceof RobotBlockEntity)) {
            return 0;
        }
        boolean hasCpu = false;
        boolean hasMemory = false;
        boolean hasEeprom = false;
        for (int slot = 0; slot < robotInventory.getContainerSize(); slot++) {
            final ItemStack stack = robotInventory.getItem(slot);
            if (stack.isEmpty() || !robotInventory.canPlaceItem(slot, stack)) {
                continue;
            }
            final DriverItem driver = Driver.driverFor(stack, li.cil.oc.api.internal.Robot.class);
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

    public static int componentCountFor(final Container robotInventory) {
        return robotInventory instanceof RobotBlockEntity robot ? robot.machine().componentCount() : 0;
    }

    public static int maxComponentsFor(final Container robotInventory) {
        return robotInventory instanceof RobotBlockEntity robot ? robot.machine().maxComponents() : 0;
    }

    public static int robotTierFor(final Container robotInventory) {
        return robotInventory instanceof RobotBlockEntity robot ? robot.tier() : 0;
    }

    public static int energyFor(final Container robotInventory) {
        if (!(robotInventory instanceof RobotBlockEntity robot) || !(robot.machine().node() instanceof Connector connector)) {
            return 0;
        }
        return clampEnergy(connector.globalBuffer());
    }

    public static int maxEnergyFor(final Container robotInventory) {
        if (!(robotInventory instanceof RobotBlockEntity robot) || !(robot.machine().node() instanceof Connector connector)) {
            return 0;
        }
        return clampEnergy(connector.globalBufferSize());
    }

    private static ContainerData robotData(final Container robotInventory) {
        return new ServerRobotData(robotInventory);
    }

    private static ContainerData clientRobotData(final RegistryFriendlyByteBuf extraData) {
        final SimpleContainerData data = new SimpleContainerData(ROBOT_DATA_COUNT);
        if (extraData != null) {
            data.set(ROBOT_TIER_INDEX, extraData.readVarInt());
        }
        return data;
    }

    private static int[] slotPosition(final int slot) {
        return slot >= 0 && slot < ROBOT_SLOT_POSITIONS.length ? ROBOT_SLOT_POSITIONS[slot] : new int[]{-1, -1};
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

    static final class RobotSlot extends Slot {
        RobotSlot(final Container container, final int slot, final int x, final int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(final ItemStack stack) {
            return container.canPlaceItem(getSlotIndex(), stack);
        }
    }

    static final class ServerRobotData implements ContainerData {
        private final Container robotInventory;

        ServerRobotData(final Container robotInventory) {
            this.robotInventory = robotInventory;
        }

        @Override
        public int get(final int index) {
            return switch (index) {
                case ROBOT_STATUS_INDEX -> robotStateFor(robotInventory);
                case ROBOT_MISSING_REQUIREMENTS_INDEX -> missingRequirementsFor(robotInventory);
                case ROBOT_COMPONENT_COUNT_INDEX -> componentCountFor(robotInventory);
                case ROBOT_MAX_COMPONENTS_INDEX -> maxComponentsFor(robotInventory);
                case ROBOT_TIER_INDEX -> robotTierFor(robotInventory);
                case ROBOT_ENERGY_INDEX -> energyFor(robotInventory);
                case ROBOT_MAX_ENERGY_INDEX -> maxEnergyFor(robotInventory);
                default -> 0;
            };
        }

        @Override
        public void set(final int index, final int value) {
        }

        @Override
        public int getCount() {
            return ROBOT_DATA_COUNT;
        }
    }

    private static int clampEnergy(final double value) {
        if (Double.isNaN(value) || value <= 0D) {
            return 0;
        }
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }
}
