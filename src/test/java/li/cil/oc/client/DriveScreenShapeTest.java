package li.cil.oc.client;

import li.cil.oc.common.menu.DriveMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Inventory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DriveScreenShapeTest {
    @Test
    void driveScreenHasMenuConstructor() throws NoSuchMethodException {
        final Constructor<DriveScreen> constructor = DriveScreen.class.getConstructor(
            DriveMenu.class,
            Inventory.class,
            Component.class);

        assertTrue(AbstractContainerScreen.class.isAssignableFrom(DriveScreen.class));
        assertArrayEquals(new Class<?>[]{DriveMenu.class, Inventory.class, Component.class}, constructor.getParameterTypes());
    }

    @Test
    void driveScreenExposesModeLabels() {
        assertTranslationKey("gui.neoopencomputers.drive.managed", DriveScreen.modeLabel(false));
        assertTranslationKey("gui.neoopencomputers.drive.unmanaged", DriveScreen.modeLabel(true));
    }

    private static void assertTranslationKey(final String expected, final Component component) {
        assertTrue(component.getContents() instanceof TranslatableContents);
        assertEquals(expected, ((TranslatableContents) component.getContents()).getKey());
    }
}
