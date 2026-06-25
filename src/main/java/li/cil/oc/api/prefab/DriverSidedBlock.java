package li.cil.oc.api.prefab;

import li.cil.oc.api.driver.DriverBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public abstract class DriverSidedBlock implements DriverBlock {
    protected final ItemStack[] blocks;

    protected DriverSidedBlock(final ItemStack... blocks) {
        this.blocks = blocks == null ? new ItemStack[0] : blocks.clone();
    }

    @Override
    public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
        if (world == null || pos == null) {
            return false;
        }

        return worksWith(world.getBlockState(pos).getBlock());
    }

    protected boolean worksWith(final Block block) {
        if (block == null) {
            return false;
        }

        for (final ItemStack stack : blocks) {
            if (stack != null && !stack.isEmpty() && stack.getItem() instanceof final BlockItem blockItem &&
                    blockItem.getBlock() == block) {
                return true;
            }
        }

        return false;
    }

    /**
     * @deprecated Minecraft 1.21 no longer exposes block metadata; override {@link #worksWith(Block)} instead.
     */
    @Deprecated
    protected boolean worksWith(final Block block, final int metadata) {
        return worksWith(block);
    }
}
