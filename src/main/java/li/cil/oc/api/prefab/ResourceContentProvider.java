package li.cil.oc.api.prefab;

import li.cil.oc.api.manual.ContentProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;

public class ResourceContentProvider implements ContentProvider {
    private final String resourceDomain;
    private final String basePath;

    public ResourceContentProvider(final String resourceDomain, final String basePath) {
        this.resourceDomain = resourceDomain;
        this.basePath = basePath;
    }

    public ResourceContentProvider(final String resourceDomain) {
        this(resourceDomain, "");
    }

    @Override
    public Iterable<String> getContent(final String path) {
        try {
            final Optional<Resource> resource = Minecraft.getInstance()
                    .getResourceManager()
                    .getResource(resourceLocation(path));
            if (resource.isEmpty()) {
                return null;
            }

            try (BufferedReader reader = resource.get().openAsReader()) {
                final ArrayList<String> lines = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
                return lines;
            }
        } catch (final Throwable ignored) {
            return null;
        }
    }

    protected ResourceLocation resourceLocation(final String path) {
        final String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        return ResourceLocation.fromNamespaceAndPath(resourceDomain, (basePath + normalizedPath).toLowerCase(Locale.ROOT));
    }
}
