package li.cil.oc.common.menu;

import li.cil.oc.common.component.ServerRackMountableEnvironment;
import li.cil.oc.common.ModMenus;
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
    public static final int SERVER_DATA_COUNT = SERVER_SLOT_COUNT * 2;

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
    private static final int FIRST_SERVER_SLOT_X = 8;
    private static final int FIRST_SERVER_SLOT_Y = 18;

    private final Container serverInventory;
    private final ContainerData serverData;

    public ServerRackMenu(final int containerId, final Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(SERVER_SLOT_COUNT), new SimpleContainerData(SERVER_DATA_COUNT));
    }

    public ServerRackMenu(final int containerId, final Inventory playerInventory, final Container serverInventory) {
        this(containerId, playerInventory, serverInventory, serverData(serverInventory));
    }

    public ServerRackMenu(final int containerId, final Inventory playerInventory, final Container serverInventory, final ContainerData serverData) {
        super(ModMenus.SERVER_RACK.get(), containerId);
        this.serverInventory = serverInventory;
        this.serverData = serverData;
        serverInventory.startOpen(playerInventory.player);
        addDataSlots(serverData);

        for (int slot = 0; slot < SERVER_SLOT_COUNT; slot++) {
            addSlot(new ServerRackSlot(serverInventory, slot, FIRST_SERVER_SLOT_X + (slot % 9) * 18, FIRST_SERVER_SLOT_Y + (slot / 9) * 18));
        }
        addPlayerInventory(playerInventory);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        ItemStack moved = ItemStack.EMPTY;
        final Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
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

    public int slotKind(final int slot) {
        return slot >= 0 && slot < SERVER_SLOT_COUNT ? serverData.get(slot) : SLOT_KIND_NONE;
    }

    public int slotTierLimit(final int slot) {
        return slot >= 0 && slot < SERVER_SLOT_COUNT ? serverData.get(SERVER_SLOT_COUNT + slot) : -1;
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
                return slotTierLimitFor(serverInventory, index - SERVER_SLOT_COUNT);
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
}
