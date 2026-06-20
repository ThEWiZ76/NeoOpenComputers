package li.cil.oc.common.item;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.item.Memory;
import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.world.item.Item;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    void cpuItemCreatesDeviceInfoEnvironment() {
        OpenComputersApi.initialize();

        ManagedEnvironment environment = CpuItem.createDeviceInfoEnvironment(0);

        assertNotNull(environment);
        assertNotNull(environment.node());
        DeviceInfo info = assertInstanceOf(DeviceInfo.class, environment);
        Map<String, String> metadata = info.getDeviceInfo();
        assertEquals(DeviceInfo.DeviceClass.Processor, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("CPU", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("FlexiArch 1 Processor", metadata.get(DeviceInfo.DeviceAttribute.Product));
        assertEquals("500", metadata.get(DeviceInfo.DeviceAttribute.Clock));
    }

    @Test
    void memoryItemIsMemoryDriver() throws NoSuchMethodException {
        final Constructor<MemoryItem> constructor = MemoryItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(MemoryItem.class));
        assertTrue(Memory.class.isAssignableFrom(MemoryItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void memoryItemCreatesDeviceInfoEnvironment() {
        OpenComputersApi.initialize();

        ManagedEnvironment environment = MemoryItem.createDeviceInfoEnvironment(0);

        assertNotNull(environment);
        assertNotNull(environment.node());
        DeviceInfo info = assertInstanceOf(DeviceInfo.class, environment);
        Map<String, String> metadata = info.getDeviceInfo();
        assertEquals(DeviceInfo.DeviceClass.Memory, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Memory bank", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("Multipurpose RAM Type", metadata.get(DeviceInfo.DeviceAttribute.Product));
        assertEquals("500", metadata.get(DeviceInfo.DeviceAttribute.Clock));
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
    void floppyItemIsItemDriver() throws NoSuchMethodException {
        final Constructor<FloppyItem> constructor = FloppyItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(FloppyItem.class));
        assertTrue(DriverItem.class.isAssignableFrom(FloppyItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void graphicsCardItemIsCardDriver() throws NoSuchMethodException {
        final Constructor<GraphicsCardItem> constructor = GraphicsCardItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(GraphicsCardItem.class));
        assertTrue(DriverItem.class.isAssignableFrom(GraphicsCardItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void networkCardItemIsHostAwareCardDriver() throws NoSuchMethodException {
        final Constructor<NetworkCardItem> constructor = NetworkCardItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(NetworkCardItem.class));
        assertTrue(HostAware.class.isAssignableFrom(NetworkCardItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }
}
