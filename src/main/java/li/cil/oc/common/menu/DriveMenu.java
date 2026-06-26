package li.cil.oc.common.menu;

import li.cil.oc.common.ModMenus;
import li.cil.oc.common.item.HardDiskDriveItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class DriveMenu extends AbstractContainerMenu {
    public static final int BUTTON_MANAGED = 0;
    public static final int BUTTON_UNMANAGED = 1;
    public static final int BUTTON_LOCK = 2;

    public static final int DATA_UNMANAGED = 0;
    public static final int DATA_LOCKED = 1;
    public static final int DATA_COUNT = 2;

    private final ItemStack driveStack;
    private final ContainerData driveData;

    public DriveMenu(final int containerId, final Inventory playerInventory) {
        this(containerId, playerInventory, ItemStack.EMPTY, new SimpleContainerData(DATA_COUNT));
    }

    public DriveMenu(final int containerId, final Inventory playerInventory, final ItemStack driveStack) {
        this(containerId, playerInventory, driveStack, driveData(driveStack));
    }

    public DriveMenu(final int containerId, final Inventory playerInventory, final ItemStack driveStack, final ContainerData driveData) {
        super(ModMenus.DRIVE.get(), containerId);
        checkContainerDataCount(driveData, DATA_COUNT);
        this.driveStack = driveStack == null ? ItemStack.EMPTY : driveStack;
        this.driveData = driveData;
        addDataSlots(driveData);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(final Player player) {
        if (driveStack.isEmpty() || !(driveStack.getItem() instanceof HardDiskDriveItem)) {
            return false;
        }
        if (player == null) {
            return true;
        }
        return player.getMainHandItem() == driveStack || player.getOffhandItem() == driveStack;
    }

    @Override
    public boolean clickMenuButton(final Player player, final int id) {
        if (!stillValid(player)) {
            return false;
        }
        return switch (id) {
            case BUTTON_MANAGED -> {
                HardDiskDriveItem.setUnmanaged(driveStack, false);
                yield true;
            }
            case BUTTON_UNMANAGED -> {
                HardDiskDriveItem.setUnmanaged(driveStack, true);
                yield true;
            }
            case BUTTON_LOCK -> {
                HardDiskDriveItem.lock(driveStack, player);
                yield true;
            }
            default -> false;
        };
    }

    public boolean isUnmanaged() {
        return driveData.get(DATA_UNMANAGED) != 0;
    }

    public boolean isLocked() {
        return driveData.get(DATA_LOCKED) != 0;
    }

    public static ContainerData driveData(final ItemStack driveStack) {
        return new ContainerData() {
            @Override
            public int get(final int index) {
                return switch (index) {
                    case DATA_UNMANAGED -> HardDiskDriveItem.isUnmanaged(driveStack) ? 1 : 0;
                    case DATA_LOCKED -> HardDiskDriveItem.isLocked(driveStack) ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(final int index, final int value) {
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }
}
