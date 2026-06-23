package li.cil.oc.client;

import li.cil.oc.api.manual.ImageRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

public final class ItemStackImageRenderer implements ImageRenderer {
    private static final int CYCLE_SPEED_MS = 1000;
    private final List<ItemStack> stacks;

    public ItemStackImageRenderer(final List<ItemStack> stacks) {
        this.stacks = List.copyOf(stacks);
    }

    public List<ItemStack> stacks() {
        return stacks;
    }

    @Override
    public int getWidth() {
        return 32;
    }

    @Override
    public int getHeight() {
        return 32;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(final int mouseX, final int mouseY) {
        if (stacks.isEmpty()) {
            return;
        }

        final int index = (int) ((System.currentTimeMillis() % ((long) CYCLE_SPEED_MS * stacks.size())) / CYCLE_SPEED_MS);
        final Minecraft minecraft = Minecraft.getInstance();
        final GuiGraphics graphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
        graphics.pose().pushPose();
        graphics.pose().scale(2.0F, 2.0F, 2.0F);
        graphics.renderItem(stacks.get(index), 0, 0);
        graphics.pose().popPose();
        graphics.flush();
    }
}
