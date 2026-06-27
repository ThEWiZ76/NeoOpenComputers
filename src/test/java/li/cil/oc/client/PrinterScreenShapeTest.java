package li.cil.oc.client;

import li.cil.oc.common.menu.PrinterMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Inventory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PrinterScreenShapeTest {
    @Test
    void printerScreenHasMenuConstructor() throws NoSuchMethodException {
        final Constructor<PrinterScreen> constructor = PrinterScreen.class.getConstructor(
            PrinterMenu.class,
            Inventory.class,
            Component.class);

        assertTrue(AbstractContainerScreen.class.isAssignableFrom(PrinterScreen.class));
        assertArrayEquals(new Class<?>[]{PrinterMenu.class, Inventory.class, Component.class}, constructor.getParameterTypes());
    }

    @Test
    void printerScreenExposesTooltips() {
        final List<Component> materialTooltip = PrinterScreen.materialTooltip(10, 20);
        final List<Component> inkTooltip = PrinterScreen.inkTooltip(30, 40);
        final List<Component> progressTooltip = PrinterScreen.progressTooltip(50);

        assertEquals(2, materialTooltip.size());
        assertTranslationKey("gui.neoopencomputers.printer.material", materialTooltip.get(0));
        assertEquals("10/20", materialTooltip.get(1).getString());
        assertEquals(2, inkTooltip.size());
        assertTranslationKey("gui.neoopencomputers.printer.ink", inkTooltip.get(0));
        assertEquals("30/40", inkTooltip.get(1).getString());
        assertEquals(2, progressTooltip.size());
        assertTranslationKey("gui.neoopencomputers.printer.progress", progressTooltip.get(0));
        assertEquals("50%", progressTooltip.get(1).getString());
    }

    private static void assertTranslationKey(final String expected, final Component component) {
        assertTrue(component.getContents() instanceof TranslatableContents);
        assertEquals(expected, ((TranslatableContents) component.getContents()).getKey());
    }
}
