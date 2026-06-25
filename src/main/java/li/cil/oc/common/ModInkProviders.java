package li.cil.oc.common;

import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;

public final class ModInkProviders {
    public static final int UPSTREAM_INK_CARTRIDGE_VALUE = 50000;
    public static final int UPSTREAM_DYE_VALUE = UPSTREAM_INK_CARTRIDGE_VALUE / 10;

    private static boolean registered;

    public static void registerDefaults() {
        if (registered) {
            return;
        }
        registered = true;
        try {
            InkProviders.add(ModInkProviders.class.getMethod("inkCartridgeInkProvider", ItemStack.class));
            InkProviders.add(ModInkProviders.class.getMethod("dyeInkProvider", ItemStack.class));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Missing default ink provider callbacks", e);
        }
    }

    public static int inkCartridgeInkProvider(final ItemStack stack) {
        return stack != null && stack.is(ModItems.INK_CARTRIDGE.get()) ? UPSTREAM_INK_CARTRIDGE_VALUE : 0;
    }

    public static int dyeInkProvider(final ItemStack stack) {
        return stack != null && stack.getItem() instanceof DyeItem ? UPSTREAM_DYE_VALUE : 0;
    }

    private ModInkProviders() {
    }
}
