package li.cil.oc.client;

import li.cil.oc.common.menu.AssemblerMenu;
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

final class AssemblerScreenShapeTest {
    @Test
    void assemblerScreenHasMenuConstructor() throws NoSuchMethodException {
        final Constructor<AssemblerScreen> constructor = AssemblerScreen.class.getConstructor(
            AssemblerMenu.class,
            Inventory.class,
            Component.class);

        assertTrue(AbstractContainerScreen.class.isAssignableFrom(AssemblerScreen.class));
        assertArrayEquals(new Class<?>[]{AssemblerMenu.class, Inventory.class, Component.class}, constructor.getParameterTypes());
    }

    @Test
    void assemblerScreenExposesStatusLabels() throws NoSuchMethodException {
        final Method statusLabel = AssemblerScreen.class.getMethod("statusLabel", int.class, int.class);
        final Method statusTooltip = AssemblerScreen.class.getMethod("statusTooltip", int.class, int.class);

        assertEquals(Component.class, statusLabel.getReturnType());
        assertEquals(List.class, statusTooltip.getReturnType());
    }

    @Test
    void assemblerStatusTooltipShowsStateAndProgress() {
        assertTranslationKey("gui.neoopencomputers.assembler.state.idle", AssemblerScreen.statusLabel(AssemblerMenu.STATE_IDLE, 0));
        assertTranslationKey("gui.neoopencomputers.assembler.state.ready", AssemblerScreen.statusLabel(AssemblerMenu.STATE_READY, 0));
        assertTranslationKey("gui.neoopencomputers.assembler.state.busy", AssemblerScreen.statusLabel(AssemblerMenu.STATE_BUSY, 42));

        final List<Component> tooltip = AssemblerScreen.statusTooltip(AssemblerMenu.STATE_BUSY, 42);

        assertEquals(3, tooltip.size());
        assertTranslationKey("gui.neoopencomputers.assembler.status", tooltip.get(0));
        assertTranslationKey("gui.neoopencomputers.assembler.state.busy", tooltip.get(1));
        assertTranslationKey("gui.neoopencomputers.assembler.progress", tooltip.get(2));
    }

    private static void assertTranslationKey(final String expected, final Component component) {
        assertTrue(component.getContents() instanceof TranslatableContents);
        assertEquals(expected, ((TranslatableContents) component.getContents()).getKey());
    }
}
