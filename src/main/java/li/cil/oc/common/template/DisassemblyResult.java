package li.cil.oc.common.template;

import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

/** Paid recipe parts and unconditional returned contents have different processing rules. */
public record DisassemblyResult(ItemStack[] queued, ItemStack[] immediate) {
    public static final DisassemblyResult EMPTY = new DisassemblyResult(null, null);

    public DisassemblyResult {
        queued = copy(queued);
        immediate = copy(immediate);
    }

    public boolean isEmpty() { return queued.length == 0 && immediate.length == 0; }

    private static ItemStack[] copy(ItemStack[] items) {
        return items == null ? new ItemStack[0] : Arrays.stream(items).filter(item -> item != null && !item.isEmpty()).map(ItemStack::copy).toArray(ItemStack[]::new);
    }
}
