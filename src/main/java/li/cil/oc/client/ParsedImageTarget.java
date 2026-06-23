package li.cil.oc.client;

import net.minecraft.resources.ResourceLocation;

record ParsedImageTarget(ResourceLocation id) {
    static ParsedImageTarget parse(final String data) {
        if (data == null || data.isBlank()) {
            return null;
        }
        final int metadataAt = data.lastIndexOf('@');
        final String id = metadataAt > 0 ? data.substring(0, metadataAt) : data;
        try {
            return new ParsedImageTarget(ResourceLocation.parse(id));
        } catch (final Exception ignored) {
            return null;
        }
    }
}
