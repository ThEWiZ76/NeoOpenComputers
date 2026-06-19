package li.cil.oc.api;

import li.cil.oc.api.detail.ItemInfo;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.Callable;

public final class Items {
    public static ItemInfo get(final String name) {
        if (API.items != null) {
            return API.items.get(name);
        }
        return null;
    }

    public static ItemInfo get(final ItemStack stack) {
        if (API.items != null) {
            return API.items.get(stack);
        }
        return null;
    }

    public static ItemStack registerFloppy(final String name, final DyeColor color, final Callable<li.cil.oc.api.fs.FileSystem> factory, final boolean doRecipeCycling) {
        if (API.items != null) {
            return API.items.registerFloppy(name, color, factory, doRecipeCycling);
        }
        return null;
    }

    public static ItemStack registerEEPROM(final String name, final byte[] code, final byte[] data, final boolean readonly) {
        if (API.items != null) {
            return API.items.registerEEPROM(name, code, data, readonly);
        }
        return null;
    }

    private Items() {
    }
}
