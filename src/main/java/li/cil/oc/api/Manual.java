package li.cil.oc.api;

import li.cil.oc.api.manual.ContentProvider;
import li.cil.oc.api.manual.ImageProvider;
import li.cil.oc.api.manual.ImageRenderer;
import li.cil.oc.api.manual.PathProvider;
import li.cil.oc.api.manual.TabIconRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class Manual {
    public static void addTab(final TabIconRenderer renderer, final String tooltip, final String path) {
        if (API.manual != null) {
            API.manual.addTab(renderer, tooltip, path);
        }
    }

    public static void addProvider(final PathProvider provider) {
        if (API.manual != null) {
            API.manual.addProvider(provider);
        }
    }

    public static void addProvider(final ContentProvider provider) {
        if (API.manual != null) {
            API.manual.addProvider(provider);
        }
    }

    public static void addProvider(final String prefix, final ImageProvider provider) {
        if (API.manual != null) {
            API.manual.addProvider(prefix, provider);
        }
    }

    public static String pathFor(final ItemStack stack) {
        if (API.manual != null) {
            return API.manual.pathFor(stack);
        }
        return null;
    }

    public static String pathFor(final Level world, final BlockPos pos) {
        if (API.manual != null) {
            return API.manual.pathFor(world, pos);
        }
        return null;
    }

    public static Iterable<String> contentFor(final String path) {
        if (API.manual != null) {
            return API.manual.contentFor(path);
        }
        return null;
    }

    public static ImageRenderer imageFor(final String path) {
        if (API.manual != null) {
            return API.manual.imageFor(path);
        }
        return null;
    }

    public static void openFor(final Player player) {
        if (API.manual != null) {
            API.manual.openFor(player);
        }
    }

    public static void reset() {
        if (API.manual != null) {
            API.manual.reset();
        }
    }

    public static void navigate(final String path) {
        if (API.manual != null) {
            API.manual.navigate(path);
        }
    }

    private Manual() {
    }
}
