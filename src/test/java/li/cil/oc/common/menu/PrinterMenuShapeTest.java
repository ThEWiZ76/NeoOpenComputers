package li.cil.oc.common.menu;

import li.cil.oc.common.blockentity.PrinterBlockEntity;
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

final class PrinterMenuShapeTest {
    @Test
    void printerMenuHasClientAndServerConstructors() throws NoSuchMethodException {
        final Constructor<PrinterMenu> clientConstructor = PrinterMenu.class.getConstructor(int.class, Inventory.class);
        final Constructor<PrinterMenu> serverConstructor = PrinterMenu.class.getConstructor(int.class, Inventory.class, Container.class);
        final Constructor<PrinterMenu> dataConstructor = PrinterMenu.class.getConstructor(int.class, Inventory.class, Container.class, ContainerData.class);

        assertTrue(AbstractContainerMenu.class.isAssignableFrom(PrinterMenu.class));
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class}, clientConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, Container.class}, serverConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, Container.class, ContainerData.class}, dataConstructor.getParameterTypes());
    }

    @Test
    void printerMenuSlotCountsAndPositionsMatchUpstream() {
        assertEquals(PrinterBlockEntity.CONTAINER_SIZE, PrinterMenu.PRINTER_SLOT_COUNT);
        assertEquals(36, PrinterMenu.PLAYER_SLOT_COUNT);
        assertEquals(39, PrinterMenu.TOTAL_SLOT_COUNT);
        assertEquals(3, PrinterMenu.PRINTER_DATA_COUNT);
        assertEquals(18, PrinterMenu.printerSlotX(PrinterBlockEntity.SLOT_MATERIAL));
        assertEquals(19, PrinterMenu.printerSlotY(PrinterBlockEntity.SLOT_MATERIAL));
        assertEquals(18, PrinterMenu.printerSlotX(PrinterBlockEntity.SLOT_INK));
        assertEquals(51, PrinterMenu.printerSlotY(PrinterBlockEntity.SLOT_INK));
        assertEquals(152, PrinterMenu.printerSlotX(PrinterBlockEntity.SLOT_OUTPUT));
        assertEquals(35, PrinterMenu.printerSlotY(PrinterBlockEntity.SLOT_OUTPUT));
    }

    @Test
    void printerMenuExposesProgressAndResourceBars() throws NoSuchMethodException {
        final Method progress = PrinterMenu.class.getMethod("progress");
        final Method amountMaterial = PrinterMenu.class.getMethod("amountMaterial");
        final Method amountInk = PrinterMenu.class.getMethod("amountInk");

        assertEquals(int.class, progress.getReturnType());
        assertEquals(int.class, amountMaterial.getReturnType());
        assertEquals(int.class, amountInk.getReturnType());
    }
}
