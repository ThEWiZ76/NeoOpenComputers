package li.cil.oc.common.menu;

import li.cil.oc.common.ModMenus;
import li.cil.oc.common.blockentity.PrinterBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PrinterMenu extends AbstractContainerMenu {
    public static final int PRINTER_SLOT_COUNT = PrinterBlockEntity.CONTAINER_SIZE;
    public static final int PLAYER_SLOT_COUNT = 36;
    public static final int TOTAL_SLOT_COUNT = PRINTER_SLOT_COUNT + PLAYER_SLOT_COUNT;
    public static final int PRINTER_PROGRESS_INDEX = 0;
    public static final int PRINTER_MATERIAL_INDEX = 1;
    public static final int PRINTER_INK_INDEX = 2;
    public static final int PRINTER_DATA_COUNT = 3;

    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 84;
    private static final int PLAYER_HOTBAR_Y = 142;
    private static final int[][] PRINTER_SLOT_POSITIONS = {
        {18, 19},
        {18, 51},
        {152, 35}
    };

    private final Container printerInventory;
    private final ContainerData printerData;

    public PrinterMenu(final int containerId, final Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(PRINTER_SLOT_COUNT), new SimpleContainerData(PRINTER_DATA_COUNT));
    }

    public PrinterMenu(final int containerId, final Inventory playerInventory, final Container printerInventory) {
        this(containerId, playerInventory, printerInventory, printerData(printerInventory));
    }

    public PrinterMenu(final int containerId, final Inventory playerInventory, final Container printerInventory, final ContainerData printerData) {
        super(ModMenus.PRINTER.get(), containerId);
        checkContainerSize(printerInventory, PRINTER_SLOT_COUNT);
        this.printerInventory = printerInventory;
        this.printerData = printerData;
        printerInventory.startOpen(playerInventory.player);
        addDataSlots(printerData);

        for (int slot = 0; slot < PRINTER_SLOT_COUNT; slot++) {
            final int printerSlot = slot;
            addSlot(new Slot(printerInventory, printerSlot, printerSlotX(slot), printerSlotY(slot)) {
                @Override
                public boolean mayPlace(final ItemStack stack) {
                    return printerInventory.canPlaceItem(printerSlot, stack);
                }
            });
        }
        addPlayerInventory(playerInventory);
    }

    public static int printerSlotX(final int slot) {
        return PRINTER_SLOT_POSITIONS[slot][0];
    }

    public static int printerSlotY(final int slot) {
        return PRINTER_SLOT_POSITIONS[slot][1];
    }

    public int progress() {
        return printerData.get(PRINTER_PROGRESS_INDEX);
    }

    public int amountMaterial() {
        return printerData.get(PRINTER_MATERIAL_INDEX);
    }

    public int amountInk() {
        return printerData.get(PRINTER_INK_INDEX);
    }

    public int materialCapacity() {
        return PrinterBlockEntity.MAX_MATERIAL;
    }

    public int inkCapacity() {
        return PrinterBlockEntity.MAX_INK;
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        ItemStack moved = ItemStack.EMPTY;
        final Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            final ItemStack stack = slot.getItem();
            moved = stack.copy();
            if (index < PRINTER_SLOT_COUNT) {
                if (!moveItemStackTo(stack, PRINTER_SLOT_COUNT, TOTAL_SLOT_COUNT, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, PRINTER_SLOT_COUNT, false)) {
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
        return printerInventory.stillValid(player);
    }

    @Override
    public void removed(final Player player) {
        super.removed(player);
        printerInventory.stopOpen(player);
    }

    private static ContainerData printerData(final Container printerInventory) {
        return new ContainerData() {
            @Override
            public int get(final int index) {
                if (!(printerInventory instanceof PrinterBlockEntity printer)) {
                    return 0;
                }
                return switch (index) {
                    case PRINTER_PROGRESS_INDEX -> printer.isPrinting() ? Math.clamp((int) Math.round(printer.progress()), 0, 100) : 0;
                    case PRINTER_MATERIAL_INDEX -> printer.amountMaterial();
                    case PRINTER_INK_INDEX -> printer.amountInk();
                    default -> 0;
                };
            }

            @Override
            public void set(final int index, final int value) {
            }

            @Override
            public int getCount() {
                return PRINTER_DATA_COUNT;
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
