package li.cil.oc.common.template;

import li.cil.oc.common.item.NavigationUpgradeItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;

final class NavigationUpgradeDisassemblerTemplate implements DisassemblerTemplate {
    @Override public String name() { return "navigation_upgrade"; }
    @Override public boolean matches(ItemStack stack) { return stack.getItem() instanceof NavigationUpgradeItem; }
    @Override public ItemStack[] disassemble(ItemStack stack) {
        final var map = NavigationUpgradeItem.originalMap(stack, null);
        return map.isEmpty() ? new ItemStack[0] : new ItemStack[]{map};
    }

    @Override public DisassemblyResult plan(ItemStack stack, ItemStack[] ingredients) { return plan(stack, ingredients, null); }

    @Override public DisassemblyResult plan(ItemStack stack, ItemStack[] ingredients, HolderLookup.Provider registries) {
        final var parts = new ArrayList<ItemStack>();
        for (var ingredient : ingredients) {
            if (ingredient.is(Items.FILLED_MAP)) {
                final var map = NavigationUpgradeItem.originalMap(stack, registries);
                if (map.isEmpty()) return DisassemblyResult.EMPTY;
                parts.add(map);
            } else parts.add(ingredient);
        }
        return new DisassemblyResult(parts.toArray(ItemStack[]::new), null);
    }
}
