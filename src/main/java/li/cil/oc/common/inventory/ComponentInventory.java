package li.cil.oc.common.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

/** Active components must write back before vanilla menu transfers copy/shrink their stacks. */
public interface ComponentInventory extends Container {
    void saveItemData(int slot);

    static void saveBeforeTransfer(Slot slot) {
        if (slot.container instanceof ComponentInventory inventory) inventory.saveItemData(slot.getContainerSlot());
    }
}
