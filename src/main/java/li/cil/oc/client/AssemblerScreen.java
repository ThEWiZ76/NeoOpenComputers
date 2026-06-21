package li.cil.oc.client;

import li.cil.oc.common.menu.AssemblerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class AssemblerScreen extends AbstractContainerScreen<AssemblerMenu> {
    public AssemblerScreen(final AssemblerMenu menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
        imageHeight = 204;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(final GuiGraphics guiGraphics, final float partialTick, final int mouseX, final int mouseY) {
        final int left = leftPos;
        final int top = topPos;
        guiGraphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFF2E3440);
        guiGraphics.fill(left + 7, top + 16, left + 169, top + 114, 0xFF3B4252);
        for (int slot = 0; slot < AssemblerMenu.ASSEMBLER_SLOT_COUNT; slot++) {
            drawSlot(guiGraphics, left + AssemblerMenu.assemblerSlotX(slot) - 1, top + AssemblerMenu.assemblerSlotY(slot) - 1);
        }
        guiGraphics.drawString(font, statusLabel(menu.assemblyState(), menu.assemblyProgress()), left + 8, top + 100, 0xFFD8DEE9, false);
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        if (mouseX >= leftPos + 8 && mouseX < leftPos + 168 && mouseY >= topPos + 98 && mouseY < topPos + 110) {
            guiGraphics.renderComponentTooltip(font, statusTooltip(menu.assemblyState(), menu.assemblyProgress()), mouseX, mouseY);
        }
    }

    public static Component statusLabel(final int state, final int progress) {
        return Component.translatable(switch (state) {
            case AssemblerMenu.STATE_READY -> "gui.neoopencomputers.assembler.state.ready";
            case AssemblerMenu.STATE_BUSY -> "gui.neoopencomputers.assembler.state.busy";
            default -> "gui.neoopencomputers.assembler.state.idle";
        }, progress);
    }

    public static List<Component> statusTooltip(final int state, final int progress) {
        if (state == AssemblerMenu.STATE_BUSY) {
            return List.of(
                Component.translatable("gui.neoopencomputers.assembler.status"),
                statusLabel(state, progress),
                Component.translatable("gui.neoopencomputers.assembler.progress", progress));
        }
        return List.of(Component.translatable("gui.neoopencomputers.assembler.status"), statusLabel(state, progress));
    }

    private static void drawSlot(final GuiGraphics guiGraphics, final int left, final int top) {
        guiGraphics.fill(left - 1, top - 1, left + 17, top + 17, 0xFF1F232B);
        guiGraphics.fill(left, top, left + 16, top + 16, 0xFF4C566A);
    }
}
