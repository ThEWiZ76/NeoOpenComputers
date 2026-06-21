package li.cil.oc.common.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
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
        final Constructor<RackMenu> dataConstructor = RackMenu.class.getConstructor(int.class, Inventory.class, Container.class, ContainerData.class);

        assertTrue(AbstractContainerMenu.class.isAssignableFrom(RackMenu.class));
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class}, clientConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, Container.class}, serverConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, Container.class, ContainerData.class}, dataConstructor.getParameterTypes());
    }

    @Test
    void rackMenuSlotCountsAreStable() {
        assertEquals(4, RackMenu.RACK_SLOT_COUNT);
        assertEquals(36, RackMenu.PLAYER_SLOT_COUNT);
        assertEquals(40, RackMenu.TOTAL_SLOT_COUNT);
        assertEquals(4, RackMenu.RACK_STATE_COUNT);
        assertEquals(0, RackMenu.STATE_EMPTY);
        assertEquals(1, RackMenu.STATE_READY);
        assertEquals(2, RackMenu.STATE_RUNNING);
        assertEquals(3, RackMenu.STATE_INCOMPLETE);
    }

    @Test
    void rackMenuExposesServerRackInventoryTarget() throws NoSuchMethodException {
        assertEquals(Container.class, RackMenu.class.getMethod("rackInventory").getReturnType());
    }

    @Test
    void rackMenuExposesRackStateLookup() throws NoSuchMethodException {
        assertEquals(int.class, RackMenu.class.getMethod("rackState", int.class).getReturnType());
        assertEquals(int.class, RackMenu.class.getMethod("rackStateFor", Container.class, int.class).getReturnType());
    }
}
