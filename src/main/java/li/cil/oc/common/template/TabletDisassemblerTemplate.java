package li.cil.oc.common.template;

import li.cil.oc.common.ModItems;
import li.cil.oc.common.item.TabletItem;
import net.minecraft.world.item.ItemStack;

final class TabletDisassemblerTemplate implements DisassemblerTemplate {
    @Override
    public String name() {
        return "tablet";
    }

    @Override
    public boolean matches(final ItemStack stack) {
        return stack.getItem() instanceof TabletItem;
    }

    @Override
    public ItemStack[] disassemble(final ItemStack stack) {
        return ModItems.TABLET.get().disassembleToIngredients(stack);
    }
}
