package li.cil.oc.client;

import li.cil.oc.common.menu.DisassemblerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class DisassemblerScreen extends AbstractContainerScreen<DisassemblerMenu> {
    public DisassemblerScreen(final DisassemblerMenu menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(final GuiGraphics guiGraphics, final float partialTick, final int mouseX, final int mouseY) {
        final int left = leftPos;
        final int top = topPos;
        guiGraphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFF2E3440);
        guiGraphics.fill(left + 7, top + 16, left + 169, top + 76, 0xFF3B4252);
        for (int slot = 0; slot < DisassemblerMenu.DISASSEMBLER_SLOT_COUNT; slot++) {
            drawSlot(guiGraphics, left + DisassemblerMenu.disassemblerSlotX(slot) - 1, top + DisassemblerMenu.disassemblerSlotY(slot) - 1);
        }
        guiGraphics.drawString(font, statusLabel(menu.disassemblyState()), left + 8, top + 62, 0xFFD8DEE9, false);
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        if (mouseX >= leftPos + 8 && mouseX < leftPos + 168 && mouseY >= topPos + 60 && mouseY < topPos + 72) {
            guiGraphics.renderComponentTooltip(font, statusTooltip(menu.disassemblyState()), mouseX, mouseY);
        }
    }

    public static Component statusLabel(final int state) {
        return Component.translatable(switch (state) {
            case DisassemblerMenu.STATE_READY -> "gui.neoopencomputers.disassembler.state.ready";
            case DisassemblerMenu.STATE_BLOCKED -> "gui.neoopencomputers.disassembler.state.blocked";
            default -> "gui.neoopencomputers.disassembler.state.empty";
        });
    }

    public static List<Component> statusTooltip(final int state) {
        return List.of(Component.translatable("gui.neoopencomputers.disassembler.status"), statusLabel(state));
    }

    private static void drawSlot(final GuiGraphics guiGraphics, final int left, final int top) {
        guiGraphics.fill(left - 1, top - 1, left + 17, top + 17, 0xFF1F232B);
        guiGraphics.fill(left, top, left + 16, top + 16, 0xFF4C566A);
    }
}
