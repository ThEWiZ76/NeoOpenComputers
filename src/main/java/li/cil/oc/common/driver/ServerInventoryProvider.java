package li.cil.oc.common.driver;

import li.cil.oc.api.driver.InventoryProvider;
import li.cil.oc.common.component.ServerRackMountableEnvironment;
import li.cil.oc.common.item.ServerItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ServerInventoryProvider implements InventoryProvider {
    @Override
    public boolean worksWith(final ItemStack stack, final Player player) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof ServerItem;
    }

    @Override
    public Container getInventory(final ItemStack stack, final Player player) {
        if (!worksWith(stack, player)) {
            return null;
        }
        final ServerItem item = (ServerItem) stack.getItem();
        return new ServerRackMountableEnvironment(player, item.tier(stack), item.dataTag(stack));
    }
}
