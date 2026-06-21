package li.cil.oc.client;

import li.cil.oc.common.menu.RaidMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RaidScreenShapeTest {
    @Test
    void raidScreenHasMenuConstructor() throws NoSuchMethodException {
        final Constructor<RaidScreen> constructor = RaidScreen.class.getConstructor(
            RaidMenu.class,
            Inventory.class,
            Component.class);

        assertTrue(AbstractContainerScreen.class.isAssignableFrom(RaidScreen.class));
        assertArrayEquals(new Class<?>[]{RaidMenu.class, Inventory.class, Component.class}, constructor.getParameterTypes());
    }
}
