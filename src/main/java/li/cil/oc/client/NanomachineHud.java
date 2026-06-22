package li.cil.oc.client;

import li.cil.oc.api.API;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.common.ModSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

final class NanomachineHud {
    private static final int WIDTH = 8;
    private static final int HEIGHT = 12;
    private static final int BACKGROUND = 0xAA11151A;
    private static final int BORDER = 0xCC38414C;
    private static final int FILL = 0xCC4DE8B6;

    private NanomachineHud() {
    }

    static void render(final GuiGraphics graphics, final Minecraft minecraft) {
        if (minecraft.player == null || API.nanomachines == null) {
            return;
        }
        final Controller controller = API.nanomachines.getController(minecraft.player);
        if (controller == null || controller.getLocalBufferSize() <= 0D) {
            return;
        }
        final int screenWidth = graphics.guiWidth();
        final int screenHeight = graphics.guiHeight();
        final double fill = controller.getLocalBuffer() / controller.getLocalBufferSize();
        final Layout layout = layout(screenWidth, screenHeight, ModSettings.nanomachineHudPos(), fill);
        graphics.fill(layout.left(), layout.top(), layout.left() + layout.width(), layout.top() + layout.height(), BACKGROUND);
        graphics.fill(layout.left(), layout.top(), layout.left() + layout.width(), layout.top() + 1, BORDER);
        graphics.fill(layout.left(), layout.top() + layout.height() - 1, layout.left() + layout.width(), layout.top() + layout.height(), BORDER);
        graphics.fill(layout.left(), layout.top(), layout.left() + 1, layout.top() + layout.height(), BORDER);
        graphics.fill(layout.left() + layout.width() - 1, layout.top(), layout.left() + layout.width(), layout.top() + layout.height(), BORDER);
        graphics.fill(layout.left() + 1, layout.fillTop(), layout.left() + layout.width() - 1, layout.top() + layout.height() - 1, FILL);
    }

    static Layout layout(final int screenWidth, final int screenHeight, final List<Double> position, final double fill) {
        final double x = position.size() > 0 ? position.get(0) : -1D;
        final double y = position.size() > 1 ? position.get(1) : -1D;
        final int left = (int) Math.min(screenWidth - WIDTH, x < 0D ? screenWidth / 2D - 91D - 12D : x < 1D ? screenWidth * x : x);
        final int top = (int) Math.min(screenHeight - HEIGHT, y < 0D ? screenHeight - 39D : y < 1D ? screenHeight * y : y);
        final double clampedFill = Math.clamp(fill, 0D, 1D);
        final int fillTop = top + (int) Math.round(HEIGHT * (1D - clampedFill));
        return new Layout(left, top, WIDTH, HEIGHT, fillTop);
    }

    record Layout(int left, int top, int width, int height, int fillTop) {
    }
}
