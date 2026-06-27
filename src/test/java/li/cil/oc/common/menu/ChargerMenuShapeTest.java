package li.cil.oc.common.menu;

import li.cil.oc.common.blockentity.ChargerBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChargerMenuShapeTest {
    @Test
    void chargerMenuHasClientAndServerConstructors() throws NoSuchMethodException {
        final Constructor<ChargerMenu> clientConstructor = ChargerMenu.class.getConstructor(int.class, Inventory.class);
        final Constructor<ChargerMenu> serverConstructor = ChargerMenu.class.getConstructor(int.class, Inventory.class, Container.class);

        assertTrue(AbstractContainerMenu.class.isAssignableFrom(ChargerMenu.class));
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class}, clientConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, Container.class}, serverConstructor.getParameterTypes());
    }

    @Test
    void chargerMenuSlotCountsAreStable() {
        assertEquals(ChargerBlockEntity.CONTAINER_SIZE, ChargerMenu.CHARGER_SLOT_COUNT);
        assertEquals(36, ChargerMenu.PLAYER_SLOT_COUNT);
        assertEquals(37, ChargerMenu.TOTAL_SLOT_COUNT);
    }
}
