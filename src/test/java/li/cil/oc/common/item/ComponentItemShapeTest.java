package li.cil.oc.common.item;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.item.CallBudget;
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
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
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
        assertTrue(CallBudget.class.isAssignableFrom(CpuItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void cpuItemExposesUpstreamCallBudgets() throws Exception {
        final var budgetMethod = CpuItem.class.getDeclaredMethod("callBudget", int.class);

        assertEquals(0.5D, budgetMethod.invoke(null, 0));
        assertEquals(1.0D, budgetMethod.invoke(null, 1));
        assertEquals(1.5D, budgetMethod.invoke(null, 2));
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
    void apuItemIsProcessorDriver() throws NoSuchMethodException {
        final Constructor<ApuItem> constructor = ApuItem.class.getConstructor(Item.Properties.class, int.class);

        assertTrue(Item.class.isAssignableFrom(ApuItem.class));
        assertTrue(Processor.class.isAssignableFrom(ApuItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class, int.class}, constructor.getParameterTypes());
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
    void inkCartridgeItemAcceptsEmptyCartridgeRemainder() throws NoSuchMethodException {
        final Constructor<InkCartridgeItem> constructor = InkCartridgeItem.class.getConstructor(Item.Properties.class, Item.class);

        assertTrue(Item.class.isAssignableFrom(InkCartridgeItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class, Item.class}, constructor.getParameterTypes());
    }

    @Test
    void wrenchItemIsInternalWrenchTool() throws NoSuchMethodException {
        final Constructor<WrenchItem> constructor = WrenchItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(WrenchItem.class));
        assertTrue(li.cil.oc.api.internal.Wrench.class.isAssignableFrom(WrenchItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void wrenchItemDeclaresSneakBypassHook() throws NoSuchMethodException {
        assertArrayEquals(
            new Class<?>[]{ItemStack.class, LevelReader.class, BlockPos.class, Player.class},
            WrenchItem.class.getDeclaredMethod("doesSneakBypassUse", ItemStack.class, LevelReader.class, BlockPos.class, Player.class).getParameterTypes());
    }

    @Test
    void texturePickerItemIsSimpleToolItem() throws NoSuchMethodException {
        final Constructor<TexturePickerItem> constructor = TexturePickerItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(TexturePickerItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void terminalItemIsSingleStackToolItem() throws NoSuchMethodException {
        final Constructor<TerminalItem> constructor = TerminalItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(TerminalItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void terminalServerItemIsSimpleComponentItem() throws NoSuchMethodException {
        final Constructor<TerminalServerItem> constructor = TerminalServerItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(TerminalServerItem.class));
        assertTrue(li.cil.oc.api.driver.DriverItem.class.isAssignableFrom(TerminalServerItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void nanomachinesItemUsesUpstreamConsumptionShape() throws NoSuchMethodException {
        final Constructor<NanomachinesItem> constructor = NanomachinesItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(NanomachinesItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
        assertArrayEquals(
            new Class<?>[]{ItemStack.class, net.minecraft.world.entity.LivingEntity.class},
            NanomachinesItem.class.getDeclaredMethod("getUseDuration", ItemStack.class, net.minecraft.world.entity.LivingEntity.class).getParameterTypes());
    }

    @Test
    void serverItemIsSingleStackTieredRackMountable() throws NoSuchMethodException {
        final Constructor<ServerItem> constructor = ServerItem.class.getConstructor(Item.Properties.class, int.class);

        assertTrue(Item.class.isAssignableFrom(ServerItem.class));
        assertTrue(Tiered.class.isAssignableFrom(ServerItem.class));
        assertTrue(li.cil.oc.api.driver.DriverItem.class.isAssignableFrom(ServerItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class, int.class}, constructor.getParameterTypes());
        assertArrayEquals(new Class<?>[0], ServerItem.class.getDeclaredMethod("tier").getParameterTypes());
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
    void tankControllerUpgradeItemIsHostAwareUpgradeDriver() throws NoSuchMethodException {
        final Constructor<TankControllerUpgradeItem> constructor = TankControllerUpgradeItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(TankControllerUpgradeItem.class));
        assertTrue(HostAware.class.isAssignableFrom(TankControllerUpgradeItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void tractorBeamUpgradeItemIsHostAwareUpgradeDriver() throws NoSuchMethodException {
        final Constructor<TractorBeamUpgradeItem> constructor = TractorBeamUpgradeItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(TractorBeamUpgradeItem.class));
        assertTrue(HostAware.class.isAssignableFrom(TractorBeamUpgradeItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void leashUpgradeItemIsHostAwareUpgradeDriver() throws NoSuchMethodException {
        final Constructor<LeashUpgradeItem> constructor = LeashUpgradeItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(LeashUpgradeItem.class));
        assertTrue(HostAware.class.isAssignableFrom(LeashUpgradeItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void angelUpgradeItemIsHostAwareUpgradeDriver() throws NoSuchMethodException {
        final Constructor<AngelUpgradeItem> constructor = AngelUpgradeItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(AngelUpgradeItem.class));
        assertTrue(HostAware.class.isAssignableFrom(AngelUpgradeItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void chunkloaderUpgradeItemIsHostAwareUpgradeDriver() throws NoSuchMethodException {
        final Constructor<ChunkloaderUpgradeItem> constructor = ChunkloaderUpgradeItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(ChunkloaderUpgradeItem.class));
        assertTrue(HostAware.class.isAssignableFrom(ChunkloaderUpgradeItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void mfuItemIsHostAwareUpgradeDriver() throws NoSuchMethodException {
        final Constructor<MfuItem> constructor = MfuItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(MfuItem.class));
        assertTrue(HostAware.class.isAssignableFrom(MfuItem.class));
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
    void databaseUpgradeItemIsHostAwareUpgradeDriver() throws NoSuchMethodException {
        final Constructor<DatabaseUpgradeItem> constructor = DatabaseUpgradeItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(DatabaseUpgradeItem.class));
        assertTrue(DriverItem.class.isAssignableFrom(DatabaseUpgradeItem.class));
        assertTrue(HostAware.class.isAssignableFrom(DatabaseUpgradeItem.class));
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
