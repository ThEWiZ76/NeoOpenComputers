package li.cil.oc.common.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RelayMenuShapeTest {
    @Test
    void relayMenuHasClientAndServerConstructors() throws NoSuchMethodException {
        final Constructor<RelayMenu> clientConstructor = RelayMenu.class.getConstructor(int.class, Inventory.class);
        final Constructor<RelayMenu> serverConstructor = RelayMenu.class.getConstructor(int.class, Inventory.class, Container.class);

        assertTrue(AbstractContainerMenu.class.isAssignableFrom(RelayMenu.class));
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class}, clientConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, Container.class}, serverConstructor.getParameterTypes());
    }

    @Test
    void relayMenuSlotCountsAreStable() {
        assertEquals(4, RelayMenu.RELAY_SLOT_COUNT);
        assertEquals(36, RelayMenu.PLAYER_SLOT_COUNT);
        assertEquals(40, RelayMenu.TOTAL_SLOT_COUNT);
    }
}
