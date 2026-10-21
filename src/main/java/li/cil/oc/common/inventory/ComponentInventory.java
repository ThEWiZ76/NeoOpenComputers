package li.cil.oc.common.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Active components must write back before vanilla menu transfers copy/shrink their stacks. */
public interface ComponentInventory extends Container {
    void saveItemData(int slot);

    static void saveBeforeTransfer(Slot slot) {
        if (slot.container instanceof ComponentInventory inventory) inventory.saveItemData(slot.getContainerSlot());
    }

    /** Teardown may change item data; the actual move must compare the finalized components. */
    static boolean hasPotentialCapacity(ItemStack stack, List<Slot> slots, int start, int end) {
        for (int index = start; index < end; index++) {
            final Slot destination = slots.get(index);
            final ItemStack existing = destination.getItem();
            if (existing.isEmpty() || ItemStack.isSameItem(existing, stack)
                && existing.getCount() < Math.min(destination.getMaxStackSize(), existing.getMaxStackSize())) return true;
        }
        return false;
    }
}
