package li.cil.oc.common.template;

import li.cil.oc.common.blockentity.AssemblerBlockEntity;
import net.minecraft.world.item.ItemStack;

public interface AssemblerTemplate {
    String name();

    boolean matches(ItemStack stack);

    boolean validate(AssemblerBlockEntity assembler);

    default boolean canPlaceItem(final AssemblerBlockEntity assembler, final int slot, final ItemStack stack) {
        return slot == AssemblerBlockEntity.SLOT_TEMPLATE && matches(stack);
    }

    ItemStack assemble(AssemblerBlockEntity assembler);

    default double energyRequired(final AssemblerBlockEntity assembler) {
        return 1D;
    }
}
