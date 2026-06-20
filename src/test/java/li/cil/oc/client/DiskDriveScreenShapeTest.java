package li.cil.oc.client;

import li.cil.oc.common.menu.DiskDriveMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
}
