package li.cil.oc.common.blockentity;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.menu.DisassemblerMenu;
import li.cil.oc.common.template.DisassemblerTemplates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public class DisassemblerBlockEntity extends BlockEntity implements Container, MenuProvider, DeviceInfo {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT_START = 1;
    public static final int OUTPUT_SLOT_COUNT = 9;
    public static final int CONTAINER_SIZE = 10;
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
        DeviceInfo.DeviceAttribute.Description, "Disassembler",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
        DeviceInfo.DeviceAttribute.Product, "Break.3R-100"
    );

    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);

    public DisassemblerBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.DISASSEMBLER.get(), pos, blockState);
    }

    public boolean canInsert(final ItemStack stack) {
        return !stack.isEmpty() && items.get(SLOT_INPUT).isEmpty() && DisassemblerTemplates.select(stack).isPresent();
    }

    public boolean canDisassemble() {
        return outputsFit(DisassemblerTemplates.disassemble(items.get(SLOT_INPUT)));
    }

    public boolean disassemble() {
        return disassemble(level == null ? RandomSource.create() : level.random, defaultBreakChance());
    }

    public boolean disassemble(final RandomSource random, final double breakChance) {
        final ItemStack input = items.get(SLOT_INPUT);
        final ItemStack[] outputs = survivingOutputs(DisassemblerTemplates.disassemble(input), random, breakChance);
        if (!outputsFit(outputs) && outputs.length > 0 && !hasAdjacentInventory()) {
            return false;
        }
        items.set(SLOT_INPUT, ItemStack.EMPTY);
        routeOutputs(outputs);
        setChanged();
        return true;
    }

    public boolean containsOutput(final Item item) {
        for (int slot = SLOT_OUTPUT_START; slot < CONTAINER_SIZE; slot++) {
            if (items.get(slot).is(item)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
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
        final ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
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
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        return slot == SLOT_INPUT && canInsert(stack);
    }

    @Override
    public boolean stillValid(final Player player) {
        return !isRemoved();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.neoopencomputers.disassembler");
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory playerInventory, final Player player) {
        return new DisassemblerMenu(containerId, playerInventory, this);
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return DEVICE_INFO;
    }

    public static double defaultBreakChance() {
        return ModSettings.disassemblerBreakChance();
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    private static ItemStack[] survivingOutputs(final ItemStack[] outputs, final RandomSource random, final double breakChance) {
        if (outputs == null || outputs.length == 0) {
            return new ItemStack[0];
        }
        final java.util.ArrayList<ItemStack> surviving = new java.util.ArrayList<>();
        final double chance = Math.max(0D, Math.min(1D, breakChance));
        for (ItemStack output : outputs) {
            if (output == null || output.isEmpty()) {
                continue;
            }
            final ItemStack stack = output.copy();
            int survivors = 0;
            for (int count = 0; count < stack.getCount(); count++) {
                if (random.nextDouble() >= chance) {
                    survivors++;
                }
            }
            if (survivors > 0) {
                stack.setCount(survivors);
                surviving.add(stack);
            }
        }
        return surviving.toArray(ItemStack[]::new);
    }

    private void routeOutputs(final ItemStack[] outputs) {
        for (ItemStack output : outputs) {
            ItemStack remaining = output.copy();
            remaining = insertIntoAdjacentInventory(remaining);
            if (!remaining.isEmpty()) {
                remaining = insertIntoInternalOutput(remaining);
            }
            if (!remaining.isEmpty() && level != null) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D, remaining);
            }
        }
    }

    private boolean hasAdjacentInventory() {
        if (level == null) {
            return false;
        }
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(direction)) instanceof Container) {
                return true;
            }
        }
        return false;
    }

    private ItemStack insertIntoAdjacentInventory(final ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return stack;
        }
        ItemStack remaining = stack;
        for (Direction direction : Direction.values()) {
            final BlockEntity blockEntity = level.getBlockEntity(worldPosition.relative(direction));
            if (blockEntity instanceof Container container) {
                remaining = insertInto(container, remaining);
                if (remaining.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }
        return remaining;
    }

    private ItemStack insertIntoInternalOutput(final ItemStack stack) {
        ItemStack remaining = stack;
        for (int slot = SLOT_OUTPUT_START; slot < CONTAINER_SIZE; slot++) {
            if (items.get(slot).isEmpty()) {
                items.set(slot, remaining.copy());
                return ItemStack.EMPTY;
            }
        }
        return remaining;
    }

    private static ItemStack insertInto(final Container container, final ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (remaining.isEmpty()) {
                return ItemStack.EMPTY;
            }
            final ItemStack existing = container.getItem(slot);
            if (existing.isEmpty()) {
                final ItemStack inserted = remaining.copy();
                inserted.setCount(Math.min(remaining.getCount(), Math.min(inserted.getMaxStackSize(), container.getMaxStackSize())));
                if (container.canPlaceItem(slot, inserted)) {
                    container.setItem(slot, inserted);
                    remaining.shrink(inserted.getCount());
                }
            } else if (ItemStack.isSameItemSameComponents(existing, remaining) && existing.getCount() < Math.min(existing.getMaxStackSize(), container.getMaxStackSize())) {
                final int transferable = Math.min(remaining.getCount(), Math.min(existing.getMaxStackSize(), container.getMaxStackSize()) - existing.getCount());
                existing.grow(transferable);
                container.setItem(slot, existing);
                remaining.shrink(transferable);
            }
        }
        return remaining;
    }

    private boolean outputsFit(final ItemStack[] outputs) {
        if (outputs == null || outputs.length == 0 || outputs.length > OUTPUT_SLOT_COUNT) {
            return false;
        }
        for (int slot = SLOT_OUTPUT_START; slot < CONTAINER_SIZE; slot++) {
            if (!items.get(slot).isEmpty()) {
                return false;
            }
        }
        for (ItemStack output : outputs) {
            if (output == null || output.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidSlot(final int slot) {
        return slot >= 0 && slot < CONTAINER_SIZE;
    }
}
