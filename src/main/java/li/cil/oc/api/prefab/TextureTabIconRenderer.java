package li.cil.oc.api.prefab;

import li.cil.oc.api.manual.TabIconRenderer;
import li.cil.oc.client.ManualRenderContext;
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
        final GuiGraphics activeGraphics = ManualRenderContext.currentGraphics();
        final GuiGraphics graphics = activeGraphics == null ? new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource()) : activeGraphics;
        graphics.blit(location, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
        if (activeGraphics == null) {
            graphics.flush();
        }
    }
}
