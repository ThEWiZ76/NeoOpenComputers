package li.cil.oc.common.menu;

import li.cil.oc.common.blockentity.AdapterBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AdapterMenuShapeTest {
    @Test
    void adapterMenuHasClientAndServerConstructors() throws NoSuchMethodException {
        final Constructor<AdapterMenu> clientConstructor = AdapterMenu.class.getConstructor(int.class, Inventory.class);
        final Constructor<AdapterMenu> serverConstructor = AdapterMenu.class.getConstructor(int.class, Inventory.class, Container.class);

        assertTrue(AbstractContainerMenu.class.isAssignableFrom(AdapterMenu.class));
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class}, clientConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, Container.class}, serverConstructor.getParameterTypes());
    }

    @Test
    void adapterMenuSlotCountsAndPositionMatchUpstream() {
        assertEquals(AdapterBlockEntity.CONTAINER_SIZE, AdapterMenu.ADAPTER_SLOT_COUNT);
        assertEquals(36, AdapterMenu.PLAYER_SLOT_COUNT);
        assertEquals(37, AdapterMenu.TOTAL_SLOT_COUNT);
        assertEquals(80, AdapterMenu.adapterSlotX());
        assertEquals(35, AdapterMenu.adapterSlotY());
    }
}
