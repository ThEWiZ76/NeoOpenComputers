package li.cil.oc.client;

import li.cil.oc.common.menu.ServerRackMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ServerRackScreenShapeTest {
    @Test
    void serverRackScreenHasMenuConstructor() throws NoSuchMethodException {
        final Constructor<ServerRackScreen> constructor = ServerRackScreen.class.getConstructor(
            ServerRackMenu.class,
            Inventory.class,
            Component.class);

        assertTrue(AbstractContainerScreen.class.isAssignableFrom(ServerRackScreen.class));
        assertArrayEquals(new Class<?>[]{ServerRackMenu.class, Inventory.class, Component.class}, constructor.getParameterTypes());
    }
}
