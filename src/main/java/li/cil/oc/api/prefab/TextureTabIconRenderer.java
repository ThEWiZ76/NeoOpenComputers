package li.cil.oc.api.prefab;

import li.cil.oc.api.manual.TabIconRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class TextureTabIconRenderer implements TabIconRenderer {
    private final ResourceLocation location;

    public TextureTabIconRenderer(final ResourceLocation location) {
        this.location = location;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render() {
        if (location == null) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final GuiGraphics graphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
        graphics.blit(location, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
        graphics.flush();
    }
}
