package li.cil.oc.client;

import li.cil.oc.common.menu.TerminalMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TerminalScreenShapeTest {
    @Test
    void terminalScreenHasMenuConstructor() throws NoSuchMethodException {
        final Constructor<TerminalScreen> constructor = TerminalScreen.class.getConstructor(
            TerminalMenu.class,
            Inventory.class,
            Component.class);

        assertTrue(AbstractContainerScreen.class.isAssignableFrom(TerminalScreen.class));
        assertArrayEquals(new Class<?>[]{TerminalMenu.class, Inventory.class, Component.class}, constructor.getParameterTypes());
    }
}
