package li.cil.oc.common.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ComputerCaseMenuShapeTest {
    @Test
    void computerCaseMenuHasClientAndServerConstructors() throws NoSuchMethodException {
        final Constructor<ComputerCaseMenu> clientConstructor = ComputerCaseMenu.class.getConstructor(int.class, Inventory.class);
        final Constructor<ComputerCaseMenu> serverConstructor = ComputerCaseMenu.class.getConstructor(int.class, Inventory.class, Container.class);

        assertTrue(AbstractContainerMenu.class.isAssignableFrom(ComputerCaseMenu.class));
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class}, clientConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, Container.class}, serverConstructor.getParameterTypes());
    }

    @Test
    void computerCaseMenuSlotCountsAreStable() {
        assertEquals(7, ComputerCaseMenu.COMPUTER_SLOT_COUNT);
        assertEquals(36, ComputerCaseMenu.PLAYER_SLOT_COUNT);
        assertEquals(43, ComputerCaseMenu.TOTAL_SLOT_COUNT);
    }
}
