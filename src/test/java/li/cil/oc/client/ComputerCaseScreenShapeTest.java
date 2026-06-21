package li.cil.oc.client;

import li.cil.oc.common.menu.ComputerCaseMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ComputerCaseScreenShapeTest {
    @Test
    void computerCaseScreenHasMenuConstructor() throws NoSuchMethodException {
        final Constructor<ComputerCaseScreen> constructor = ComputerCaseScreen.class.getConstructor(
            ComputerCaseMenu.class,
            Inventory.class,
            Component.class);

        assertTrue(AbstractContainerScreen.class.isAssignableFrom(ComputerCaseScreen.class));
        assertArrayEquals(new Class<?>[]{ComputerCaseMenu.class, Inventory.class, Component.class}, constructor.getParameterTypes());
    }

    @Test
    void computerCaseScreenExposesStatusLabels() throws NoSuchMethodException {
        final Method statusLabel = ComputerCaseScreen.class.getMethod("statusLabel", int.class);
        final Method statusTooltip = ComputerCaseScreen.class.getMethod("statusTooltip", int.class, int.class, int.class, int.class);

        assertEquals(Component.class, statusLabel.getReturnType());
        assertEquals(List.class, statusTooltip.getReturnType());
    }

    @Test
    void computerCaseStatusTooltipShowsStateAndMissingRequirements() {
        assertTranslationKey("gui.neoopencomputers.computer_case.state.empty", ComputerCaseScreen.statusLabel(ComputerCaseMenu.STATE_EMPTY));
        assertTranslationKey("gui.neoopencomputers.computer_case.state.ready", ComputerCaseScreen.statusLabel(ComputerCaseMenu.STATE_READY));
        assertTranslationKey("gui.neoopencomputers.computer_case.state.running", ComputerCaseScreen.statusLabel(ComputerCaseMenu.STATE_RUNNING));
        assertTranslationKey("gui.neoopencomputers.computer_case.state.incomplete", ComputerCaseScreen.statusLabel(ComputerCaseMenu.STATE_INCOMPLETE));

        final List<Component> tooltip = ComputerCaseScreen.statusTooltip(
            ComputerCaseMenu.STATE_INCOMPLETE,
            ComputerCaseMenu.MISSING_CPU | ComputerCaseMenu.MISSING_MEMORY | ComputerCaseMenu.MISSING_EEPROM,
            0,
            0);

        assertEquals(5, tooltip.size());
        assertTranslationKey("gui.neoopencomputers.computer_case.status", tooltip.get(0));
        assertTranslationKey("gui.neoopencomputers.computer_case.state.incomplete", tooltip.get(1));
        assertTranslationKey("gui.neoopencomputers.rack.missing.cpu", tooltip.get(2));
        assertTranslationKey("gui.neoopencomputers.rack.missing.memory", tooltip.get(3));
        assertTranslationKey("gui.neoopencomputers.rack.missing.eeprom", tooltip.get(4));
    }

    @Test
    void computerCaseStatusTooltipShowsComponentCapacity() {
        final List<Component> tooltip = ComputerCaseScreen.statusTooltip(
            ComputerCaseMenu.STATE_READY,
            0,
            3,
            8);

        assertEquals(3, tooltip.size());
        assertTranslationKey("gui.neoopencomputers.computer_case.status", tooltip.get(0));
        assertTranslationKey("gui.neoopencomputers.computer_case.state.ready", tooltip.get(1));
        assertTranslationKey("gui.neoopencomputers.computer_case.components", tooltip.get(2));
        assertEquals(3, ((TranslatableContents) tooltip.get(2).getContents()).getArgs()[0]);
        assertEquals(8, ((TranslatableContents) tooltip.get(2).getContents()).getArgs()[1]);
    }

    private static void assertTranslationKey(final String expected, final Component component) {
        assertTrue(component.getContents() instanceof TranslatableContents);
        assertEquals(expected, ((TranslatableContents) component.getContents()).getKey());
    }
}
