package li.cil.oc.common.menu;

import li.cil.oc.common.ModMenus;
import li.cil.oc.common.blockentity.DisassemblerBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DisassemblerMenu extends AbstractContainerMenu {
    public static final int DISASSEMBLER_SLOT_COUNT = DisassemblerBlockEntity.CONTAINER_SIZE;
    public static final int PLAYER_SLOT_COUNT = 36;
    public static final int TOTAL_SLOT_COUNT = DISASSEMBLER_SLOT_COUNT + PLAYER_SLOT_COUNT;
    public static final int DISASSEMBLER_STATE_INDEX = 0;
    public static final int DISASSEMBLER_DATA_COUNT = 1;
    public static final int STATE_EMPTY = 0;
    public static final int STATE_READY = 1;
    public static final int STATE_BLOCKED = 2;

    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 84;
    private static final int PLAYER_HOTBAR_Y = 142;
    private static final int[][] DISASSEMBLER_SLOT_POSITIONS = {
        {80, 17},
        {8, 44}, {26, 44}, {44, 44}, {62, 44}, {80, 44}, {98, 44}, {116, 44}, {134, 44}, {152, 44}
    };

    private final Container disassemblerInventory;
    private final ContainerData disassemblerData;

    public DisassemblerMenu(final int containerId, final Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(DISASSEMBLER_SLOT_COUNT), new SimpleContainerData(DISASSEMBLER_DATA_COUNT));
    }

    public DisassemblerMenu(final int containerId, final Inventory playerInventory, final Container disassemblerInventory) {
        this(containerId, playerInventory, disassemblerInventory, disassemblerData(disassemblerInventory));
    }

    public DisassemblerMenu(final int containerId, final Inventory playerInventory, final Container disassemblerInventory, final ContainerData disassemblerData) {
        super(ModMenus.DISASSEMBLER.get(), containerId);
        checkContainerSize(disassemblerInventory, DISASSEMBLER_SLOT_COUNT);
        this.disassemblerInventory = disassemblerInventory;
        this.disassemblerData = disassemblerData;
        disassemblerInventory.startOpen(playerInventory.player);
        addDataSlots(disassemblerData);

        for (int slot = 0; slot < DISASSEMBLER_SLOT_COUNT; slot++) {
            final int disassemblerSlot = slot;
            final int[] position = DISASSEMBLER_SLOT_POSITIONS[slot];
            addSlot(new Slot(disassemblerInventory, disassemblerSlot, position[0], position[1]) {
                @Override
                public boolean mayPlace(final ItemStack stack) {
                    return disassemblerInventory.canPlaceItem(disassemblerSlot, stack);
                }
            });
        }
        addPlayerInventory(playerInventory);
    }

    public static int disassemblerSlotX(final int slot) {
        return DISASSEMBLER_SLOT_POSITIONS[slot][0];
    }

    public static int disassemblerSlotY(final int slot) {
        return DISASSEMBLER_SLOT_POSITIONS[slot][1];
    }

    public int disassemblyState() {
        return disassemblerData.get(DISASSEMBLER_STATE_INDEX);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        ItemStack moved = ItemStack.EMPTY;
        final Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            final ItemStack stack = slot.getItem();
            moved = stack.copy();
            if (index < DISASSEMBLER_SLOT_COUNT) {
                if (!moveItemStackTo(stack, DISASSEMBLER_SLOT_COUNT, TOTAL_SLOT_COUNT, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, DISASSEMBLER_SLOT_COUNT, false)) {
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
        return disassemblerInventory.stillValid(player);
    }

    @Override
    public void removed(final Player player) {
        super.removed(player);
        disassemblerInventory.stopOpen(player);
    }

    public static int stateFor(final Container disassemblerInventory) {
        if (!(disassemblerInventory instanceof DisassemblerBlockEntity disassembler) ||
            disassembler.getItem(DisassemblerBlockEntity.SLOT_INPUT).isEmpty()) {
            return STATE_EMPTY;
        }
        return disassembler.canDisassemble() ? STATE_READY : STATE_BLOCKED;
    }

    private static ContainerData disassemblerData(final Container disassemblerInventory) {
        return new ContainerData() {
            @Override
            public int get(final int index) {
                return index == DISASSEMBLER_STATE_INDEX ? stateFor(disassemblerInventory) : 0;
            }

            @Override
            public void set(final int index, final int value) {
            }

            @Override
            public int getCount() {
                return DISASSEMBLER_DATA_COUNT;
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
