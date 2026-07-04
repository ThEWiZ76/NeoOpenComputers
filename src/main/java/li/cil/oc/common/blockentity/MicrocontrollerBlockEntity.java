package li.cil.oc.common.blockentity;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.block.MicrocontrollerBlock;
import li.cil.oc.common.menu.MicrocontrollerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.IMenuProviderExtension;

public class MicrocontrollerBlockEntity extends BlockEntity implements Container, MenuProvider, IMenuProviderExtension {
    private static final String SLOT_TYPE_EEPROM = "eeprom";
    private static final int TIER_ANY = Integer.MAX_VALUE;
    private static final MicrocontrollerSlot[][] SLOT_LAYOUTS = {
        {
            new MicrocontrollerSlot(Slot.CPU, 0),
            new MicrocontrollerSlot(Slot.Memory, 0),
            new MicrocontrollerSlot(SLOT_TYPE_EEPROM, TIER_ANY),
            new MicrocontrollerSlot(Slot.Card, 0),
            new MicrocontrollerSlot(Slot.Card, 0),
            new MicrocontrollerSlot(Slot.Upgrade, 1)
        },
        {
            new MicrocontrollerSlot(Slot.CPU, 0),
            new MicrocontrollerSlot(Slot.Memory, 0),
            new MicrocontrollerSlot(Slot.Memory, 0),
            new MicrocontrollerSlot(SLOT_TYPE_EEPROM, TIER_ANY),
            new MicrocontrollerSlot(Slot.Card, 1),
            new MicrocontrollerSlot(Slot.Card, 0),
            new MicrocontrollerSlot(Slot.Upgrade, 2)
        },
        {
            new MicrocontrollerSlot(Slot.CPU, 2),
            new MicrocontrollerSlot(Slot.Memory, 2),
            new MicrocontrollerSlot(Slot.Memory, 2),
            new MicrocontrollerSlot(SLOT_TYPE_EEPROM, TIER_ANY),
            new MicrocontrollerSlot(Slot.Card, 2),
            new MicrocontrollerSlot(Slot.Card, 2),
            new MicrocontrollerSlot(Slot.Card, 2),
            new MicrocontrollerSlot(Slot.Upgrade, 2),
            new MicrocontrollerSlot(Slot.Upgrade, 2),
            new MicrocontrollerSlot(Slot.Upgrade, 2),
            new MicrocontrollerSlot(Slot.Upgrade, 2),
            new MicrocontrollerSlot(Slot.Upgrade, 2),
            new MicrocontrollerSlot(Slot.Upgrade, 2),
            new MicrocontrollerSlot(Slot.Upgrade, 2),
            new MicrocontrollerSlot(Slot.Upgrade, 2),
            new MicrocontrollerSlot(Slot.Upgrade, 2)
        }
    };

    private final NonNullList<ItemStack> items;
    private final int tier;

    public MicrocontrollerBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.MICROCONTROLLER.get(), pos, blockState);
        tier = tierFromBlockState(blockState);
        items = NonNullList.withSize(slotCount(tier), ItemStack.EMPTY);
    }

    public static int slotCount(final int tier) {
        return slotLayout(tier).length;
    }

    public static String slotType(final int tier, final int slot) {
        final MicrocontrollerSlot[] layout = slotLayout(tier);
        return slot >= 0 && slot < layout.length ? layout[slot].type() : Slot.None;
    }

    public static int slotTier(final int tier, final int slot) {
        final MicrocontrollerSlot[] layout = slotLayout(tier);
        return slot >= 0 && slot < layout.length ? layout[slot].tier() : -1;
    }

    public int tier() {
        return tier;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.neoopencomputers.microcontroller.title");
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory playerInventory, final Player player) {
        return new MicrocontrollerMenu(containerId, playerInventory, this);
    }

    @Override
    public void writeClientSideData(final AbstractContainerMenu menu, final RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(tier);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (final ItemStack stack : items) {
            if (!stack.isEmpty()) {
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
        return isValidSlot(slot) ? ContainerHelper.takeItem(items, slot) : ItemStack.EMPTY;
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
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        if (!isValidSlot(slot) || stack.isEmpty()) {
            return false;
        }
        final DriverItem driver = Driver.driverFor(stack);
        return driver != null
            && slotType(tier, slot).equals(driver.slot(stack))
            && driver.tier(stack) <= slotTier(tier, slot);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    private boolean isValidSlot(final int slot) {
        return slot >= 0 && slot < items.size();
    }

    private static MicrocontrollerSlot[] slotLayout(final int tier) {
        if (tier <= 0) {
            return SLOT_LAYOUTS[0];
        }
        if (tier == 1) {
            return SLOT_LAYOUTS[1];
        }
        return SLOT_LAYOUTS[2];
    }

    private static int tierFromBlockState(final BlockState blockState) {
        if (blockState != null && blockState.getBlock() instanceof MicrocontrollerBlock microcontrollerBlock) {
            return microcontrollerBlock.tier();
        }
        return 0;
    }

    private record MicrocontrollerSlot(String type, int tier) {
    }
}
