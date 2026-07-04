package li.cil.oc.common.blockentity;

import li.cil.oc.api.internal.Tiered;
import li.cil.oc.common.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class RobotBlockEntity extends BlockEntity implements Container, Tiered {
    public static final String TAG_TIER = "oc:tier";
    private static final int TIER_ANY = Integer.MAX_VALUE;
    private static final String SLOT_TYPE_EEPROM = "eeprom";
    private static final RobotSlot[][] CONTAINER_LAYOUTS = {
        {new RobotSlot(li.cil.oc.api.driver.item.Slot.Container, 1), new RobotSlot(li.cil.oc.api.driver.item.Slot.Container, 0), new RobotSlot(li.cil.oc.api.driver.item.Slot.Container, 0)},
        {new RobotSlot(li.cil.oc.api.driver.item.Slot.Container, 2), new RobotSlot(li.cil.oc.api.driver.item.Slot.Container, 1), new RobotSlot(li.cil.oc.api.driver.item.Slot.Container, 0)},
        {new RobotSlot(li.cil.oc.api.driver.item.Slot.Container, 2), new RobotSlot(li.cil.oc.api.driver.item.Slot.Container, 1), new RobotSlot(li.cil.oc.api.driver.item.Slot.Container, 1)}
    };
    private static final RobotSlot[][] UPGRADE_LAYOUTS = {
        {
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Upgrade, 0),
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Upgrade, 0),
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Upgrade, 0)
        },
        {
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Upgrade, 1),
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Upgrade, 1),
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Upgrade, 1),
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Upgrade, 0),
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Upgrade, 0),
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Upgrade, 0)
        },
        {
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Upgrade, 2),
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Upgrade, 2),
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Upgrade, 2),
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Upgrade, 1),
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Upgrade, 1),
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Upgrade, 1),
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Upgrade, 0),
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Upgrade, 0),
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Upgrade, 0)
        }
    };
    private static final RobotSlot[][] COMPONENT_LAYOUTS = {
        {
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Card, 0),
            RobotSlot.NONE,
            RobotSlot.NONE,
            new RobotSlot(li.cil.oc.api.driver.item.Slot.CPU, 0),
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Memory, 0),
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Memory, 0),
            new RobotSlot(SLOT_TYPE_EEPROM, TIER_ANY),
            new RobotSlot(li.cil.oc.api.driver.item.Slot.HDD, 0)
        },
        {
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Card, 1),
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Card, 0),
            RobotSlot.NONE,
            new RobotSlot(li.cil.oc.api.driver.item.Slot.CPU, 1),
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Memory, 1),
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Memory, 1),
            new RobotSlot(SLOT_TYPE_EEPROM, TIER_ANY),
            new RobotSlot(li.cil.oc.api.driver.item.Slot.HDD, 1)
        },
        {
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Card, 2),
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Card, 1),
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Card, 1),
            new RobotSlot(li.cil.oc.api.driver.item.Slot.CPU, 2),
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Memory, 2),
            new RobotSlot(li.cil.oc.api.driver.item.Slot.Memory, 2),
            new RobotSlot(SLOT_TYPE_EEPROM, TIER_ANY),
            new RobotSlot(li.cil.oc.api.driver.item.Slot.HDD, 2),
            new RobotSlot(li.cil.oc.api.driver.item.Slot.HDD, 1)
        }
    };
    private static final int MAX_SLOT_COUNT = slotCount(2);

    private final NonNullList<ItemStack> items = NonNullList.withSize(MAX_SLOT_COUNT, ItemStack.EMPTY);
    private int tier;

    public RobotBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.ROBOT.get(), pos, blockState);
    }

    @Override
    public int tier() {
        return tier;
    }

    public void setTier(final int tier) {
        this.tier = normalizeTier(tier);
        setChanged();
    }

    public static int slotCount(final int tier) {
        return containerSlotCount(tier) + upgradeSlotCount(tier) + componentSlotCount(tier);
    }

    public static int containerSlotCount(final int tier) {
        return CONTAINER_LAYOUTS[normalizeTier(tier)].length;
    }

    public static int upgradeSlotCount(final int tier) {
        return UPGRADE_LAYOUTS[normalizeTier(tier)].length;
    }

    public static int componentSlotCount(final int tier) {
        return COMPONENT_LAYOUTS[normalizeTier(tier)].length;
    }

    public static String slotType(final int tier, final int slot) {
        return slotAt(tier, slot).type();
    }

    public static int slotTier(final int tier, final int slot) {
        return slotAt(tier, slot).tier();
    }

    @Override
    public int getContainerSize() {
        return slotCount(tier);
    }

    @Override
    public boolean isEmpty() {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            if (!items.get(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(final int slot) {
        return isValidSlot(slot) ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(final int slot, final int amount) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        final ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        if (!isValidSlot(slot)) {
            return;
        }
        items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(final Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < items.size(); slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tier = normalizeTier(tag.getInt(TAG_TIER));
        ContainerHelper.loadAllItems(tag, items, registries);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(TAG_TIER, tier);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    private boolean isValidSlot(final int slot) {
        return slot >= 0 && slot < getContainerSize();
    }

    private static RobotSlot slotAt(final int tier, final int slot) {
        if (slot < 0) {
            return RobotSlot.NONE;
        }
        final RobotSlot[] containers = CONTAINER_LAYOUTS[normalizeTier(tier)];
        if (slot < containers.length) {
            return containers[slot];
        }
        int relative = slot - containers.length;
        final RobotSlot[] upgrades = UPGRADE_LAYOUTS[normalizeTier(tier)];
        if (relative < upgrades.length) {
            return upgrades[relative];
        }
        relative -= upgrades.length;
        final RobotSlot[] components = COMPONENT_LAYOUTS[normalizeTier(tier)];
        if (relative < components.length) {
            return components[relative];
        }
        return RobotSlot.NONE;
    }

    private static int normalizeTier(final int tier) {
        return Math.max(0, Math.min(2, tier));
    }

    private record RobotSlot(String type, int tier) {
        private static final RobotSlot NONE = new RobotSlot(li.cil.oc.api.driver.item.Slot.None, -1);
    }
}
