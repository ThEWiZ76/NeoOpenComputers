package li.cil.oc.common.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ServerRackMenuShapeTest {
    @Test
    void serverRackMenuHasClientAndServerConstructors() throws NoSuchMethodException {
        final Constructor<ServerRackMenu> clientConstructor = ServerRackMenu.class.getConstructor(int.class, Inventory.class);
        final Constructor<ServerRackMenu> serverConstructor = ServerRackMenu.class.getConstructor(int.class, Inventory.class, Container.class);

        assertTrue(AbstractContainerMenu.class.isAssignableFrom(ServerRackMenu.class));
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class}, clientConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, Container.class}, serverConstructor.getParameterTypes());
    }

    @Test
    void serverRackMenuSlotCountsAreStable() {
        assertEquals(17, ServerRackMenu.SERVER_SLOT_COUNT);
        assertEquals(36, ServerRackMenu.PLAYER_SLOT_COUNT);
        assertEquals(53, ServerRackMenu.TOTAL_SLOT_COUNT);
    }

    @Test
    void serverRackMenuExposesServerInventoryTarget() throws NoSuchMethodException {
        assertEquals(Container.class, ServerRackMenu.class.getMethod("serverInventory").getReturnType());
    }
}
