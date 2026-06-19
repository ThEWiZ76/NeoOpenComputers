package li.cil.oc.api.prefab;

import li.cil.oc.api.manual.TabIconRenderer;
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
        final GuiGraphics graphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
        graphics.renderItem(stack, 0, 0);
        graphics.flush();
    }
}
