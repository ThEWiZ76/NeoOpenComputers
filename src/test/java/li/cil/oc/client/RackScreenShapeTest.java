package li.cil.oc.client;

import li.cil.oc.common.menu.RackMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RackScreenShapeTest {
    @Test
    void rackScreenHasMenuConstructor() throws NoSuchMethodException {
        final Constructor<RackScreen> constructor = RackScreen.class.getConstructor(
            RackMenu.class,
            Inventory.class,
            Component.class);

        assertTrue(AbstractContainerScreen.class.isAssignableFrom(RackScreen.class));
        assertArrayEquals(new Class<?>[]{RackMenu.class, Inventory.class, Component.class}, constructor.getParameterTypes());
    }
}
