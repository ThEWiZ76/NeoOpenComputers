package li.cil.oc.common.menu;

import li.cil.oc.common.ModMenus;
import li.cil.oc.common.blockentity.AssemblerBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class AssemblerMenu extends AbstractContainerMenu {
    public static final int ASSEMBLER_SLOT_COUNT = AssemblerBlockEntity.CONTAINER_SIZE;
    public static final int PLAYER_SLOT_COUNT = 36;
    public static final int TOTAL_SLOT_COUNT = ASSEMBLER_SLOT_COUNT + PLAYER_SLOT_COUNT;
    public static final int ASSEMBLER_STATE_INDEX = 0;
    public static final int ASSEMBLER_PROGRESS_INDEX = 1;
    public static final int ASSEMBLER_DATA_COUNT = 2;
    public static final int STATE_IDLE = 0;
    public static final int STATE_READY = 1;
    public static final int STATE_BUSY = 2;

    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 122;
    private static final int PLAYER_HOTBAR_Y = 180;
    private static final int[][] ASSEMBLER_SLOT_POSITIONS = {
        {80, 17},
        {8, 44}, {26, 44}, {44, 44},
        {62, 44}, {80, 44}, {98, 44}, {116, 44}, {134, 44}, {152, 44},
        {8, 62}, {26, 62}, {44, 62},
        {62, 62}, {80, 62}, {98, 62}, {116, 62}, {134, 62}, {152, 62},
        {62, 80}, {80, 80}, {98, 80}
    };

    private final Container assemblerInventory;
    private final ContainerData assemblerData;

    public AssemblerMenu(final int containerId, final Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(ASSEMBLER_SLOT_COUNT), new SimpleContainerData(ASSEMBLER_DATA_COUNT));
    }

    public AssemblerMenu(final int containerId, final Inventory playerInventory, final Container assemblerInventory) {
        this(containerId, playerInventory, assemblerInventory, assemblerData(assemblerInventory));
    }

    public AssemblerMenu(final int containerId, final Inventory playerInventory, final Container assemblerInventory, final ContainerData assemblerData) {
        super(ModMenus.ASSEMBLER.get(), containerId);
        checkContainerSize(assemblerInventory, ASSEMBLER_SLOT_COUNT);
        this.assemblerInventory = assemblerInventory;
        this.assemblerData = assemblerData;
        assemblerInventory.startOpen(playerInventory.player);
        addDataSlots(assemblerData);

        for (int slot = 0; slot < ASSEMBLER_SLOT_COUNT; slot++) {
            final int assemblerSlot = slot;
            final int[] position = ASSEMBLER_SLOT_POSITIONS[slot];
            addSlot(new Slot(assemblerInventory, assemblerSlot, position[0], position[1]) {
                @Override
                public boolean mayPlace(final ItemStack stack) {
                    return assemblerInventory.canPlaceItem(assemblerSlot, stack);
                }
            });
        }
        addPlayerInventory(playerInventory);
    }

    public static int assemblerSlotX(final int slot) {
        return ASSEMBLER_SLOT_POSITIONS[slot][0];
    }

    public static int assemblerSlotY(final int slot) {
        return ASSEMBLER_SLOT_POSITIONS[slot][1];
    }

    public int assemblyState() {
        return assemblerData.get(ASSEMBLER_STATE_INDEX);
    }

    public int assemblyProgress() {
        return assemblerData.get(ASSEMBLER_PROGRESS_INDEX);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        ItemStack moved = ItemStack.EMPTY;
        final Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            final ItemStack stack = slot.getItem();
            moved = stack.copy();
            if (index < ASSEMBLER_SLOT_COUNT) {
                if (!moveItemStackTo(stack, ASSEMBLER_SLOT_COUNT, TOTAL_SLOT_COUNT, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, ASSEMBLER_SLOT_COUNT, false)) {
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
        return assemblerInventory.stillValid(player);
    }

    @Override
    public void removed(final Player player) {
        super.removed(player);
        assemblerInventory.stopOpen(player);
    }

    public static int stateFor(final Container assemblerInventory) {
        if (!(assemblerInventory instanceof AssemblerBlockEntity assembler)) {
            return STATE_IDLE;
        }
        if (assembler.isAssembling()) {
            return STATE_BUSY;
        }
        return assembler.canAssemble() ? STATE_READY : STATE_IDLE;
    }

    public static int progressFor(final Container assemblerInventory) {
        if (!(assemblerInventory instanceof AssemblerBlockEntity assembler) || !assembler.isAssembling()) {
            return 0;
        }
        return Math.clamp((int) Math.round(assembler.progress()), 0, 100);
    }

    private static ContainerData assemblerData(final Container assemblerInventory) {
        return new ContainerData() {
            @Override
            public int get(final int index) {
                return switch (index) {
                    case ASSEMBLER_STATE_INDEX -> stateFor(assemblerInventory);
                    case ASSEMBLER_PROGRESS_INDEX -> progressFor(assemblerInventory);
                    default -> 0;
                };
            }

            @Override
            public void set(final int index, final int value) {
            }

            @Override
            public int getCount() {
                return ASSEMBLER_DATA_COUNT;
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
