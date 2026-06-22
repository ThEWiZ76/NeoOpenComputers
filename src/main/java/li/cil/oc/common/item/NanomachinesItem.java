package li.cil.oc.common.item;

import li.cil.oc.api.API;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.common.NanomachinesRegistry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class NanomachinesItem extends Item {
    public NanomachinesItem(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand usedHand) {
        final ItemStack stack = player.getItemInHand(usedHand);
        player.startUsingItem(usedHand);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public UseAnim getUseAnimation(final ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public int getUseDuration(final ItemStack stack, final LivingEntity entity) {
        return 32;
    }

    @Override
    public ItemStack finishUsingItem(final ItemStack stack, final Level level, final LivingEntity livingEntity) {
        if (livingEntity instanceof Player player && !level.isClientSide) {
            final var itemData = NanomachineItemData.dataTag(stack).copy();
            li.cil.oc.api.Nanomachines.uninstallController(player);
            if (API.nanomachines instanceof NanomachinesRegistry registry) {
                registry.installController(player, itemData);
            } else {
                final Controller controller = li.cil.oc.api.Nanomachines.installController(player);
                if (controller != null) {
                    controller.reconfigure();
                }
            }
            stack.shrink(1);
        }
        return stack;
    }
}
