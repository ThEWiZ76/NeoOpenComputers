package li.cil.oc.common.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RackMenuShapeTest {
    @Test
    void rackMenuHasClientAndServerConstructors() throws NoSuchMethodException {
        final Constructor<RackMenu> clientConstructor = RackMenu.class.getConstructor(int.class, Inventory.class);
        final Constructor<RackMenu> serverConstructor = RackMenu.class.getConstructor(int.class, Inventory.class, Container.class);

        assertTrue(AbstractContainerMenu.class.isAssignableFrom(RackMenu.class));
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class}, clientConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, Container.class}, serverConstructor.getParameterTypes());
    }

    @Test
    void rackMenuSlotCountsAreStable() {
        assertEquals(4, RackMenu.RACK_SLOT_COUNT);
        assertEquals(36, RackMenu.PLAYER_SLOT_COUNT);
        assertEquals(40, RackMenu.TOTAL_SLOT_COUNT);
    }
}
