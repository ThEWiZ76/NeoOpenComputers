package li.cil.oc.common.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DiskDriveMenuShapeTest {
    @Test
    void diskDriveMenuHasClientAndServerConstructors() throws NoSuchMethodException {
        final Constructor<DiskDriveMenu> clientConstructor = DiskDriveMenu.class.getConstructor(int.class, Inventory.class);
        final Constructor<DiskDriveMenu> serverConstructor = DiskDriveMenu.class.getConstructor(int.class, Inventory.class, Container.class);

        assertTrue(AbstractContainerMenu.class.isAssignableFrom(DiskDriveMenu.class));
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class}, clientConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, Container.class}, serverConstructor.getParameterTypes());
    }

    @Test
    void diskDriveMenuSlotCountsAreStable() {
        assertEquals(1, DiskDriveMenu.DRIVE_SLOT_COUNT);
        assertEquals(36, DiskDriveMenu.PLAYER_SLOT_COUNT);
        assertEquals(37, DiskDriveMenu.TOTAL_SLOT_COUNT);
    }

    @Test
    void diskDriveMenuDoesNotCarryPortOnlyMediaStatusData() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/menu/DiskDriveMenu.java"));

        assertTrue(!source.contains("ContainerData"));
        assertTrue(!source.contains("mediaState"));
        assertTrue(!source.contains("STATE_LOADED"));
    }

    @Test
    void diskDriveMenuSlotUsesDiskInventoryPlacementRules() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/menu/DiskDriveMenu.java"));

        assertTrue(source.contains("boolean mayPlace"), "Drive slot should validate floppy placement server-side");
        assertTrue(source.contains("diskInventory.canPlaceItem(DiskDriveBlockEntity.SLOT_FLOPPY, stack)"), "Drive slot should delegate to disk drive inventory rules");
        assertTrue(source.contains("int getMaxStackSize"), "Drive slot should keep media stack size at one");
        assertTrue(source.contains("diskInventory.getMaxStackSize()"), "Drive slot should use disk drive max stack size");
    }
}
