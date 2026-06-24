package li.cil.oc.common.menu;

import li.cil.oc.common.component.ServerRackMountableEnvironment;
import li.cil.oc.common.ModMenus;
import li.cil.oc.api.util.StateAware;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ServerRackMenu extends AbstractContainerMenu {
    public static final int SERVER_SLOT_COUNT = 17;
    public static final int PLAYER_SLOT_COUNT = 36;
    public static final int TOTAL_SLOT_COUNT = SERVER_SLOT_COUNT + PLAYER_SLOT_COUNT;
    public static final int SERVER_STATUS_INDEX = SERVER_SLOT_COUNT * 2;
    public static final int SERVER_MISSING_REQUIREMENTS_INDEX = SERVER_STATUS_INDEX + 1;
    public static final int SERVER_COMPONENT_COUNT_INDEX = SERVER_MISSING_REQUIREMENTS_INDEX + 1;
    public static final int SERVER_MAX_COMPONENTS_INDEX = SERVER_COMPONENT_COUNT_INDEX + 1;
    public static final int SERVER_IS_ITEM_INDEX = SERVER_MAX_COMPONENTS_INDEX + 1;
    public static final int SERVER_PRESENT_INDEX = SERVER_IS_ITEM_INDEX + 1;
    public static final int SERVER_DATA_COUNT = SERVER_PRESENT_INDEX + 1;

    public static final int STATE_EMPTY = 0;
    public static final int STATE_READY = 1;
    public static final int STATE_RUNNING = 2;
    public static final int STATE_INCOMPLETE = 3;
    public static final int MISSING_CPU = ServerRackMountableEnvironment.MISSING_CPU;
    public static final int MISSING_MEMORY = ServerRackMountableEnvironment.MISSING_MEMORY;
    public static final int MISSING_EEPROM = ServerRackMountableEnvironment.MISSING_EEPROM;

    public static final int SLOT_KIND_NONE = 0;
    public static final int SLOT_KIND_CARD = 1;
    public static final int SLOT_KIND_CPU = 2;
    public static final int SLOT_KIND_COMPONENT_BUS = 3;
    public static final int SLOT_KIND_MEMORY = 4;
    public static final int SLOT_KIND_HDD = 5;
    public static final int SLOT_KIND_EEPROM = 6;

    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 84;
    private static final int PLAYER_HOTBAR_Y = 142;
    private static final int FIRST_COLUMN_X = 76;
    private static final int FIRST_SLOT_Y = 7;
    private static final int LAST_SLOT_X = 26;
    private static final int LAST_SLOT_Y = 34;
    private static final int SLOT_STRIDE = 18;
    private static final int HIDDEN_SLOT_X = -1000;
    private static final int HIDDEN_SLOT_Y = -1000;

    private final Container serverInventory;
    private final ContainerData serverData;
    private final ItemStack lockedStack;

    public ServerRackMenu(final int containerId, final Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(SERVER_SLOT_COUNT), clientData());
    }

    public ServerRackMenu(final int containerId, final Inventory playerInventory, final Container serverInventory) {
        this(containerId, playerInventory, serverInventory, serverData(serverInventory));
    }

    public ServerRackMenu(final int containerId, final Inventory playerInventory, final Container serverInventory, final ItemStack lockedStack) {
        this(containerId, playerInventory, serverInventory, serverData(serverInventory), lockedStack);
    }

    public ServerRackMenu(final int containerId, final Inventory playerInventory, final Container serverInventory, final ContainerData serverData) {
        this(containerId, playerInventory, serverInventory, serverData, ItemStack.EMPTY);
    }

    public ServerRackMenu(final int containerId, final Inventory playerInventory, final Container serverInventory, final ContainerData serverData, final ItemStack lockedStack) {
        super(ModMenus.SERVER_RACK.get(), containerId);
        this.serverInventory = serverInventory;
        this.serverData = serverData;
        this.lockedStack = lockedStack == null ? ItemStack.EMPTY : lockedStack;
        serverInventory.startOpen(playerInventory.player);
        addDataSlots(serverData);

        final int tier = serverTierFor(serverInventory);
        for (int slot = 0; slot < SERVER_SLOT_COUNT; slot++) {
            final ServerSlotPosition position = slotPositionForTier(tier, slot);
            addSlot(new ServerRackSlot(
                serverInventory,
                slot,
                position == null ? HIDDEN_SLOT_X : position.x(),
                position == null ? HIDDEN_SLOT_Y : position.y()));
        }
        addPlayerInventory(playerInventory);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        ItemStack moved = ItemStack.EMPTY;
        final Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            if (isLockedSlot(index)) {
                return ItemStack.EMPTY;
            }
            final ItemStack stack = slot.getItem();
            moved = stack.copy();
            if (index < SERVER_SLOT_COUNT) {
                if (!moveItemStackTo(stack, SERVER_SLOT_COUNT, TOTAL_SLOT_COUNT, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, SERVER_SLOT_COUNT, false)) {
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
        return serverInventory.stillValid(player);
    }

    public Container serverInventory() {
        return serverInventory;
    }

    public static Component serverTitle() {
        return Component.translatable("oc:container.server");
    }

    public ItemStack lockedStack() {
        return lockedStack;
    }

    public boolean isLockedStack(final ItemStack stack) {
        return !lockedStack.isEmpty()
            && stack != null
            && !stack.isEmpty()
            && (stack == lockedStack || ItemStack.isSameItem(stack, lockedStack));
    }

    public boolean isLockedSlot(final int index) {
        return index >= SERVER_SLOT_COUNT && index >= 0 && index < slots.size() && isLockedStack(slots.get(index).getItem());
    }

    public int slotKind(final int slot) {
        return slot >= 0 && slot < SERVER_SLOT_COUNT ? serverData.get(slot) : SLOT_KIND_NONE;
    }

    public int slotTierLimit(final int slot) {
        return slot >= 0 && slot < SERVER_SLOT_COUNT ? serverData.get(SERVER_SLOT_COUNT + slot) : -1;
    }

    public int serverState() {
        return serverData.get(SERVER_STATUS_INDEX);
    }

    public int missingRequirements() {
        return serverData.get(SERVER_MISSING_REQUIREMENTS_INDEX);
    }

    public int componentCount() {
        return serverData.get(SERVER_COMPONENT_COUNT_INDEX);
    }

    public int maxComponents() {
        return serverData.get(SERVER_MAX_COMPONENTS_INDEX);
    }

    public boolean isItem() {
        return serverData.get(SERVER_IS_ITEM_INDEX) != 0;
    }

    public boolean serverPresent() {
        return serverData.get(SERVER_PRESENT_INDEX) != 0;
    }

    @Override
    public void removed(final Player player) {
        super.removed(player);
        serverInventory.stopOpen(player);
    }

    public static int slotKindFor(final Container serverInventory, final int slot) {
        return serverInventory instanceof ServerRackMountableEnvironment server ? slotKindCode(server.slotTypeName(slot)) : SLOT_KIND_NONE;
    }

    public static int slotKindForTier(final int tier, final int slot) {
        return slotKindCode(ServerRackMountableEnvironment.slotTypeName(tier, slot));
    }

    public static int slotTierLimitFor(final Container serverInventory, final int slot) {
        return serverInventory instanceof ServerRackMountableEnvironment server ? server.slotTierLimit(slot) : -1;
    }

    public static int slotTierLimitForTier(final int tier, final int slot) {
        return ServerRackMountableEnvironment.slotTierLimit(tier, slot);
    }

    public static ServerSlotPosition slotPositionForTier(final int tier, final int slot) {
        final int verticalSlots = Math.min(3, 1 + Math.clamp(tier, 0, 2));
        int index = 0;
        for (int row = 0; row <= 1; row++) {
            if (slot == index++) {
                return new ServerSlotPosition(FIRST_COLUMN_X, FIRST_SLOT_Y + row * SLOT_STRIDE);
            }
        }
        for (final int columnX : new int[]{100, 124, 148}) {
            for (int row = 0; row <= verticalSlots; row++) {
                if (slot == index++) {
                    return new ServerSlotPosition(columnX, FIRST_SLOT_Y + row * SLOT_STRIDE);
                }
            }
        }
        for (int row = 2; row <= verticalSlots; row++) {
            if (slot == index++) {
                return new ServerSlotPosition(FIRST_COLUMN_X, FIRST_SLOT_Y + row * SLOT_STRIDE);
            }
        }
        if (slot == index) {
            return new ServerSlotPosition(LAST_SLOT_X, LAST_SLOT_Y);
        }
        return null;
    }

    public static int serverStateFor(final Container serverInventory) {
        if (!(serverInventory instanceof ServerRackMountableEnvironment server)) {
            return STATE_EMPTY;
        }
        final var states = server.getCurrentState();
        if (server.machine().isRunning() || server.machine().isPaused() || states.contains(StateAware.State.IsWorking)) {
            return STATE_RUNNING;
        }
        if (states.contains(StateAware.State.CanWork)) {
            return STATE_READY;
        }
        return STATE_INCOMPLETE;
    }

    public static int missingRequirementsFor(final Container serverInventory) {
        return serverInventory instanceof ServerRackMountableEnvironment server ? server.missingRequiredComponents() : 0;
    }

    public static int componentCountFor(final Container serverInventory) {
        return serverInventory instanceof ServerRackMountableEnvironment server ? server.machine().componentCount() : 0;
    }

    public static int maxComponentsFor(final Container serverInventory) {
        return serverInventory instanceof ServerRackMountableEnvironment server ? server.machine().maxComponents() : 0;
    }

    public static boolean isItemFor(final Container serverInventory) {
        return !(serverInventory instanceof ServerRackMountableEnvironment server) || server.rack() == null;
    }

    static ContainerData clientData() {
        final SimpleContainerData data = new SimpleContainerData(SERVER_DATA_COUNT);
        data.set(SERVER_PRESENT_INDEX, 1);
        return data;
    }

    public static boolean serverPresentFor(final Container serverInventory) {
        if (!(serverInventory instanceof ServerRackMountableEnvironment server)) {
            return false;
        }
        return server.rack() != null && server.rack().getMountable(server.slot()) == server;
    }

    public static int slotKindCode(final String type) {
        return switch (type) {
            case li.cil.oc.api.driver.item.Slot.Card -> SLOT_KIND_CARD;
            case li.cil.oc.api.driver.item.Slot.CPU -> SLOT_KIND_CPU;
            case li.cil.oc.api.driver.item.Slot.ComponentBus -> SLOT_KIND_COMPONENT_BUS;
            case li.cil.oc.api.driver.item.Slot.Memory -> SLOT_KIND_MEMORY;
            case li.cil.oc.api.driver.item.Slot.HDD -> SLOT_KIND_HDD;
            case ServerRackMountableEnvironment.SLOT_TYPE_EEPROM -> SLOT_KIND_EEPROM;
            default -> SLOT_KIND_NONE;
        };
    }

    private static ContainerData serverData(final Container serverInventory) {
        return new ContainerData() {
            @Override
            public int get(final int index) {
                if (index < 0 || index >= SERVER_DATA_COUNT) {
                    return 0;
                }
                if (index < SERVER_SLOT_COUNT) {
                    return slotKindFor(serverInventory, index);
                }
                if (index < SERVER_STATUS_INDEX) {
                    return slotTierLimitFor(serverInventory, index - SERVER_SLOT_COUNT);
                }
                if (index == SERVER_STATUS_INDEX) {
                    return serverStateFor(serverInventory);
                }
                if (index == SERVER_MISSING_REQUIREMENTS_INDEX) {
                    return missingRequirementsFor(serverInventory);
                }
                if (index == SERVER_COMPONENT_COUNT_INDEX) {
                    return componentCountFor(serverInventory);
                }
                if (index == SERVER_MAX_COMPONENTS_INDEX) {
                    return maxComponentsFor(serverInventory);
                }
                if (index == SERVER_IS_ITEM_INDEX) {
                    return isItemFor(serverInventory) ? 1 : 0;
                }
                return serverPresentFor(serverInventory) ? 1 : 0;
            }

            @Override
            public void set(final int index, final int value) {
            }

            @Override
            public int getCount() {
                return SERVER_DATA_COUNT;
            }
        };
    }

    private static int serverTierFor(final Container serverInventory) {
        if (serverInventory instanceof ServerRackMountableEnvironment server) {
            return server.tier();
        }
        final int size = serverInventory.getContainerSize();
        if (size >= ServerRackMountableEnvironment.slotCountForTier(2)) {
            return 2;
        }
        if (size >= ServerRackMountableEnvironment.slotCountForTier(1)) {
            return 1;
        }
        return 0;
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

    private static final class ServerRackSlot extends Slot {
        private ServerRackSlot(final Container container, final int slot, final int x, final int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(final ItemStack stack) {
            return container.canPlaceItem(getSlotIndex(), stack);
        }
    }

    public record ServerSlotPosition(int x, int y) {
    }
}
