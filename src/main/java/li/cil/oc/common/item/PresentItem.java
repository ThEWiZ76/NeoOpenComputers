package li.cil.oc.common.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class PresentItem extends Item {
    public PresentItem(final Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final var stack = player.getItemInHand(hand);
        if (stack.isEmpty()) return InteractionResultHolder.fail(stack);
        if (!level.isClientSide) {
            final var gift = PresentLoot.next(level, player.getRandom());
            if (gift.isEmpty()) return InteractionResultHolder.fail(stack);
            stack.shrink(1);
            level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 0.2F, 1);
            deliver(player, gift);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static void deliver(final Player player, final ItemStack stack) {
        player.getInventory().add(stack);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        if (!stack.isEmpty()) player.drop(stack, false);
    }
}
