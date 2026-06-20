package li.cil.oc.common.template;

import net.minecraft.world.item.ItemStack;

public interface DisassemblerTemplate {
    String name();

    boolean matches(ItemStack stack);

    ItemStack[] disassemble(ItemStack stack);
}
