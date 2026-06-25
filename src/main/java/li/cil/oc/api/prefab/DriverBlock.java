package li.cil.oc.api.prefab;

import net.minecraft.world.item.ItemStack;

/**
 * @deprecated Use {@link DriverSidedBlock} instead.
 */
@Deprecated
public abstract class DriverBlock extends DriverSidedBlock {
    protected DriverBlock(final ItemStack... blocks) {
        super(blocks);
    }
}
