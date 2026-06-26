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
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.common.driver.ScreenItemDriver;
import li.cil.oc.common.DriveEnvironment;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.ModSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ComponentItemShapeTest {
    @Test
    void cpuItemIsProcessorDriver() throws NoSuchMethodException {
        final Constructor<CpuItem> constructor = CpuItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(CpuItem.class));
        assertTrue(Processor.class.isAssignableFrom(CpuItem.class));
        assertTrue(MutableProcessor.class.isAssignableFrom(CpuItem.class));
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
    void cpuItemUsesConfiguredCallBudgets() throws Exception {
        withCachedConfig(ModSettings.CALL_BUDGETS, List.of(0.25D, 0.75D, 2.0D), () -> {
            assertEquals(0.25D, cpu(0).getCallBudget(null));
            assertEquals(0.75D, cpu(1).getCallBudget(null));
            assertEquals(2.0D, cpu(2).getCallBudget(null));
        });
    }

    @Test
    void processorsUseConfiguredComponentCounts() throws Exception {
        withCachedConfig(ModSettings.CPU_COMPONENT_COUNT, List.of(2, 4, 6, 64), () -> {
            assertEquals(2, cpu(0).supportedComponents(null));
            assertEquals(4, cpu(1).supportedComponents(null));
            assertEquals(6, cpu(2).supportedComponents(null));
            assertEquals(2, componentBus(0).supportedComponents(null));
            assertEquals(4, componentBus(1).supportedComponents(null));
            assertEquals(6, componentBus(2).supportedComponents(null));
            assertEquals(64, componentBus(3).supportedComponents(null));
        });
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
        assertEquals("MightyPirates GmbH & Co. KG", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
        assertEquals("FlexiArch 1 Processor", metadata.get(DeviceInfo.DeviceAttribute.Product));
        assertEquals("500", metadata.get(DeviceInfo.DeviceAttribute.Clock));
    }

    @Test
    void memoryItemIsMemoryDriver() throws NoSuchMethodException {
        final Constructor<MemoryItem> constructor = MemoryItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(MemoryItem.class));
        assertTrue(Memory.class.isAssignableFrom(MemoryItem.class));
        assertTrue(CallBudget.class.isAssignableFrom(MemoryItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void memoryItemUsesConfiguredCallBudgets() throws Exception {
        withCachedConfig(ModSettings.CALL_BUDGETS, List.of(0.25D, 0.75D, 2.0D), () -> {
            assertEquals(0.25D, memory(0).getCallBudget(null));
            assertEquals(0.75D, memory(1).getCallBudget(null));
            assertEquals(2.0D, memory(2).getCallBudget(null));
        });
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
        assertEquals("MightyPirates GmbH & Co. KG", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
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
    void diskDriveMountableItemIsHostAwareRackMountableDriver() throws NoSuchMethodException {
        final Constructor<DiskDriveMountableItem> constructor = DiskDriveMountableItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(DiskDriveMountableItem.class));
        assertTrue(DriverItem.class.isAssignableFrom(DiskDriveMountableItem.class));
        assertTrue(HostAware.class.isAssignableFrom(DiskDriveMountableItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void screenItemDriverIsHostAwareLikeUpstreamDriverScreen() throws NoSuchMethodException {
        final Constructor<ScreenItemDriver> constructor = ScreenItemDriver.class.getConstructor(Item.class, int.class);

        assertTrue(DriverItem.class.isAssignableFrom(ScreenItemDriver.class));
        assertTrue(HostAware.class.isAssignableFrom(ScreenItemDriver.class));
        assertArrayEquals(new Class<?>[]{Item.class, int.class}, constructor.getParameterTypes());
    }

    @Test
    void hardDiskDrivePersistsFilesystemDataInStack() throws Exception {
        OpenComputersApi.initialize();
        CompoundTag stackData = new CompoundTag();
        ManagedEnvironment first = HardDiskDriveItem.createEnvironment(stackData, saved -> stackData.put("disk", saved.copy()), null);
        Component firstComponent = assertInstanceOf(Component.class, first.node());
        charge(first, 1D);
        Object handle = firstComponent.invoke("open", null, "boot.txt", "w")[0];
        firstComponent.invoke("write", null, handle, "ready".getBytes(StandardCharsets.UTF_8));
        firstComponent.invoke("close", null, handle);

        first.save(new CompoundTag());
        ManagedEnvironment second = HardDiskDriveItem.createEnvironment(stackData.getCompound("disk"), saved -> {}, null);
        Component secondComponent = assertInstanceOf(Component.class, second.node());
        charge(second, 1D);
        Object readHandle = secondComponent.invoke("open", null, "boot.txt", "r")[0];
        byte[] data = (byte[]) secondComponent.invoke("read", null, readHandle, 16)[0];

        assertEquals("ready", new String(data, StandardCharsets.UTF_8));
    }

    @Test
    void hardDiskDriveCapacityUsesConfiguredHddSizes() throws Exception {
        OpenComputersApi.initialize();

        withCachedConfig(ModSettings.HDD_SIZES, List.of(2, 4, 8), () -> {
            assertHardDiskCapacity(0, 2L * 1024L);
            assertHardDiskCapacity(1, 4L * 1024L);
            assertHardDiskCapacity(2, 8L * 1024L);
        });
    }

    @Test
    void hardDiskDriveUsesUpstreamFilesystemSpeeds() throws Exception {
        OpenComputersApi.initialize();

        assertHardDiskClock(0, "140/140/60");
        assertHardDiskClock(1, "200/200/80");
        assertHardDiskClock(2, "260/260/100");
    }

    @Test
    void hardDiskDriveLabelIsWritableAndPersistsLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        final CompoundTag savedData = new CompoundTag();
        final ManagedEnvironment first = HardDiskDriveItem.createEnvironment(new CompoundTag(), saved -> savedData.put("disk", saved.copy()), null);
        final Component firstComponent = assertInstanceOf(Component.class, first.node());

        assertArrayEquals(new Object[]{"abcdefghijklmnop"}, firstComponent.invoke("setLabel", null, "abcdefghijklmnopq"));
        first.save(new CompoundTag());

        final ManagedEnvironment second = HardDiskDriveItem.createEnvironment(savedData.getCompound("disk"), saved -> {}, null);
        final Component secondComponent = assertInstanceOf(Component.class, second.node());

        assertArrayEquals(new Object[]{"abcdefghijklmnop"}, secondComponent.invoke("getLabel", null));
    }

    @Test
    void driveWithoutLabelReturnsNoLabelResultLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        final ManagedEnvironment environment = new DriveEnvironment(512, 1, null, null, null, 0, null);
        final Component component = assertInstanceOf(Component.class, environment.node());

        assertNull(component.invoke("getLabel", null));
    }

    @Test
    void unmanagedHardDiskDriveExposesRawSectorCallbacksLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        final CompoundTag data = new CompoundTag();
        data.putBoolean("oc:unmanaged", true);

        final ManagedEnvironment environment = HardDiskDriveItem.createEnvironment(1, data, saved -> {}, null);
        final Component component = assertInstanceOf(Component.class, environment.node());

        assertEquals("drive", component.name());
        assertTrue(component.methods().contains("getCapacity"));
        assertTrue(component.methods().contains("getSectorSize"));
        assertTrue(component.methods().contains("getPlatterCount"));
        assertTrue(component.methods().contains("readSector"));
        assertTrue(component.methods().contains("writeSector"));
        assertTrue(component.methods().contains("readByte"));
        assertTrue(component.methods().contains("writeByte"));
        assertArrayEquals(new Object[]{ModSettings.hddSize(1) * 1024}, component.invoke("getCapacity", null));
        assertArrayEquals(new Object[]{512}, component.invoke("getSectorSize", null));
        assertArrayEquals(new Object[]{4}, component.invoke("getPlatterCount", null));
    }

    @Test
    void unmanagedHardDiskDrivePersistsRawSectorDataInStack() throws Exception {
        OpenComputersApi.initialize();
        final CompoundTag data = new CompoundTag();
        data.putBoolean("oc:unmanaged", true);
        final CompoundTag savedData = new CompoundTag();
        final ManagedEnvironment first = HardDiskDriveItem.createEnvironment(0, data, saved -> savedData.put("disk", saved.copy()), null);
        final Component firstComponent = assertInstanceOf(Component.class, first.node());

        assertNull(firstComponent.invoke("writeSector", null, 1, "boot".getBytes(StandardCharsets.UTF_8)));
        assertNull(firstComponent.invoke("writeByte", null, 513, 65));
        first.save(new CompoundTag());

        final ManagedEnvironment second = HardDiskDriveItem.createEnvironment(0, savedData.getCompound("disk"), saved -> {}, null);
        final Component secondComponent = assertInstanceOf(Component.class, second.node());
        final byte[] sector = (byte[]) secondComponent.invoke("readSector", null, 1)[0];

        assertEquals("boot", new String(sector, 0, 4, StandardCharsets.UTF_8));
        assertArrayEquals(new Object[]{65}, secondComponent.invoke("readByte", null, 513));
    }

    @Test
    void lockedUnmanagedHardDiskDriveRejectsWritesLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        final CompoundTag data = new CompoundTag();
        data.putBoolean("oc:unmanaged", true);
        data.putString("oc:lock", "tester");
        final ManagedEnvironment environment = HardDiskDriveItem.createEnvironment(data, saved -> {}, null);
        final Component component = assertInstanceOf(Component.class, environment.node());

        assertThrows(Exception.class, () -> component.invoke("setLabel", null, "locked"));
        assertThrows(Exception.class, () -> component.invoke("writeSector", null, 1, "x".getBytes(StandardCharsets.UTF_8)));
        assertThrows(Exception.class, () -> component.invoke("writeByte", null, 1, 1));
    }

    @Test
    void eepromItemIsItemDriver() throws NoSuchMethodException {
        final Constructor<EepromItem> constructor = EepromItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(EepromItem.class));
        assertTrue(DriverItem.class.isAssignableFrom(EepromItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void eepromItemSneakBypassesBlockUseLikeUpstream() throws Exception {
        assertArrayEquals(
            new Class<?>[]{ItemStack.class, LevelReader.class, BlockPos.class, Player.class},
            EepromItem.class.getDeclaredMethod("doesSneakBypassUse", ItemStack.class, LevelReader.class, BlockPos.class, Player.class).getParameterTypes());
        assertTrue(allocate(EepromItem.class).doesSneakBypassUse(null, null, BlockPos.ZERO, null));
    }

    @Test
    void floppyItemIsItemDriver() throws NoSuchMethodException {
        final Constructor<FloppyItem> constructor = FloppyItem.class.getConstructor(Item.Properties.class);

        assertTrue(Item.class.isAssignableFrom(FloppyItem.class));
        assertTrue(DriverItem.class.isAssignableFrom(FloppyItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void floppyItemSneakBypassesBlockUseLikeUpstream() throws Exception {
        assertArrayEquals(
            new Class<?>[]{ItemStack.class, LevelReader.class, BlockPos.class, Player.class},
            FloppyItem.class.getDeclaredMethod("doesSneakBypassUse", ItemStack.class, LevelReader.class, BlockPos.class, Player.class).getParameterTypes());
        assertTrue(allocate(FloppyItem.class).doesSneakBypassUse(null, null, BlockPos.ZERO, null));
    }

    @Test
    void blankFloppyUsesConfiguredWritableFilesystem() throws Exception {
        OpenComputersApi.initialize();
        CompoundTag savedData = new CompoundTag();

        withCachedConfig(ModSettings.FLOPPY_SIZE, 2, () -> {
            ManagedEnvironment first = FloppyItem.createWritableEnvironment(new CompoundTag(), saved -> savedData.put("disk", saved.copy()), new CompoundTag(), null);
            Component firstComponent = assertInstanceOf(Component.class, first.node());
            charge(first, 1D);
            assertEquals(2L * 1024L, firstComponent.invoke("spaceTotal", null)[0]);
            Object handle = firstComponent.invoke("open", null, "note.txt", "w")[0];
            firstComponent.invoke("write", null, handle, "ok".getBytes(StandardCharsets.UTF_8));
            firstComponent.invoke("close", null, handle);
            first.save(new CompoundTag());

            ManagedEnvironment second = FloppyItem.createWritableEnvironment(savedData.getCompound("disk"), saved -> {}, new CompoundTag(), null);
            Component secondComponent = assertInstanceOf(Component.class, second.node());
            charge(second, 1D);
            Object readHandle = secondComponent.invoke("open", null, "note.txt", "r")[0];
            byte[] data = (byte[]) secondComponent.invoke("read", null, readHandle, 16)[0];

            assertEquals("ok", new String(data, StandardCharsets.UTF_8));
        });
    }

    @Test
    void blankFloppyLabelIsWritableAndPersistsLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        final CompoundTag savedData = new CompoundTag();
        final ManagedEnvironment first = FloppyItem.createWritableEnvironment(new CompoundTag(), saved -> savedData.put("disk", saved.copy()), new CompoundTag(), null);
        final Component firstComponent = assertInstanceOf(Component.class, first.node());

        assertArrayEquals(new Object[]{"abcdefghijklmnop"}, firstComponent.invoke("setLabel", null, "abcdefghijklmnopq"));
        first.save(new CompoundTag());

        final ManagedEnvironment second = FloppyItem.createWritableEnvironment(savedData.getCompound("disk"), saved -> {}, new CompoundTag(), null);
        final Component secondComponent = assertInstanceOf(Component.class, second.node());

        assertArrayEquals(new Object[]{"abcdefghijklmnop"}, secondComponent.invoke("getLabel", null));
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
        assertTrue(HostAware.class.isAssignableFrom(GraphicsCardItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void apuItemIsProcessorDriver() throws NoSuchMethodException {
        final Constructor<ApuItem> constructor = ApuItem.class.getConstructor(Item.Properties.class, int.class);

        assertTrue(Item.class.isAssignableFrom(ApuItem.class));
        assertTrue(Processor.class.isAssignableFrom(ApuItem.class));
        assertTrue(HostAware.class.isAssignableFrom(ApuItem.class));
        assertArrayEquals(new Class<?>[]{Item.Properties.class, int.class}, constructor.getParameterTypes());
    }

    @Test
    void apuItemCreatesUpstreamDeviceInfoEnvironment() throws Exception {
        final ManagedEnvironment environment = allocate(ApuItem.class).createEnvironment(null, null);
        final DeviceInfo info = assertInstanceOf(DeviceInfo.class, environment);
        final Map<String, String> metadata = info.getDeviceInfo();

        assertEquals(DeviceInfo.DeviceClass.Processor, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("APU", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MightyPirates GmbH & Co. KG", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
        assertEquals("FlexiArch 1 Processor (Builtin Graphics)", metadata.get(DeviceInfo.DeviceAttribute.Product));
        assertEquals("800", metadata.get(DeviceInfo.DeviceAttribute.Capacity));
        assertEquals("1", metadata.get(DeviceInfo.DeviceAttribute.Width));
        assertEquals("500+640/640/40/1280/320/640", metadata.get(DeviceInfo.DeviceAttribute.Clock));
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
        assertTrue(HostAware.class.isAssignableFrom(TerminalServerItem.class));
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
        assertTrue(HostAware.class.isAssignableFrom(ServerItem.class));
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
    void leashUpgradeItemCreatesUpstreamLeashComponentEnvironment() throws Exception {
        OpenComputersApi.initialize();
        final LeashUpgradeItem item = allocate(LeashUpgradeItem.class);

        final ManagedEnvironment environment = item.createEnvironment(null, new TestEnvironmentHost());
        assertNotNull(environment);
        final Component component = assertInstanceOf(Component.class, environment.node());
        final DeviceInfo deviceInfo = assertInstanceOf(DeviceInfo.class, environment);
        final Map<String, String> metadata = deviceInfo.getDeviceInfo();

        assertEquals("leash", component.name());
        assertEquals(Visibility.Network, component.visibility());
        assertEquals(DeviceInfo.DeviceClass.Generic, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Leash", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("FlockControl (FC-3LS)", metadata.get(DeviceInfo.DeviceAttribute.Product));
        assertEquals("8", metadata.get(DeviceInfo.DeviceAttribute.Capacity));
    }

    @Test
    void barcodeReaderUpgradeItemCreatesUpstreamComponentEnvironment() throws Exception {
        OpenComputersApi.initialize();
        final Class<?> itemClass = Class.forName("li.cil.oc.common.item.BarcodeReaderUpgradeItem");
        final Item item = (Item) allocate(itemClass.asSubclass(Item.class));

        final ManagedEnvironment environment = ((HostAware) item).createEnvironment(null, new TestEnvironmentHost());
        assertNotNull(environment);
        final Component component = assertInstanceOf(Component.class, environment.node());
        final DeviceInfo deviceInfo = assertInstanceOf(DeviceInfo.class, environment);
        final Map<String, String> metadata = deviceInfo.getDeviceInfo();

        assertEquals("barcode_reader", component.name());
        assertEquals(Visibility.Network, component.visibility());
        assertEquals(DeviceInfo.DeviceClass.Generic, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Barcode reader upgrade", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("Readerizer Deluxe", metadata.get(DeviceInfo.DeviceAttribute.Product));
    }

    @Test
    void debugCardItemCreatesUpstreamDebugComponentEnvironment() throws Exception {
        OpenComputersApi.initialize();
        final Class<?> itemClass = Class.forName("li.cil.oc.common.item.DebugCardItem");
        final Item item = (Item) allocate(itemClass.asSubclass(Item.class));
        final DriverItem driver = assertInstanceOf(DriverItem.class, item);

        final ManagedEnvironment environment = driver.createEnvironment(null, new TestEnvironmentHost());
        assertNotNull(environment);
        final Component component = assertInstanceOf(Component.class, environment.node());

        assertEquals("card", driver.slot(null));
        assertEquals("debug", component.name());
        assertEquals(Visibility.Neighbors, component.visibility());
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

    @Test
    void cardDriversKeepUpstreamClientWorldEnvironmentGuard() throws Exception {
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/AngelUpgradeItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/ApuItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/BatteryUpgradeItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/ChunkloaderUpgradeItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/CraftingUpgradeItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/DataCardItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/DatabaseUpgradeItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/DebugCardItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/EepromItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/FloppyItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/GeneratorUpgradeItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/driver/GeolyzerItemDriver.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/GraphicsCardItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/HardDiskDriveItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/InternetCardItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/InventoryControllerUpgradeItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/LeashUpgradeItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/LinkedCardItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/MfuItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/driver/MotionSensorItemDriver.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/NavigationUpgradeItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/NetworkCardItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/PistonUpgradeItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/RedstoneCardItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/SignUpgradeItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/SolarGeneratorUpgradeItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/StickyPistonUpgradeItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/TankControllerUpgradeItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/TankUpgradeItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/TabletItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/driver/TransposerItemDriver.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/TractorBeamUpgradeItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/TradingUpgradeItem.java");
        assertClientWorldGuard("src/main/java/li/cil/oc/common/item/WirelessNetworkCardItem.java");
    }

    private static void assertClientWorldGuard(final String sourcePath) throws Exception {
        final String source = Files.readString(Path.of(sourcePath));

        assertTrue(source.contains("ItemDriverData.isClientSide(host)"), sourcePath + " does not guard client-world environment creation like upstream");
    }

    private static void assertHardDiskCapacity(final int tier, final long expectedCapacity) throws Exception {
        final ManagedEnvironment environment = HardDiskDriveItem.createEnvironment(tier, new CompoundTag(), saved -> {}, null);
        final Component component = assertInstanceOf(Component.class, environment.node());

        assertEquals(expectedCapacity, component.invoke("spaceTotal", null)[0]);
    }

    private static void assertHardDiskClock(final int tier, final String expectedClock) {
        final ManagedEnvironment environment = HardDiskDriveItem.createEnvironment(tier, new CompoundTag(), saved -> {}, null);
        final DeviceInfo info = assertInstanceOf(DeviceInfo.class, environment);

        assertEquals(expectedClock, info.getDeviceInfo().get(DeviceInfo.DeviceAttribute.Clock));
    }

    private static CpuItem cpu(final int tier) throws Exception {
        final CpuItem item = allocate(CpuItem.class);
        setField(item, "tier", tier);
        return item;
    }

    private static MemoryItem memory(final int tier) throws Exception {
        final MemoryItem item = allocate(MemoryItem.class);
        setField(item, "tier", tier);
        return item;
    }

    private static ComponentBusItem componentBus(final int tier) throws Exception {
        final ComponentBusItem item = allocate(ComponentBusItem.class);
        setField(item, "tier", Math.max(0, Math.min(2, tier)));
        setField(item, "componentTier", Math.max(0, Math.min(3, tier)));
        return item;
    }

    private static <T> T allocate(final Class<T> type) throws Exception {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return type.cast(((Unsafe) unsafeField.get(null)).allocateInstance(type));
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static <T> void withCachedConfig(final ModConfigSpec.ConfigValue<T> value, final T override, final ThrowingRunnable action) throws Exception {
        final Field cachedValue = ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
        cachedValue.setAccessible(true);
        final Object previous = cachedValue.get(value);
        cachedValue.set(value, override);
        try {
            action.run();
        } finally {
            cachedValue.set(value, previous);
        }
    }

    private static Connector charge(final ManagedEnvironment environment, final double energy) {
        Connector connector = assertInstanceOf(Connector.class, environment.node());
        connector.setLocalBufferSize(energy);
        connector.changeBuffer(energy);
        return connector;
    }

    private static final class TestEnvironmentHost implements EnvironmentHost {
        @Override
        public Level world() {
            return null;
        }

        @Override
        public double xPosition() {
            return 0;
        }

        @Override
        public double yPosition() {
            return 0;
        }

        @Override
        public double zPosition() {
            return 0;
        }

        @Override
        public void markChanged() {
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
