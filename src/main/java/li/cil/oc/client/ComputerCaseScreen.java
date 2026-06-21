package li.cil.oc.client;

import li.cil.oc.common.menu.ComputerCaseMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class ComputerCaseScreen extends AbstractContainerScreen<ComputerCaseMenu> {
    public ComputerCaseScreen(final ComputerCaseMenu menu, final Inventory playerInventory, final Component title) {
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
        for (int slot = 0; slot < ComputerCaseMenu.COMPUTER_SLOT_COUNT; slot++) {
            drawSlot(guiGraphics, left + ComputerCaseMenu.computerSlotX(slot) - 1, top + ComputerCaseMenu.computerSlotY(slot) - 1);
        }
        guiGraphics.drawString(font, statusLabel(menu.computerState()), left + 8, top + 62, 0xFFD8DEE9, false);
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        if (mouseX >= leftPos + 8 && mouseX < leftPos + 168 && mouseY >= topPos + 60 && mouseY < topPos + 72) {
            guiGraphics.renderComponentTooltip(font, statusTooltip(menu.computerState(), menu.missingRequirements(), menu.componentCount(), menu.maxComponents()), mouseX, mouseY);
        }
    }

    public static Component statusLabel(final int state) {
        return Component.translatable(switch (state) {
            case ComputerCaseMenu.STATE_READY -> "gui.neoopencomputers.computer_case.state.ready";
            case ComputerCaseMenu.STATE_RUNNING -> "gui.neoopencomputers.computer_case.state.running";
            case ComputerCaseMenu.STATE_INCOMPLETE -> "gui.neoopencomputers.computer_case.state.incomplete";
            default -> "gui.neoopencomputers.computer_case.state.empty";
        });
    }

    public static List<Component> statusTooltip(final int state, final int missingRequirements, final int componentCount, final int maxComponents) {
        final List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("gui.neoopencomputers.computer_case.status"));
        tooltip.add(statusLabel(state));
        if (maxComponents > 0) {
            tooltip.add(Component.translatable("gui.neoopencomputers.computer_case.components", componentCount, maxComponents));
        }
        if ((missingRequirements & ComputerCaseMenu.MISSING_CPU) != 0) {
            tooltip.add(Component.translatable("gui.neoopencomputers.rack.missing.cpu"));
        }
        if ((missingRequirements & ComputerCaseMenu.MISSING_MEMORY) != 0) {
            tooltip.add(Component.translatable("gui.neoopencomputers.rack.missing.memory"));
        }
        if ((missingRequirements & ComputerCaseMenu.MISSING_EEPROM) != 0) {
            tooltip.add(Component.translatable("gui.neoopencomputers.rack.missing.eeprom"));
        }
        return tooltip;
    }

    private static void drawSlot(final GuiGraphics guiGraphics, final int left, final int top) {
        guiGraphics.fill(left - 1, top - 1, left + 17, top + 17, 0xFF1F232B);
        guiGraphics.fill(left, top, left + 16, top + 16, 0xFF4C566A);
    }
}
