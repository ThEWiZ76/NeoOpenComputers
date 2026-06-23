package li.cil.oc.client;

import li.cil.oc.api.manual.ImageRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Optional;

public class TextureImageRenderer implements ImageRenderer {
    private final ResourceLocation location;
    private int width = -1;
    private int height = -1;

    public TextureImageRenderer(final ResourceLocation location) {
        this.location = location;
    }

    public ResourceLocation location() {
        return location;
    }

    @Override
    public int getWidth() {
        loadSize();
        return width > 0 ? width : 16;
    }

    @Override
    public int getHeight() {
        loadSize();
        return height > 0 ? height : 16;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(final int mouseX, final int mouseY) {
        final Minecraft minecraft = Minecraft.getInstance();
        final GuiGraphics graphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
        graphics.blit(location, 0, 0, 0.0F, 0.0F, getWidth(), getHeight(), getWidth(), getHeight());
        graphics.flush();
    }

    private void loadSize() {
        if (width >= 0 && height >= 0) {
            return;
        }
        width = 16;
        height = 16;

        if (tryLoadSizeFromClasspath()) {
            return;
        }
        tryLoadSizeFromResourceManager();
    }

    private boolean tryLoadSizeFromClasspath() {
        final String path = "/assets/" + location.getNamespace() + "/" + location.getPath();
        try (InputStream stream = TextureImageRenderer.class.getResourceAsStream(path)) {
            return loadSize(stream);
        } catch (final Exception ignored) {
            return false;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void tryLoadSizeFromResourceManager() {
        try {
            final Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
            if (resource.isPresent()) {
                try (InputStream stream = resource.get().open()) {
                    loadSize(stream);
                }
            }
        } catch (final Exception ignored) {
        }
    }

    private boolean loadSize(final InputStream stream) throws Exception {
        if (stream == null) {
            return false;
        }
        final BufferedImage image = ImageIO.read(stream);
        if (image == null) {
            return false;
        }
        width = image.getWidth();
        height = image.getHeight();
        return true;
    }
}
