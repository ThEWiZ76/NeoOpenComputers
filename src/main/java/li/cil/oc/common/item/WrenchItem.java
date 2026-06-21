package li.cil.oc.common.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

public class WrenchItem extends Item implements li.cil.oc.api.internal.Wrench {
    public WrenchItem(final Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level level = context.getLevel();
        if (!rotateBlock(level, context.getClickedPos(), level.isClientSide())) {
            return InteractionResult.PASS;
        }
        final Player player = context.getPlayer();
        if (player != null && !level.isClientSide()) {
            player.swing(context.getHand());
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean useWrenchOnBlock(final Player player, final Level world, final BlockPos pos, final boolean simulate) {
        if (!rotateBlock(world, pos, simulate)) {
            return false;
        }
        if (player != null && !simulate) {
            player.swing(player.getUsedItemHand());
        }
        return true;
    }

    @Override
    public boolean doesSneakBypassUse(final ItemStack stack, final LevelReader level, final BlockPos pos, final Player player) {
        return true;
    }

    public static boolean rotateBlock(final Level level, final BlockPos pos, final boolean simulate) {
        if (level == null || !level.isLoaded(pos)) {
            return false;
        }
        final BlockState state = level.getBlockState(pos);
        final BlockState rotated = state.rotate(Rotation.CLOCKWISE_90);
        if (rotated == state) {
            return false;
        }
        if (!simulate) {
            level.setBlock(pos, rotated, Block.UPDATE_ALL);
        }
        return true;
    }
}
