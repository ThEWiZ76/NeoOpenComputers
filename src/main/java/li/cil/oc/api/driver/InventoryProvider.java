package li.cil.oc.api.driver;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface InventoryProvider {
    boolean worksWith(ItemStack stack, Player player);

    Container getInventory(ItemStack stack, Player player);
}
