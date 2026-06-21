package li.cil.oc.common.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RaidMenuShapeTest {
    @Test
    void raidMenuHasClientAndServerConstructors() throws NoSuchMethodException {
        final Constructor<RaidMenu> clientConstructor = RaidMenu.class.getConstructor(int.class, Inventory.class);
        final Constructor<RaidMenu> serverConstructor = RaidMenu.class.getConstructor(int.class, Inventory.class, Container.class);

        assertTrue(AbstractContainerMenu.class.isAssignableFrom(RaidMenu.class));
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class}, clientConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, Container.class}, serverConstructor.getParameterTypes());
    }

    @Test
    void raidMenuSlotCountsAreStable() {
        assertEquals(3, RaidMenu.RAID_SLOT_COUNT);
        assertEquals(36, RaidMenu.PLAYER_SLOT_COUNT);
        assertEquals(39, RaidMenu.TOTAL_SLOT_COUNT);
    }
}
