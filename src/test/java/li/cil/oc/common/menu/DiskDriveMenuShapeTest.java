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

final class DiskDriveMenuShapeTest {
    @Test
    void diskDriveMenuHasClientAndServerConstructors() throws NoSuchMethodException {
        final Constructor<DiskDriveMenu> clientConstructor = DiskDriveMenu.class.getConstructor(int.class, Inventory.class);
        final Constructor<DiskDriveMenu> serverConstructor = DiskDriveMenu.class.getConstructor(int.class, Inventory.class, Container.class);
        final Constructor<DiskDriveMenu> dataConstructor = DiskDriveMenu.class.getConstructor(int.class, Inventory.class, Container.class, ContainerData.class);

        assertTrue(AbstractContainerMenu.class.isAssignableFrom(DiskDriveMenu.class));
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class}, clientConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, Container.class}, serverConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, Container.class, ContainerData.class}, dataConstructor.getParameterTypes());
    }

    @Test
    void diskDriveMenuSlotCountsAreStable() {
        assertEquals(1, DiskDriveMenu.DRIVE_SLOT_COUNT);
        assertEquals(36, DiskDriveMenu.PLAYER_SLOT_COUNT);
        assertEquals(37, DiskDriveMenu.TOTAL_SLOT_COUNT);
        assertEquals(1, DiskDriveMenu.DRIVE_DATA_COUNT);
        assertEquals(0, DiskDriveMenu.STATE_EMPTY);
        assertEquals(1, DiskDriveMenu.STATE_LOADED);
    }

    @Test
    void diskDriveMenuExposesMediaState() throws NoSuchMethodException {
        final Method mediaState = DiskDriveMenu.class.getMethod("mediaState");
        final Method mediaStateFor = DiskDriveMenu.class.getMethod("mediaStateFor", Container.class);

        assertEquals(int.class, mediaState.getReturnType());
        assertEquals(int.class, mediaStateFor.getReturnType());
    }

    @Test
    void nonDiskDriveInventoryReportsEmptyState() {
        assertEquals(DiskDriveMenu.STATE_EMPTY, DiskDriveMenu.mediaStateFor(null));
    }
}
