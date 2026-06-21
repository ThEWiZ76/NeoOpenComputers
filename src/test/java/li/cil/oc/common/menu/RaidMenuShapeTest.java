package li.cil.oc.common.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RaidMenuShapeTest {
    @Test
    void raidMenuHasClientAndServerConstructors() throws NoSuchMethodException {
        final Constructor<RaidMenu> clientConstructor = RaidMenu.class.getConstructor(int.class, Inventory.class);
        final Constructor<RaidMenu> serverConstructor = RaidMenu.class.getConstructor(int.class, Inventory.class, Container.class);
        final Constructor<RaidMenu> dataConstructor = RaidMenu.class.getConstructor(int.class, Inventory.class, Container.class, ContainerData.class);

        assertTrue(AbstractContainerMenu.class.isAssignableFrom(RaidMenu.class));
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class}, clientConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, Container.class}, serverConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, Container.class, ContainerData.class}, dataConstructor.getParameterTypes());
    }

    @Test
    void raidMenuSlotCountsAreStable() {
        assertEquals(3, RaidMenu.RAID_SLOT_COUNT);
        assertEquals(36, RaidMenu.PLAYER_SLOT_COUNT);
        assertEquals(39, RaidMenu.TOTAL_SLOT_COUNT);
        assertEquals(0, RaidMenu.RAID_STATUS_INDEX);
        assertEquals(1, RaidMenu.RAID_CAPACITY_INDEX);
        assertEquals(2, RaidMenu.RAID_DATA_COUNT);
        assertEquals(0, RaidMenu.STATE_EMPTY);
        assertEquals(1, RaidMenu.STATE_INCOMPLETE);
        assertEquals(2, RaidMenu.STATE_READY);
    }

    @Test
    void raidMenuExposesStatusAccess() throws NoSuchMethodException {
        final Method state = RaidMenu.class.getMethod("raidState");
        final Method capacity = RaidMenu.class.getMethod("raidCapacity");
        final Method stateFor = RaidMenu.class.getMethod("raidStateFor", Container.class);
        final Method capacityFor = RaidMenu.class.getMethod("raidCapacityFor", Container.class);

        assertEquals(int.class, state.getReturnType());
        assertEquals(int.class, capacity.getReturnType());
        assertEquals(int.class, stateFor.getReturnType());
        assertEquals(int.class, capacityFor.getReturnType());
    }

    @Test
    void nonRaidInventoryReportsEmptyStatus() {
        assertEquals(RaidMenu.STATE_EMPTY, RaidMenu.raidStateFor(null));
        assertEquals(0, RaidMenu.raidCapacityFor(null));
    }
}
