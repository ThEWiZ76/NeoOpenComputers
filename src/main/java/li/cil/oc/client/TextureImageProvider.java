package li.cil.oc.client;

import li.cil.oc.api.manual.ImageProvider;
import li.cil.oc.api.manual.ImageRenderer;
import net.minecraft.resources.ResourceLocation;

public final class TextureImageProvider implements ImageProvider {
    @Override
    public ImageRenderer getImage(final String data) {
        try {
            return new TextureImageRenderer(ResourceLocation.parse(data));
        } catch (final Exception ignored) {
            return new MissingManualImageRenderer("oc:gui.Manual.Warning.ImageMissing");
        }
    }
}
