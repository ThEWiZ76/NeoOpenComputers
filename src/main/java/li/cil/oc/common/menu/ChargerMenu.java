package li.cil.oc.common.menu;

import li.cil.oc.common.ModMenus;
import li.cil.oc.common.blockentity.ChargerBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ChargerMenu extends AbstractContainerMenu {
    public static final int CHARGER_SLOT_COUNT = ChargerBlockEntity.CONTAINER_SIZE;
    public static final int PLAYER_SLOT_COUNT = 36;
    public static final int TOTAL_SLOT_COUNT = CHARGER_SLOT_COUNT + PLAYER_SLOT_COUNT;

    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 84;
    private static final int PLAYER_HOTBAR_Y = 142;

    private final Container chargerInventory;

    public ChargerMenu(final int containerId, final Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(CHARGER_SLOT_COUNT));
    }

    public ChargerMenu(final int containerId, final Inventory playerInventory, final Container chargerInventory) {
        super(ModMenus.CHARGER.get(), containerId);
        checkContainerSize(chargerInventory, CHARGER_SLOT_COUNT);
        this.chargerInventory = chargerInventory;
        chargerInventory.startOpen(playerInventory.player);

        addSlot(new Slot(chargerInventory, ChargerBlockEntity.SLOT_CHARGEABLE, 80, 35) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return chargerInventory.canPlaceItem(ChargerBlockEntity.SLOT_CHARGEABLE, stack);
            }
        });
        addPlayerInventory(playerInventory);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        ItemStack moved = ItemStack.EMPTY;
        final Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            final boolean fromCharger = index < CHARGER_SLOT_COUNT;
            // Finish the owned environment before copying its stack into the player inventory.
            final ItemStack stack = fromCharger ? chargerInventory.removeItemNoUpdate(index) : slot.getItem();
            moved = stack.copy();
            if (fromCharger) {
                if (!moveItemStackTo(stack, CHARGER_SLOT_COUNT, TOTAL_SLOT_COUNT, true)) {
                    chargerInventory.setItem(index, stack);
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, CHARGER_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else if (fromCharger) {
                slot.setByPlayer(stack);
            } else {
                slot.setChanged();
            }
        }
        return moved;
    }

    @Override
    public boolean stillValid(final Player player) {
        return chargerInventory.stillValid(player);
    }

    @Override
    public void removed(final Player player) {
        super.removed(player);
        chargerInventory.stopOpen(player);
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
