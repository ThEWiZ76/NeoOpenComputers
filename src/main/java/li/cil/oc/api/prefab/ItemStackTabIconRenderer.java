package li.cil.oc.api.prefab;

import li.cil.oc.api.manual.TabIconRenderer;
import li.cil.oc.client.ManualRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ItemStackTabIconRenderer implements TabIconRenderer {
    private final ItemStack stack;

    public ItemStackTabIconRenderer(final ItemStack stack) {
        this.stack = stack;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render() {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final GuiGraphics activeGraphics = ManualRenderContext.currentGraphics();
        final GuiGraphics graphics = activeGraphics == null ? new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource()) : activeGraphics;
        graphics.renderItem(stack, 0, 0);
        if (activeGraphics == null) {
            graphics.flush();
        }
    }
}
