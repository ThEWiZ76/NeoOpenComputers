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

    /**
     * Registers a factory-backed disk using the active mod namespace and name as its persistent identity.
     * Call from the owning mod's loading context with a stable name unique within that mod.
     * Register the same name on every startup so existing disks can resolve their factory.
     */
    public static ItemStack registerFloppy(final String name, final DyeColor color, final Callable<li.cil.oc.api.fs.FileSystem> factory, final boolean doRecipeCycling) {
        if (API.items != null) {
            return API.items.registerFloppy(name, color, factory, doRecipeCycling);
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack registerEEPROM(final String name, final byte[] code, final byte[] data, final boolean readonly) {
        if (API.items != null) {
            return API.items.registerEEPROM(name, code, data, readonly);
        }
        return ItemStack.EMPTY;
    }

    private Items() {
    }
}
