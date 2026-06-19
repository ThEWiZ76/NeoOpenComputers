package li.cil.oc.common;

import li.cil.oc.api.detail.ManualAPI;
import li.cil.oc.api.manual.ContentProvider;
import li.cil.oc.api.manual.ImageProvider;
import li.cil.oc.api.manual.ImageRenderer;
import li.cil.oc.api.manual.PathProvider;
import li.cil.oc.api.manual.TabIconRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ManualRegistry implements ManualAPI {
    private final List<Tab> tabs = new ArrayList<>();
    private final List<PathProvider> pathProviders = new ArrayList<>();
    private final List<ContentProvider> contentProviders = new ArrayList<>();
    private final Map<String, ImageProvider> imageProviders = new LinkedHashMap<>();
    private boolean opened;
    private boolean reset;
    private String lastNavigationPath;

    @Override
    public void addTab(final TabIconRenderer renderer, final String tooltip, final String path) {
        tabs.add(new Tab(renderer, tooltip, path));
    }

    @Override
    public void addProvider(final PathProvider provider) {
        pathProviders.add(provider);
    }

    @Override
    public void addProvider(final ContentProvider provider) {
        contentProviders.add(provider);
    }

    @Override
    public void addProvider(final String prefix, final ImageProvider provider) {
        imageProviders.put(prefix, provider);
    }

    @Override
    public String pathFor(final ItemStack stack) {
        for (final PathProvider provider : pathProviders) {
            final String path = provider.pathFor(stack);
            if (path != null) {
                return path;
            }
        }
        return null;
    }

    @Override
    public String pathFor(final Level world, final BlockPos pos) {
        for (final PathProvider provider : pathProviders) {
            final String path = provider.pathFor(world, pos);
            if (path != null) {
                return path;
            }
        }
        return null;
    }

    @Override
    public Iterable<String> contentFor(final String path) {
        for (final ContentProvider provider : contentProviders) {
            final Iterable<String> content = provider.getContent(path);
            if (content != null) {
                return content;
            }
        }
        return null;
    }

    @Override
    public ImageRenderer imageFor(final String path) {
        final int separator = path.indexOf(':');
        if (separator < 1) {
            return null;
        }

        final ImageProvider provider = imageProviders.get(path.substring(0, separator));
        return provider == null ? null : provider.getImage(path.substring(separator + 1));
    }

    @Override
    public void openFor(final Player player) {
        opened = true;
    }

    @Override
    public void reset() {
        reset = true;
    }

    @Override
    public void navigate(final String path) {
        lastNavigationPath = path;
    }

    int tabCount() {
        return tabs.size();
    }

    boolean wasOpened() {
        return opened;
    }

    boolean wasReset() {
        return reset;
    }

    String lastNavigationPath() {
        return lastNavigationPath;
    }

    private record Tab(TabIconRenderer renderer, String tooltip, String path) {
    }
}
