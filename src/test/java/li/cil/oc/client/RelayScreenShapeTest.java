package li.cil.oc.client;

import li.cil.oc.common.menu.RelayMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RelayScreenShapeTest {
    @Test
    void relayScreenHasMenuConstructor() throws NoSuchMethodException {
        final Constructor<RelayScreen> constructor = RelayScreen.class.getConstructor(
            RelayMenu.class,
            Inventory.class,
            Component.class);

        assertTrue(AbstractContainerScreen.class.isAssignableFrom(RelayScreen.class));
        assertArrayEquals(new Class<?>[]{RelayMenu.class, Inventory.class, Component.class}, constructor.getParameterTypes());
    }
}
