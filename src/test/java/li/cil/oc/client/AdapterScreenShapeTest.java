package li.cil.oc.client;

import li.cil.oc.common.menu.AdapterMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AdapterScreenShapeTest {
    @Test
    void adapterScreenHasMenuConstructor() throws NoSuchMethodException {
        final Constructor<AdapterScreen> constructor = AdapterScreen.class.getConstructor(
            AdapterMenu.class,
            Inventory.class,
            Component.class);

        assertTrue(AbstractContainerScreen.class.isAssignableFrom(AdapterScreen.class));
        assertArrayEquals(new Class<?>[]{AdapterMenu.class, Inventory.class, Component.class}, constructor.getParameterTypes());
    }
}
