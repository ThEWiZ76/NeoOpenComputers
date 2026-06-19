package li.cil.oc.api.detail;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.Callable;

public interface ItemAPI {
    ItemInfo get(String name);

    ItemInfo get(ItemStack stack);

    ItemStack registerFloppy(String name, DyeColor color, Callable<li.cil.oc.api.fs.FileSystem> factory, boolean doRecipeCycling);

    ItemStack registerEEPROM(String name, byte[] code, byte[] data, boolean readonly);
}
