package li.cil.oc.client;

import li.cil.oc.common.menu.ComputerCaseMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
}
