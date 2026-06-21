package li.cil.oc.client;

import li.cil.oc.common.menu.DiskDriveMenu;
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

final class DiskDriveScreenShapeTest {
    @Test
    void diskDriveScreenHasMenuConstructor() throws NoSuchMethodException {
        final Constructor<DiskDriveScreen> constructor = DiskDriveScreen.class.getConstructor(
            DiskDriveMenu.class,
            Inventory.class,
            Component.class);

        assertTrue(AbstractContainerScreen.class.isAssignableFrom(DiskDriveScreen.class));
        assertArrayEquals(new Class<?>[]{DiskDriveMenu.class, Inventory.class, Component.class}, constructor.getParameterTypes());
    }

    @Test
    void diskDriveScreenExposesStatusLabels() throws NoSuchMethodException {
        final Method statusLabel = DiskDriveScreen.class.getMethod("statusLabel", int.class);
        final Method statusTooltip = DiskDriveScreen.class.getMethod("statusTooltip", int.class);

        assertEquals(Component.class, statusLabel.getReturnType());
        assertEquals(List.class, statusTooltip.getReturnType());
    }

    @Test
    void diskDriveStatusTooltipShowsMediaState() {
        assertTranslationKey("gui.neoopencomputers.disk_drive.state.empty", DiskDriveScreen.statusLabel(DiskDriveMenu.STATE_EMPTY));
        assertTranslationKey("gui.neoopencomputers.disk_drive.state.loaded", DiskDriveScreen.statusLabel(DiskDriveMenu.STATE_LOADED));

        final List<Component> tooltip = DiskDriveScreen.statusTooltip(DiskDriveMenu.STATE_LOADED);

        assertEquals(2, tooltip.size());
        assertTranslationKey("gui.neoopencomputers.disk_drive.status", tooltip.get(0));
        assertTranslationKey("gui.neoopencomputers.disk_drive.state.loaded", tooltip.get(1));
    }

    private static void assertTranslationKey(final String expected, final Component component) {
        assertTrue(component.getContents() instanceof TranslatableContents);
        assertEquals(expected, ((TranslatableContents) component.getContents()).getKey());
    }
}
