package li.cil.oc.common.item;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.item.Chargeable;
import li.cil.oc.api.driver.item.Container;
import li.cil.oc.api.driver.item.Inventory;
import li.cil.oc.api.driver.item.Memory;
import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.internal.Tiered;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
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
    void hardDiskDrivePersistsFilesystemDataInStack() throws Exception {
        OpenComputersApi.initialize();
        CompoundTag stackData = new CompoundTag();
        ManagedEnvironment first = HardDiskDriveItem.createEnvironment(stackData, saved -> stackData.put("disk", saved.copy()), null);
        Component firstComponent = assertInstanceOf(Component.class, first.node());
        Object handle = firstComponent.invoke("open", null, "boot.txt", "w")[0];
        firstComponent.invoke("write", null, handle, "ready".getBytes(StandardCharsets.UTF_8));
        firstComponent.invoke("close", null, handle);

        first.save(new CompoundTag());
        ManagedEnvironment second = HardDiskDriveItem.createEnvironment(stackData.getCompound("disk"), saved -> {}, null);
        Component secondComponent = assertInstanceOf(Component.class, second.node());
        Object readHandle = secondComponent.invoke("open", null, "boot.txt", "r")[0];
        byte[] data = (byte[]) secondComponent.invoke("read", null, readHandle, 16)[0];

        assertEquals("ready", new String(data, StandardCharsets.UTF_8));
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
    void cardContainerItemIsContainerDriver() throws NoSuchMethodException {
        final Constructor<CardContainerItem> constructor = CardContainerItem.class.getConstructor(Item.Properties.class, int.class);

        assertTrue(Item.class.isAssignableFrom(CardContainerItem.class));
        assertTrue(Container.class.isAssignableFrom(CardContainerItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class, int.class}, constructor.getParameterTypes());
    }

    @Test
    void upgradeContainerItemIsContainerDriver() throws NoSuchMethodException {
        final Constructor<UpgradeContainerItem> constructor = UpgradeContainerItem.class.getConstructor(Item.Properties.class, int.class);

        assertTrue(Item.class.isAssignableFrom(UpgradeContainerItem.class));
        assertTrue(Container.class.isAssignableFrom(UpgradeContainerItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class, int.class}, constructor.getParameterTypes());
    }

    @Test
    void tabletCaseItemIsTieredAssemblyInput() throws NoSuchMethodException {
        final Constructor<TabletCaseItem> constructor = TabletCaseItem.class.getConstructor(Item.Properties.class, int.class);

        assertTrue(Item.class.isAssignableFrom(TabletCaseItem.class));
        assertTrue(Tiered.class.isAssignableFrom(TabletCaseItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class, int.class}, constructor.getParameterTypes());
    }

    @Test
    void tabletItemIsChargeableAssemblyOutput() throws NoSuchMethodException {
        final Constructor<TabletItem> constructor = TabletItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(TabletItem.class));
        assertTrue(Chargeable.class.isAssignableFrom(TabletItem.class));
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
    void hoverUpgradeItemIsHostAwareUpgradeDriver() throws NoSuchMethodException {
        final Constructor<HoverUpgradeItem> constructor = HoverUpgradeItem.class.getConstructor(Item.Properties.class, int.class);

        assertTrue(Item.class.isAssignableFrom(HoverUpgradeItem.class));
        assertTrue(HostAware.class.isAssignableFrom(HoverUpgradeItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class, int.class}, constructor.getParameterTypes());
    }

    @Test
    void internetCardItemIsCardDriver() throws NoSuchMethodException {
        final Constructor<InternetCardItem> constructor = InternetCardItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(InternetCardItem.class));
        assertTrue(DriverItem.class.isAssignableFrom(InternetCardItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void networkCardItemIsHostAwareCardDriver() throws NoSuchMethodException {
        final Constructor<NetworkCardItem> constructor = NetworkCardItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(NetworkCardItem.class));
        assertTrue(HostAware.class.isAssignableFrom(NetworkCardItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void wirelessNetworkCardItemIsHostAwareCardDriver() throws NoSuchMethodException {
        final Constructor<WirelessNetworkCardItem> constructor = WirelessNetworkCardItem.class.getConstructor(Item.Properties.class, int.class);

        assertTrue(Item.class.isAssignableFrom(WirelessNetworkCardItem.class));
        assertTrue(HostAware.class.isAssignableFrom(WirelessNetworkCardItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class, int.class}, constructor.getParameterTypes());
    }

    @Test
    void linkedCardItemIsHostAwareCardDriver() throws NoSuchMethodException {
        final Constructor<LinkedCardItem> constructor = LinkedCardItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(LinkedCardItem.class));
        assertTrue(HostAware.class.isAssignableFrom(LinkedCardItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void navigationUpgradeItemIsHostAwareUpgradeDriver() throws NoSuchMethodException {
        final Constructor<NavigationUpgradeItem> constructor = NavigationUpgradeItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(NavigationUpgradeItem.class));
        assertTrue(HostAware.class.isAssignableFrom(NavigationUpgradeItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void batteryUpgradeItemIsChargeableHostAwareUpgradeDriver() throws NoSuchMethodException {
        final Constructor<BatteryUpgradeItem> constructor = BatteryUpgradeItem.class.getConstructor(Item.Properties.class, int.class);

        assertTrue(Item.class.isAssignableFrom(BatteryUpgradeItem.class));
        assertTrue(HostAware.class.isAssignableFrom(BatteryUpgradeItem.class));
        assertTrue(Chargeable.class.isAssignableFrom(BatteryUpgradeItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class, int.class}, constructor.getParameterTypes());
    }

    @Test
    void inventoryUpgradeItemIsInventoryHostAwareUpgradeDriver() throws NoSuchMethodException {
        final Constructor<InventoryUpgradeItem> constructor = InventoryUpgradeItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(InventoryUpgradeItem.class));
        assertTrue(HostAware.class.isAssignableFrom(InventoryUpgradeItem.class));
        assertTrue(Inventory.class.isAssignableFrom(InventoryUpgradeItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void tankUpgradeItemIsHostAwareUpgradeDriver() throws NoSuchMethodException {
        final Constructor<TankUpgradeItem> constructor = TankUpgradeItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(TankUpgradeItem.class));
        assertTrue(HostAware.class.isAssignableFrom(TankUpgradeItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void solarGeneratorUpgradeItemIsHostAwareUpgradeDriver() throws NoSuchMethodException {
        final Constructor<SolarGeneratorUpgradeItem> constructor = SolarGeneratorUpgradeItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(SolarGeneratorUpgradeItem.class));
        assertTrue(HostAware.class.isAssignableFrom(SolarGeneratorUpgradeItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void databaseUpgradeItemIsUpgradeDriver() throws NoSuchMethodException {
        final Constructor<DatabaseUpgradeItem> constructor = DatabaseUpgradeItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(DatabaseUpgradeItem.class));
        assertTrue(DriverItem.class.isAssignableFrom(DatabaseUpgradeItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void dataCardItemIsCardDriver() throws NoSuchMethodException {
        final Constructor<DataCardItem> constructor = DataCardItem.class.getConstructor(Item.Properties.class, int.class);

        assertTrue(Item.class.isAssignableFrom(DataCardItem.class));
        assertTrue(DriverItem.class.isAssignableFrom(DataCardItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class, int.class}, constructor.getParameterTypes());
    }
}
