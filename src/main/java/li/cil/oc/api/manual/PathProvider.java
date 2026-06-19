package li.cil.oc.api.manual;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface PathProvider {
    String pathFor(ItemStack stack);

    String pathFor(Level world, BlockPos pos);
}
