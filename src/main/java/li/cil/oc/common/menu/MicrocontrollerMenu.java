package li.cil.oc.common.menu;

import li.cil.oc.common.ModMenus;
import li.cil.oc.common.blockentity.MicrocontrollerBlockEntity;
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

public class MicrocontrollerMenu extends AbstractContainerMenu {
    public static final int MIN_MICROCONTROLLER_SLOT_COUNT = 6;
    public static final int MAX_MICROCONTROLLER_SLOT_COUNT = 16;
    public static final int PLAYER_SLOT_COUNT = 36;
    public static final int MICROCONTROLLER_TIER_INDEX = 0;
    public static final int MICROCONTROLLER_DATA_COUNT = 1;

    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 84;
    private static final int PLAYER_HOTBAR_Y = 142;
    private static final int[][][] MICROCONTROLLER_SLOT_POSITIONS = {
        {
            {48, 16},
            {70, 16},
            {48, 34},
            {98, 16},
            {98, 34},
            {120, 16}
        },
        {
            {48, 16},
            {70, 16},
            {70, 34},
            {48, 34},
            {98, 16},
            {98, 34},
            {120, 16}
        },
        {
            {48, 16},
            {70, 16},
            {70, 34},
            {48, 34},
            {98, 16},
            {98, 34},
            {98, 52},
            {120, 16},
            {138, 16},
            {156, 16},
            {120, 34},
            {138, 34},
            {156, 34},
            {120, 52},
            {138, 52},
            {156, 52}
        }
    };

    private final Container microcontrollerInventory;
    private final ContainerData microcontrollerData;
    private final int microcontrollerSlotCount;

    public MicrocontrollerMenu(final int containerId, final Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(MAX_MICROCONTROLLER_SLOT_COUNT), new SimpleContainerData(MICROCONTROLLER_DATA_COUNT));
    }

    public MicrocontrollerMenu(final int containerId, final Inventory playerInventory, final RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, new SimpleContainer(MAX_MICROCONTROLLER_SLOT_COUNT), clientMicrocontrollerData(extraData));
    }

    public MicrocontrollerMenu(final int containerId, final Inventory playerInventory, final MicrocontrollerBlockEntity microcontroller) {
        this(containerId, playerInventory, microcontroller, microcontrollerData(microcontroller));
    }

    public MicrocontrollerMenu(final int containerId, final Inventory playerInventory, final Container microcontrollerInventory, final ContainerData microcontrollerData) {
        super(ModMenus.MICROCONTROLLER.get(), containerId);
        checkContainerSize(microcontrollerInventory, MIN_MICROCONTROLLER_SLOT_COUNT);
        this.microcontrollerInventory = microcontrollerInventory;
        this.microcontrollerData = microcontrollerData;
        microcontrollerSlotCount = microcontrollerSlotCountForTier(microcontrollerTier());
        microcontrollerInventory.startOpen(playerInventory.player);
        addDataSlots(microcontrollerData);

        for (int slot = 0; slot < microcontrollerSlotCount; slot++) {
            final int[] position = slotPosition(microcontrollerTier(), slot);
            addSlot(new MicrocontrollerSlot(microcontrollerInventory, slot, position[0], position[1]));
        }
        addPlayerInventory(playerInventory);
    }

    public static int microcontrollerSlotX(final int tier, final int slot) {
        return slotPosition(tier, slot)[0];
    }

    public static int microcontrollerSlotY(final int tier, final int slot) {
        return slotPosition(tier, slot)[1];
    }

    public static int microcontrollerSlotCountForTier(final int tier) {
        return slotPositions(tier).length;
    }

    public static String microcontrollerSlotKind(final int tier, final int slot) {
        return MicrocontrollerBlockEntity.slotType(tier, slot);
    }

    public static int microcontrollerSlotTierLimit(final int tier, final int slot) {
        return MicrocontrollerBlockEntity.slotTier(tier, slot);
    }

    public int microcontrollerTier() {
        return microcontrollerData.get(MICROCONTROLLER_TIER_INDEX);
    }

    public Container microcontrollerInventory() {
        return microcontrollerInventory;
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        ItemStack moved = ItemStack.EMPTY;
        final Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            final ItemStack stack = slot.getItem();
            moved = stack.copy();
            if (index < microcontrollerSlotCount) {
                if (!moveItemStackTo(stack, microcontrollerSlotCount, microcontrollerSlotCount + PLAYER_SLOT_COUNT, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, microcontrollerSlotCount, false)) {
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
        return microcontrollerInventory.stillValid(player);
    }

    @Override
    public void removed(final Player player) {
        super.removed(player);
        microcontrollerInventory.stopOpen(player);
    }

    private static ContainerData microcontrollerData(final Container microcontrollerInventory) {
        return new ServerMicrocontrollerData(microcontrollerInventory);
    }

    private static ContainerData clientMicrocontrollerData(final RegistryFriendlyByteBuf extraData) {
        final SimpleContainerData data = new SimpleContainerData(MICROCONTROLLER_DATA_COUNT);
        if (extraData != null) {
            data.set(MICROCONTROLLER_TIER_INDEX, extraData.readVarInt());
        }
        return data;
    }

    private static int[] slotPosition(final int tier, final int slot) {
        final int[][] positions = slotPositions(tier);
        return slot >= 0 && slot < positions.length ? positions[slot] : new int[]{-1, -1};
    }

    private static int[][] slotPositions(final int tier) {
        if (tier <= 0) {
            return MICROCONTROLLER_SLOT_POSITIONS[0];
        }
        if (tier == 1) {
            return MICROCONTROLLER_SLOT_POSITIONS[1];
        }
        return MICROCONTROLLER_SLOT_POSITIONS[2];
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

    static final class MicrocontrollerSlot extends Slot {
        MicrocontrollerSlot(final Container container, final int slot, final int x, final int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(final ItemStack stack) {
            return container.canPlaceItem(getSlotIndex(), stack);
        }
    }

    static final class ServerMicrocontrollerData implements ContainerData {
        private final Container microcontrollerInventory;

        ServerMicrocontrollerData(final Container microcontrollerInventory) {
            this.microcontrollerInventory = microcontrollerInventory;
        }

        @Override
        public int get(final int index) {
            return switch (index) {
                case MICROCONTROLLER_TIER_INDEX -> microcontrollerInventory instanceof MicrocontrollerBlockEntity microcontroller ? microcontroller.tier() : 0;
                default -> 0;
            };
        }

        @Override
        public void set(final int index, final int value) {
        }

        @Override
        public int getCount() {
            return MICROCONTROLLER_DATA_COUNT;
        }
    }
}
