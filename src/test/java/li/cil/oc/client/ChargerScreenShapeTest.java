package li.cil.oc.client;

import li.cil.oc.common.menu.ChargerMenu;
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

final class ChargerScreenShapeTest {
    @Test
    void chargerScreenHasMenuConstructor() throws NoSuchMethodException {
        final Constructor<ChargerScreen> constructor = ChargerScreen.class.getConstructor(
            ChargerMenu.class,
            Inventory.class,
            Component.class);

        assertTrue(AbstractContainerScreen.class.isAssignableFrom(ChargerScreen.class));
        assertArrayEquals(new Class<?>[]{ChargerMenu.class, Inventory.class, Component.class}, constructor.getParameterTypes());
    }

    @Test
    void chargerScreenExposesStatusLabels() throws NoSuchMethodException {
        final Method statusLabel = ChargerScreen.class.getMethod("statusLabel");
        final Method statusTooltip = ChargerScreen.class.getMethod("statusTooltip");

        assertEquals(Component.class, statusLabel.getReturnType());
        assertEquals(List.class, statusTooltip.getReturnType());
    }

    @Test
    void chargerStatusTooltipShowsChargingSlotPurpose() {
        assertTranslationKey("gui.neoopencomputers.charger.status", ChargerScreen.statusLabel());

        final List<Component> tooltip = ChargerScreen.statusTooltip();

        assertEquals(2, tooltip.size());
        assertTranslationKey("gui.neoopencomputers.charger.status", tooltip.get(0));
        assertTranslationKey("gui.neoopencomputers.charger.slot", tooltip.get(1));
    }

    private static void assertTranslationKey(final String expected, final Component component) {
        assertTrue(component.getContents() instanceof TranslatableContents);
        assertEquals(expected, ((TranslatableContents) component.getContents()).getKey());
    }
}
