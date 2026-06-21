package li.cil.oc.client;

import li.cil.oc.common.menu.DisassemblerMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Inventory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DisassemblerScreenShapeTest {
    @Test
    void disassemblerScreenHasMenuConstructor() throws NoSuchMethodException {
        final Constructor<DisassemblerScreen> constructor = DisassemblerScreen.class.getConstructor(
            DisassemblerMenu.class,
            Inventory.class,
            Component.class);

        assertTrue(AbstractContainerScreen.class.isAssignableFrom(DisassemblerScreen.class));
        assertArrayEquals(new Class<?>[]{DisassemblerMenu.class, Inventory.class, Component.class}, constructor.getParameterTypes());
    }

    @Test
    void disassemblerScreenExposesStatusLabels() throws NoSuchMethodException {
        final Method statusLabel = DisassemblerScreen.class.getMethod("statusLabel", int.class);
        final Method statusTooltip = DisassemblerScreen.class.getMethod("statusTooltip", int.class);

        assertEquals(Component.class, statusLabel.getReturnType());
        assertEquals(List.class, statusTooltip.getReturnType());
    }

    @Test
    void disassemblerStatusTooltipShowsState() {
        assertTranslationKey("gui.neoopencomputers.disassembler.state.empty", DisassemblerScreen.statusLabel(DisassemblerMenu.STATE_EMPTY));
        assertTranslationKey("gui.neoopencomputers.disassembler.state.ready", DisassemblerScreen.statusLabel(DisassemblerMenu.STATE_READY));
        assertTranslationKey("gui.neoopencomputers.disassembler.state.blocked", DisassemblerScreen.statusLabel(DisassemblerMenu.STATE_BLOCKED));

        final List<Component> tooltip = DisassemblerScreen.statusTooltip(DisassemblerMenu.STATE_READY);

        assertEquals(2, tooltip.size());
        assertTranslationKey("gui.neoopencomputers.disassembler.status", tooltip.get(0));
        assertTranslationKey("gui.neoopencomputers.disassembler.state.ready", tooltip.get(1));
    }

    private static void assertTranslationKey(final String expected, final Component component) {
        assertTrue(component.getContents() instanceof TranslatableContents);
        assertEquals(expected, ((TranslatableContents) component.getContents()).getKey());
    }
}
