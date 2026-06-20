package li.cil.oc.common.util;

import net.minecraft.world.item.ItemStack;

public final class InventoryComparison {
    private InventoryComparison() {
    }

    public static boolean sameItem(final ItemStack stackA, final ItemStack stackB, final boolean checkComponents) {
        if (stackA == stackB) {
            return true;
        }
        if (stackA.isEmpty() || stackB.isEmpty()) {
            return stackA.isEmpty() && stackB.isEmpty();
        }
        return checkComponents ? ItemStack.isSameItemSameComponents(stackA, stackB) : ItemStack.isSameItem(stackA, stackB);
    }
}
