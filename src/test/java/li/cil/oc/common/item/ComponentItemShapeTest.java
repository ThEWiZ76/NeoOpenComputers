package li.cil.oc.common.item;

import li.cil.oc.api.driver.item.Memory;
import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.driver.DriverItem;
import net.minecraft.world.item.Item;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ComponentItemShapeTest {
    @Test
    void cpuItemIsProcessorDriver() throws NoSuchMethodException {
        final Constructor<CpuItem> constructor = CpuItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(CpuItem.class));
        assertTrue(Processor.class.isAssignableFrom(CpuItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void memoryItemIsMemoryDriver() throws NoSuchMethodException {
        final Constructor<MemoryItem> constructor = MemoryItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(MemoryItem.class));
        assertTrue(Memory.class.isAssignableFrom(MemoryItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void hardDiskDriveItemIsStorageDriver() throws NoSuchMethodException {
        final Constructor<HardDiskDriveItem> constructor = HardDiskDriveItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(HardDiskDriveItem.class));
        assertTrue(DriverItem.class.isAssignableFrom(HardDiskDriveItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void eepromItemIsItemDriver() throws NoSuchMethodException {
        final Constructor<EepromItem> constructor = EepromItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(EepromItem.class));
        assertTrue(DriverItem.class.isAssignableFrom(EepromItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void graphicsCardItemIsCardDriver() throws NoSuchMethodException {
        final Constructor<GraphicsCardItem> constructor = GraphicsCardItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(GraphicsCardItem.class));
        assertTrue(DriverItem.class.isAssignableFrom(GraphicsCardItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }
}
