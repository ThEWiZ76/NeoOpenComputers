package li.cil.oc.api.detail;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public interface ItemInfo {
    String name();

    Block block();

    Item item();

    ItemStack createItemStack(int size);
}
