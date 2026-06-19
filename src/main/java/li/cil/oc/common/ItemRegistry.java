package li.cil.oc.common;

import li.cil.oc.api.detail.ItemAPI;
import li.cil.oc.api.detail.ItemInfo;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

final class ItemRegistry implements ItemAPI {
    private final Map<String, RegisteredItemInfo> infosByName = new LinkedHashMap<>();

    RegisteredItemInfo register(final String name, final Block block, final Item item) {
        final RegisteredItemInfo info = new RegisteredItemInfo(name, block, item);
        infosByName.put(name, info);
        return info;
    }

    @Override
    public ItemInfo get(final String name) {
        return infosByName.get(name);
    }

    @Override
    public ItemInfo get(final ItemStack stack) {
        if (stack == null) {
            return null;
        }
        final Item item = stack.getItem();
        return infosByName.values().stream()
            .filter(info -> info.item() == item)
            .findFirst()
            .orElse(null);
    }

    @Override
    public ItemStack registerFloppy(final String name, final DyeColor color, final Callable<li.cil.oc.api.fs.FileSystem> factory, final boolean doRecipeCycling) {
        return null;
    }

    @Override
    public ItemStack registerEEPROM(final String name, final byte[] code, final byte[] data, final boolean readonly) {
        return null;
    }

    private record RegisteredItemInfo(String name, Block block, Item item) implements ItemInfo {
        @Override
        public ItemStack createItemStack(final int size) {
            if (item == null) {
                return null;
            }
            return new ItemStack(item, size);
        }
    }
}
