package li.cil.oc.client;

import net.minecraft.client.gui.GuiGraphics;

public final class ManualRenderContext {
    private static final ThreadLocal<GuiGraphics> CURRENT = new ThreadLocal<>();

    public static void withGraphics(final GuiGraphics graphics, final Runnable renderer) {
        final GuiGraphics previous = CURRENT.get();
        CURRENT.set(graphics);
        try {
            renderer.run();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    public static GuiGraphics currentGraphics() {
        return CURRENT.get();
    }

    private ManualRenderContext() {
    }
}
