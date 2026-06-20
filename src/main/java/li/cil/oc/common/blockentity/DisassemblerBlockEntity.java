package li.cil.oc.common.blockentity;

import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.template.DisassemblerTemplates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DisassemblerBlockEntity extends BlockEntity implements Container {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT_START = 1;
    public static final int OUTPUT_SLOT_COUNT = 9;
    public static final int CONTAINER_SIZE = 10;

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
        final ItemStack input = items.get(SLOT_INPUT);
        final ItemStack[] outputs = DisassemblerTemplates.disassemble(input);
        if (!outputsFit(outputs)) {
            return false;
        }
        items.set(SLOT_INPUT, ItemStack.EMPTY);
        for (int index = 0; index < outputs.length && index < OUTPUT_SLOT_COUNT; index++) {
            items.set(SLOT_OUTPUT_START + index, outputs[index].copy());
        }
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
    public boolean stillValid(final Player player) {
        return !isRemoved();
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
